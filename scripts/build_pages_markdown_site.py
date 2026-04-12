#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later

from __future__ import annotations

import argparse
import json
import os
import posixpath
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path, PurePosixPath
from urllib.parse import quote


MARKDOWN_LINK_PATTERN = re.compile(r"(!?\[[^\]]*])\(([^)]+)\)")
GITHUB_HOST = "github.com"
GITHUB_HTTPS_PREFIX = "https" + "://" + GITHUB_HOST + "/"


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build a markdown documentation subtree for GitHub Pages.",
    )
    parser.add_argument(
        "--output-dir",
        required=True,
        help="Directory where the rendered HTML site should be written.",
    )
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Repository root containing README.md, docs/, and module README files.",
    )
    parser.add_argument(
        "--repo-slug",
        default=None,
        help="GitHub owner/repo slug. Defaults to GITHUB_REPOSITORY or origin remote parsing.",
    )
    return parser.parse_args()


def _normalize(relative_path: str) -> PurePosixPath:
    normalized = posixpath.normpath(relative_path)
    if normalized == ".":
        return PurePosixPath(".")
    return PurePosixPath(normalized)


def _git_repo_slug(repo_root: Path) -> str | None:
    env_slug = os.environ.get("GITHUB_REPOSITORY")
    if env_slug:
        return env_slug
    try:
        remote = subprocess.check_output(
            ["git", "-C", str(repo_root), "remote", "get-url", "origin"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return None

    ssh_match = re.fullmatch(r"git@github\.com:([^/]+/[^/]+?)(?:\.git)?", remote)
    if ssh_match:
        return ssh_match.group(1)

    https_match = re.fullmatch(
        rf"{re.escape(GITHUB_HTTPS_PREFIX)}([^/]+/[^/]+?)(?:\.git)?",
        remote,
    )
    if https_match:
        return https_match.group(1)

    return None


def _github_blob_url(repo_slug: str, relative_path: PurePosixPath) -> str:
    quoted_path = quote(relative_path.as_posix(), safe="/")
    return f"{GITHUB_HTTPS_PREFIX}{repo_slug}/blob/master/{quoted_path}"


def _github_tree_url(repo_slug: str, relative_path: PurePosixPath) -> str:
    quoted_path = quote(relative_path.as_posix(), safe="/")
    return f"{GITHUB_HTTPS_PREFIX}{repo_slug}/tree/master/{quoted_path}"


def _is_external_target(target: str) -> bool:
    return bool(re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", target))


def _split_target_and_suffix(target: str) -> tuple[str, str]:
    stripped = target.strip()
    if stripped.startswith("<"):
        end = stripped.find(">")
        if end != -1:
            return stripped[: end + 1], stripped[end + 1 :]
    match = re.match(r"([^ \t]+)(.*)", stripped, re.DOTALL)
    if match is None:
        return stripped, ""
    return match.group(1), match.group(2)


def _canonical_stage_path(source_relative: PurePosixPath) -> PurePosixPath:
    parts = source_relative.parts
    if source_relative == PurePosixPath("README.md"):
        return PurePosixPath("index.md")
    if source_relative == PurePosixPath("CHANGELOG.md"):
        return PurePosixPath("changelog.md")
    if source_relative == PurePosixPath("NOTICE.md"):
        return PurePosixPath("notice.md")
    if parts and parts[0] == "docs":
        return PurePosixPath(*parts[1:])
    if len(parts) >= 2 and parts[1] == "README.md":
        return PurePosixPath(parts[0], "index.md")
    if len(parts) >= 2 and parts[1] == "CHANGELOG.md":
        return PurePosixPath(parts[0], "changelog.md")
    return source_relative


def _stage_targets(source_relative: PurePosixPath) -> list[PurePosixPath]:
    return [_canonical_stage_path(source_relative)]


def _published_markdown_sources(repo_root: Path) -> list[PurePosixPath]:
    selected: set[PurePosixPath] = set()

    for candidate in ("README.md", "CHANGELOG.md", "NOTICE.md"):
        path = repo_root / candidate
        if path.is_file():
            selected.add(PurePosixPath(candidate))

    docs_dir = repo_root / "docs"
    if docs_dir.is_dir():
        for path in docs_dir.rglob("*.md"):
            selected.add(PurePosixPath(path.relative_to(repo_root).as_posix()))

    for child in repo_root.iterdir():
        if not child.is_dir():
            continue
        if child.name.startswith(".") or child.name in {"build", ".gradle"}:
            continue
        for candidate in ("README.md", "CHANGELOG.md"):
            path = child / candidate
            if path.is_file():
                selected.add(PurePosixPath(path.relative_to(repo_root).as_posix()))
        child_docs_dir = child / "docs"
        if child_docs_dir.is_dir():
            for path in child_docs_dir.rglob("*.md"):
                selected.add(PurePosixPath(path.relative_to(repo_root).as_posix()))

    return sorted(selected, key=lambda path: path.as_posix())


def _rewrite_markdown_links(
    text: str,
    *,
    source_relative: PurePosixPath,
    stage_relative: PurePosixPath,
    canonical_targets: dict[str, PurePosixPath],
    repo_slug: str | None,
    repo_root: Path,
) -> str:
    def replace(match: re.Match[str]) -> str:
        label = match.group(1)
        target = match.group(2)
        target_token, suffix = _split_target_and_suffix(target)
        wrapped = target_token.startswith("<") and target_token.endswith(">")
        raw_target = target_token[1:-1] if wrapped else target_token

        if not raw_target or raw_target.startswith("#") or raw_target.startswith("/"):
            return match.group(0)
        if _is_external_target(raw_target):
            return match.group(0)

        if "#" in raw_target:
            raw_path, fragment = raw_target.split("#", 1)
            fragment_suffix = f"#{fragment}"
        else:
            raw_path = raw_target
            fragment_suffix = ""

        resolved_source = _normalize(posixpath.join(source_relative.parent.as_posix(), raw_path))
        resolved_key = resolved_source.as_posix()
        directory_readme_key = f"{resolved_key}/README.md" if resolved_key != "." else "README.md"
        if resolved_key in canonical_targets:
            target_stage = canonical_targets[resolved_key]
        elif directory_readme_key in canonical_targets:
            target_stage = canonical_targets[directory_readme_key]
        elif resolved_key == "docs":
            target_stage = PurePosixPath("index.md")
        else:
            target_stage = None

        if target_stage is not None:
            rewritten = posixpath.relpath(target_stage.as_posix(), start=stage_relative.parent.as_posix())
            if rewritten == ".":
                rewritten = "./"
            rewritten = f"{rewritten}{fragment_suffix}"
        else:
            target_path = repo_root / resolved_source.as_posix()
            if repo_slug is not None and target_path.exists():
                if target_path.is_dir():
                    rewritten = f"{_github_tree_url(repo_slug, resolved_source)}{fragment_suffix}"
                else:
                    rewritten = f"{_github_blob_url(repo_slug, resolved_source)}{fragment_suffix}"
            else:
                return match.group(0)

        final_token = f"<{rewritten}>" if wrapped else rewritten
        return f"{label}({final_token}{suffix})"

    return MARKDOWN_LINK_PATTERN.sub(replace, text)


def _fallback_title(path: PurePosixPath) -> str:
    stem = path.stem if path.stem != "index" else (path.parent.name or "Home")
    replacements = {
        "api": "API",
        "cli": "CLI",
        "ide": "IDE",
        "ij": "IJ",
        "jvm": "JVM",
        "k2": "K2",
        "kmp": "KMP",
    }
    words = stem.replace("-", " ").replace("_", " ").split()
    if not words:
        return "Untitled"
    return " ".join(replacements.get(word.lower(), word.capitalize()) for word in words)


def _document_title(markdown_text: str, stage_relative: PurePosixPath) -> str:
    for line in markdown_text.splitlines():
        stripped = line.strip()
        if not stripped.startswith("# "):
            continue
        title = stripped[2:].strip().strip("#").strip()
        if not title:
            continue
        title = re.sub(r"`([^`]+)`", r"\1", title)
        title = re.sub(r"\[([^\]]+)]\([^)]+\)", r"\1", title)
        title = title.replace("*", "")
        if title:
            return title
    return _fallback_title(stage_relative)


def _empty_nav_node() -> dict[str, dict[str, object]]:
    return {"files": {}, "dirs": {}}


def _insert_nav_document(
    node: dict[str, dict[str, object]],
    *,
    relative_path: PurePosixPath,
    nav_path: PurePosixPath,
    title: str,
) -> None:
    current = node
    for part in relative_path.parts[:-1]:
        current = current["dirs"].setdefault(part, _empty_nav_node())  # type: ignore[assignment]
    current["files"][relative_path.name] = {"nav_path": nav_path, "title": title}


def _build_nav_tree(documents: list[dict[str, object]]) -> dict[str, dict[str, object]]:
    tree = _empty_nav_node()
    for document in sorted(documents, key=lambda item: str(item["relative_path"])):
        _insert_nav_document(
            tree,
            relative_path=document["relative_path"],  # type: ignore[arg-type]
            nav_path=document["nav_path"],  # type: ignore[arg-type]
            title=document["title"],  # type: ignore[arg-type]
        )
    return tree


def _directory_label(directory_name: str, node: dict[str, dict[str, object]]) -> str:
    index_doc = node["files"].get("index.md")
    if isinstance(index_doc, dict):
        return str(index_doc["title"])
    return _fallback_title(PurePosixPath(directory_name))


def _file_entries(
    node: dict[str, dict[str, object]],
    *,
    include_overview: bool,
) -> list[tuple[str, str]]:
    entries: list[tuple[str, str]] = []
    index_doc = node["files"].get("index.md")
    if isinstance(index_doc, dict):
        overview_title = "Overview" if include_overview else str(index_doc["title"])
        entries.append((overview_title, str(index_doc["nav_path"])))

    for special_name, special_title in (("changelog.md", "Changelog"), ("notice.md", "Notice")):
        special_doc = node["files"].get(special_name)
        if isinstance(special_doc, dict):
            entries.append((special_title, str(special_doc["nav_path"])))

    other_files: list[tuple[str, str]] = []
    for file_name, document in node["files"].items():
        if file_name in {"index.md", "changelog.md", "notice.md"}:
            continue
        if not isinstance(document, dict):
            continue
        other_files.append((str(document["title"]), str(document["nav_path"])))

    other_files.sort(key=lambda item: item[0].lower())
    entries.extend(other_files)
    return entries


def _directory_entry(directory_name: str, node: dict[str, dict[str, object]]) -> tuple[str, object]:
    label = _directory_label(directory_name, node)
    children = _node_entries(node, include_overview=True)
    if len(children) == 1 and children[0][0] == "Overview":
        return label, children[0][1]
    return label, children


def _node_entries(
    node: dict[str, dict[str, object]],
    *,
    include_overview: bool,
) -> list[tuple[str, object]]:
    entries: list[tuple[str, object]] = list(_file_entries(node, include_overview=include_overview))
    directory_entries = [
        _directory_entry(directory_name, child_node)
        for directory_name, child_node in node["dirs"].items()
    ]
    directory_entries.sort(key=lambda item: item[0].lower())
    entries.extend(directory_entries)
    return entries


def _document_section(source_relative: PurePosixPath) -> tuple[str, str | None, PurePosixPath | None]:
    if source_relative == PurePosixPath("README.md"):
        return "home", None, None
    if source_relative in {PurePosixPath("CHANGELOG.md"), PurePosixPath("NOTICE.md")}:
        return "project", None, source_relative
    parts = source_relative.parts
    if parts and parts[0] == "docs":
        if len(parts) == 1:
            return "guides", None, None
        return "guides", None, PurePosixPath(*parts[1:])
    if not parts:
        return "project", None, None

    module_name = parts[0]
    if len(parts) == 2 and parts[1] in {"README.md", "CHANGELOG.md", "NOTICE.md"}:
        return "module", module_name, _canonical_stage_path(source_relative).relative_to(module_name)
    if len(parts) >= 3 and parts[1] == "docs":
        return "module", module_name, PurePosixPath(*parts[2:])
    return "module", module_name, _canonical_stage_path(source_relative).relative_to(module_name)


def _yaml_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def _append_yaml_nav(lines: list[str], entries: list[tuple[str, object]], indent: int = 2) -> None:
    prefix = " " * indent
    for title, value in entries:
        if isinstance(value, list):
            lines.append(f"{prefix}- {_yaml_string(title)}:")
            _append_yaml_nav(lines, value, indent + 2)
        else:
            lines.append(f"{prefix}- {_yaml_string(title)}: {_yaml_string(str(value))}")


def _build_nav(documents: list[dict[str, object]]) -> list[tuple[str, object]]:
    home_document: dict[str, object] | None = None
    project_documents: list[dict[str, object]] = []
    guide_documents: list[dict[str, object]] = []
    module_documents: dict[str, list[dict[str, object]]] = {}

    for document in documents:
        source_relative = document["source_relative"]
        if not isinstance(source_relative, PurePosixPath):
            continue
        section, module_name, relative_path = _document_section(source_relative)
        if section == "home":
            home_document = document
            continue
        if section == "project":
            if relative_path is None:
                continue
            project_documents.append(
                {
                    "relative_path": relative_path,
                    "nav_path": document["stage_relative"],
                    "title": document["title"],
                }
            )
            continue
        if relative_path is None:
            continue
        entry = {
            "relative_path": relative_path,
            "nav_path": document["stage_relative"],
            "title": document["title"],
        }
        if section == "guides":
            guide_documents.append(entry)
        elif module_name is not None:
            module_documents.setdefault(module_name, []).append(entry)

    nav: list[tuple[str, object]] = []
    if home_document is not None:
        nav.append(("Home", str(home_document["stage_relative"])))

    if project_documents:
        project_tree = _build_nav_tree(project_documents)
        project_entries = _node_entries(project_tree, include_overview=False)
        nav.append(("Project", project_entries))

    if guide_documents:
        guide_tree = _build_nav_tree(guide_documents)
        nav.append(("Guides", _node_entries(guide_tree, include_overview=False)))

    if module_documents:
        module_entries = [
            _directory_entry(module_name, _build_nav_tree(documents_for_module))
            for module_name, documents_for_module in sorted(module_documents.items())
        ]
        if len(module_entries) == 1:
            nav.append(module_entries[0])
        else:
            nav.append(("Modules", module_entries))

    return nav


def _write_mkdocs_config(
    config_path: Path,
    docs_dir: Path,
    output_dir: Path,
    site_name: str,
    *,
    nav: list[tuple[str, object]],
) -> None:
    lines = [
        f"site_name: {site_name}",
        f"docs_dir: {docs_dir}",
        f"site_dir: {output_dir}",
        "use_directory_urls: true",
        "theme:",
        "  name: mkdocs",
        "markdown_extensions:",
        "  - admonition",
        "  - attr_list",
        "  - def_list",
        "  - fenced_code",
        "  - footnotes",
        "  - tables",
        "  - toc:",
        "      permalink: true",
    ]
    if nav:
        lines.append("nav:")
        _append_yaml_nav(lines, nav)
    lines.append("")
    config_path.write_text(
        "\n".join(lines),
        encoding="utf-8",
    )


def main() -> int:
    args = _parse_args()
    repo_root = Path(args.repo_root).resolve()
    output_dir = Path(args.output_dir).resolve()
    repo_slug = args.repo_slug or _git_repo_slug(repo_root)

    published_sources = _published_markdown_sources(repo_root)
    if not published_sources:
        raise SystemExit("No markdown documentation files selected for publication.")

    canonical_targets = {
        source_relative.as_posix(): _canonical_stage_path(source_relative)
        for source_relative in published_sources
    }
    staged_documents: list[dict[str, object]] = []

    shutil.rmtree(output_dir, ignore_errors=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="pages-markdown-") as temp_dir_name:
        temp_dir = Path(temp_dir_name)
        docs_dir = temp_dir / "docs"
        docs_dir.mkdir(parents=True, exist_ok=True)

        for source_relative in published_sources:
            source_path = repo_root / source_relative.as_posix()
            source_text = source_path.read_text(encoding="utf-8")
            for stage_relative in _stage_targets(source_relative):
                document_title = _document_title(source_text, stage_relative)
                stage_text = _rewrite_markdown_links(
                    source_text,
                    source_relative=source_relative,
                    stage_relative=stage_relative,
                    canonical_targets=canonical_targets,
                    repo_slug=repo_slug,
                    repo_root=repo_root,
                )
                destination = docs_dir / stage_relative.as_posix()
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(stage_text, encoding="utf-8")
                staged_documents.append(
                    {
                        "source_relative": source_relative,
                        "stage_relative": stage_relative,
                        "title": document_title,
                    }
                )

        config_path = temp_dir / "mkdocs.yml"
        nav = _build_nav(staged_documents)
        _write_mkdocs_config(
            config_path,
            docs_dir=docs_dir,
            output_dir=output_dir,
            site_name=f"{repo_root.name} guides",
            nav=nav,
        )

        subprocess.run(
            [sys.executable, "-m", "mkdocs", "build", "--clean", "--config-file", str(config_path)],
            check=True,
        )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
