#!/usr/bin/env python3
"""
SURF API Docs Sync Script

Spring Boot 컨트롤러를 파싱하여 SURF-docs에 미문서화된 API의 스켈레톤 문서를 자동 생성합니다.

Usage:
    python sync-docs.py <surf-be-path> <surf-docs-path>
"""

import os
import re
import sys
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Set, Tuple


@dataclass
class Parameter:
    name: str
    java_type: str
    annotation: str  # PathVariable, RequestParam, RequestBody, Query
    required: bool = True


@dataclass
class DtoField:
    name: str
    java_type: str
    required: bool = False
    description: str = ""


@dataclass
class Endpoint:
    http_method: str
    path: str
    method_name: str
    parameters: List[Parameter] = field(default_factory=list)
    response_type: str = "Void"
    controller_file: str = ""
    domain: str = ""
    summary: str = ""
    http_status: str = "200 OK"


# 도메인 → docs 디렉토리 매핑
DOMAIN_DIR_MAP = {
    "member": "member",
    "post": "post",
    "comment": "comment",
    "board": "board",
    "activity": "activity",
    "team": "team",
    "home": "home",
    "feedback": "feedback",
    "notification": "notification",
    "letter": "letter",
    "file": "file",
}

# Java 타입 → 문서 타입 변환
TYPE_MAP = {
    "Long": "Long",
    "long": "Long",
    "Integer": "Integer",
    "int": "Integer",
    "String": "String",
    "Boolean": "Boolean",
    "boolean": "Boolean",
    "Double": "Double",
    "double": "Double",
    "LocalDateTime": "String (ISO 8601)",
    "LocalDate": "String (ISO 8601)",
}


def find_controllers(src_path: str) -> List[str]:
    """Controller.java 파일을 재귀 탐색."""
    controllers = []
    for root, _, files in os.walk(src_path):
        for f in files:
            if f.endswith("Controller.java"):
                controllers.append(os.path.join(root, f))
    return sorted(controllers)


def find_dto_files(src_path: str) -> Dict[str, str]:
    """DTO 파일명 → 경로 매핑."""
    dtos = {}
    for root, _, files in os.walk(src_path):
        for f in files:
            if f.endswith("DTO.java") or f.endswith("Dto.java"):
                name = f.replace(".java", "")
                dtos[name] = os.path.join(root, f)
    return dtos


def extract_domain(file_path: str) -> str:
    """파일 경로에서 도메인명 추출."""
    match = re.search(r"domain/(\w+)/", file_path)
    return match.group(1) if match else "unknown"


def display_type(java_type: str) -> str:
    """Java 타입을 문서 표시용으로 변환."""
    return TYPE_MAP.get(java_type, java_type)


def parse_method_params(params_str: str) -> List[Parameter]:
    """메서드 시그니처에서 파라미터 파싱."""
    params = []
    if not params_str or not params_str.strip():
        return params

    # 제네릭 깊이를 고려하여 콤마로 분리
    depth = 0
    current = ""
    parts = []
    for char in params_str:
        if char in "<(":
            depth += 1
        elif char in ">)":
            depth -= 1
        elif char == "," and depth == 0:
            parts.append(current.strip())
            current = ""
            continue
        current += char
    if current.strip():
        parts.append(current.strip())

    for part in parts:
        part = part.strip()
        if not part:
            continue

        # 어노테이션 없는 파라미터 (Pageable 등)
        if "@" not in part:
            if "Pageable" in part:
                params.append(Parameter(
                    name="pageable", java_type="Pageable",
                    annotation="Query", required=False
                ))
            continue

        annotation = ""
        required = True

        if "@PathVariable" in part:
            annotation = "PathVariable"
        elif "@RequestParam" in part:
            annotation = "RequestParam"
            if "required = false" in part or "required=false" in part:
                required = False
        elif "@RequestBody" in part:
            annotation = "RequestBody"
        else:
            continue

        # 어노테이션과 수식어 제거 후 타입, 이름 추출
        clean = re.sub(r"@\w+(\([^)]*\))?\s*", "", part)
        clean = clean.replace("@Valid", "").strip()
        tokens = clean.split()
        if len(tokens) >= 2:
            java_type = tokens[-2]
            name = tokens[-1]
            params.append(Parameter(
                name=name, java_type=java_type,
                annotation=annotation, required=required
            ))

    return params


def parse_dto_fields(dto_path: str) -> List[DtoField]:
    """DTO(record/class) 파일에서 필드 파싱."""
    if not os.path.exists(dto_path):
        return []

    with open(dto_path, "r", encoding="utf-8") as f:
        content = f.read()

    fields = []

    # Java record 패턴: public record Foo( ... ) {}
    record_match = re.search(
        r"public\s+record\s+\w+\s*\((.*?)\)\s*(?:implements|extends|\{)",
        content, re.DOTALL
    )
    if record_match:
        body = record_match.group(1)
        for line in body.split(","):
            line = line.strip()
            if not line:
                continue
            required = bool(re.search(r"@Not(Null|Blank|Empty)", line))
            # 어노테이션 제거
            clean = re.sub(r"@\w+(\([^)]*\))?\s*", "", line).strip()
            tokens = clean.split()
            if len(tokens) >= 2:
                fields.append(DtoField(
                    name=tokens[-1], java_type=tokens[-2], required=required
                ))
        return fields

    # 일반 클래스의 private 필드 패턴
    for match in re.finditer(
        r"private\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*;", content
    ):
        fields.append(DtoField(
            name=match.group(2), java_type=match.group(1)
        ))

    return fields


def extract_base_path(content: str) -> str:
    """클래스 레벨 @RequestMapping 경로 추출."""
    match = re.search(r'@RequestMapping\("([^"]*)"\)', content)
    return match.group(1) if match else ""


def parse_controller(file_path: str) -> List[Endpoint]:
    """컨트롤러 파일에서 모든 엔드포인트 추출."""
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    lines = content.split("\n")

    domain = extract_domain(file_path)
    base_path = extract_base_path(content)
    endpoints = []

    i = 0
    while i < len(lines):
        line = lines[i].strip()

        # @Operation summary 수집
        summary = ""
        if line.startswith("@Operation"):
            op_text = line
            paren_depth = line.count("(") - line.count(")")
            while paren_depth > 0 and i + 1 < len(lines):
                i += 1
                op_text += " " + lines[i].strip()
                paren_depth += lines[i].count("(") - lines[i].count(")")
            match = re.search(r'summary\s*=\s*"([^"]*)"', op_text)
            if match:
                summary = match.group(1)
            i += 1
            if i >= len(lines):
                break
            line = lines[i].strip()

        # HTTP 매핑 어노테이션 감지
        mapping_match = re.match(
            r'@(Get|Post|Put|Patch|Delete)Mapping\("([^"]*)"\)', line
        )
        if not mapping_match:
            # 경로 없는 매핑: @GetMapping 또는 @GetMapping()
            empty_match = re.match(
                r'@(Get|Post|Put|Patch|Delete)Mapping\s*(\(\s*\))?\s*$', line
            )
            if empty_match:
                http_method = empty_match.group(1).upper()
                path = ""
            else:
                i += 1
                continue
        else:
            http_method = mapping_match.group(1).upper()
            path = mapping_match.group(2)

        # 메서드 시그니처 수집 ('{' 까지)
        method_sig = ""
        j = i + 1
        while j < len(lines):
            method_sig += " " + lines[j]
            if "{" in lines[j]:
                break
            j += 1

        # 응답 타입
        type_match = re.search(r"ApiResponse<([^>]+)>", method_sig)
        response_type = type_match.group(1) if type_match else "Void"

        # 메서드명
        name_match = re.search(r"(\w+)\s*\(", method_sig)
        method_name = name_match.group(1) if name_match else "unknown"

        # 파라미터
        paren_start = method_sig.find("(")
        paren_end = method_sig.rfind(")")
        params_str = method_sig[paren_start + 1:paren_end] if paren_start != -1 and paren_end != -1 else ""
        params = parse_method_params(params_str)

        # HTTP 상태 코드 결정
        http_status = "200 OK"
        body_end = min(j + 20, len(lines))
        body_text = "".join(lines[j:body_end])
        if "CREATED" in body_text and http_method == "POST":
            http_status = "201 Created"
        elif "NO_CONTENT" in body_text:
            http_status = "204 No Content"

        full_path = base_path + path
        endpoints.append(Endpoint(
            http_method=http_method,
            path=full_path,
            method_name=method_name,
            parameters=params,
            response_type=response_type,
            controller_file=file_path,
            domain=domain,
            summary=summary,
            http_status=http_status,
        ))

        i = j + 1

    return endpoints


def get_documented_endpoints(docs_path: str) -> Dict[str, str]:
    """문서화된 엔드포인트의 {METHOD PATH} → 파일경로 매핑."""
    documented = {}
    for root, _, files in os.walk(docs_path):
        for f in files:
            if not f.endswith(".md") or f in ("README.md", "SUMMARY.md"):
                continue
            filepath = os.path.join(root, f)
            with open(filepath, "r", encoding="utf-8") as fh:
                content = fh.read()
            for match in re.finditer(
                r"`(GET|POST|PUT|PATCH|DELETE)\s+(/[^`]+)`", content
            ):
                key = f"{match.group(1)} {match.group(2)}"
                documented[key] = filepath
    return documented


def get_permission_text(path: str) -> str:
    """URL 패턴에서 권한 텍스트 결정."""
    if path.startswith("/v1/admin/"):
        return "관리자만 접근 가능 (MANAGER 이상)"
    elif path.startswith("/v1/user/"):
        return "일반 사용자 접근 가능 (MEMBER 이상)"
    elif path.startswith("/v1/manager/sign-in"):
        return "인증 불필요 (Public)"
    elif path.startswith("/v1/manager/"):
        return "인증된 사용자 접근 가능 (로그인 필요)"
    elif path.startswith("/login/") or path.startswith("/auth/"):
        return "인증 불필요"
    return "인증 필요"


def method_name_to_filename(method_name: str) -> str:
    """camelCase → kebab-case 파일명."""
    s = re.sub(r"([A-Z])", r"-\1", method_name).lower().lstrip("-")
    return s


def generate_markdown(endpoint: Endpoint, dto_files: Dict[str, str]) -> str:
    """엔드포인트에 대한 마크다운 문서 생성."""
    title = endpoint.summary or endpoint.method_name
    permission = get_permission_text(endpoint.path)
    auth_needed = "불필요" if "인증 불필요" in permission else "JWT 인증 필요"

    md = f"""## {title}

### 개요
{endpoint.summary or f'{endpoint.http_method} {endpoint.path}'}

### 엔드포인트
`{endpoint.http_method} {endpoint.path}`

### 인증
- **인증 필요 여부:** {auth_needed}
- **권한:** {permission}

> 요청 헤더(Header)에 아래와 같이 Authorization 필드를 포함해야 합니다.
> `Authorization: Bearer {{JWT_TOKEN}}`

### 요청 (Request)

**Headers**
| Key | Type | 설명 | 필수 |
|-----|------|------|------|
| Authorization | String | Bearer 토큰 | O |
"""

    # Path Parameters
    path_params = [p for p in endpoint.parameters if p.annotation == "PathVariable"]
    if path_params:
        md += "\n**Path Parameters**\n"
        md += "| Key | Type | 설명 | 필수 |\n"
        md += "|-----|------|------|------|\n"
        for p in path_params:
            md += f"| {p.name} | {display_type(p.java_type)} | {p.name} | O |\n"

    # Query Parameters
    query_params = [p for p in endpoint.parameters if p.annotation == "RequestParam"]
    pageable = [p for p in endpoint.parameters if p.annotation == "Query"]
    if query_params or pageable:
        md += "\n**Query Parameters**\n"
        md += "| Key | Type | 설명 | 필수 | 기본값 |\n"
        md += "|-----|------|------|------|--------|\n"
        for p in query_params:
            req = "O" if p.required else "X"
            md += f"| {p.name} | {display_type(p.java_type)} | {p.name} | {req} | - |\n"
        if pageable:
            md += "| page | Integer | 페이지 번호 | X | 0 |\n"
            md += "| size | Integer | 페이지 크기 | X | 20 |\n"

    # Request Body (DTO 필드 파싱 포함)
    body_params = [p for p in endpoint.parameters if p.annotation == "RequestBody"]
    if body_params:
        dto_name = body_params[0].java_type
        md += f"\n**Body ({dto_name})**\n"
        md += "| Key | Type | 설명 | 필수 |\n"
        md += "|-----|------|------|------|\n"

        if dto_name in dto_files:
            fields = parse_dto_fields(dto_files[dto_name])
            for f in fields:
                req = "O" if f.required else "X"
                md += f"| {f.name} | {display_type(f.java_type)} | {f.name} | {req} |\n"
        else:
            md += f"| - | - | DTO 참고 | O |\n"

    status_code = endpoint.http_status.split()[0]
    md += f"""
---

### 응답 (Response)

**성공**
| HTTP Status | 의미 |
|-------------|------|
| {endpoint.http_status} | 요청 성공 |
"""

    # 응답 DTO 필드
    resp_type = endpoint.response_type
    if resp_type != "Void":
        # Slice<T>, List<T> 등에서 내부 타입 추출
        inner_match = re.match(r"(?:Slice|List|Page)<(\w+)>", resp_type)
        inner_type = inner_match.group(1) if inner_match else resp_type

        md += f"\n**Body ({resp_type})**\n"
        md += "| Key | Type | 설명 |\n"
        md += "|-----|------|------|\n"

        if inner_type in dto_files:
            fields = parse_dto_fields(dto_files[inner_type])
            for f in fields:
                md += f"| {f.name} | {display_type(f.java_type)} | {f.name} |\n"
        else:
            md += f"| - | {resp_type} | 응답 데이터 |\n"

    md += f"""
**응답 예시**
```json
{{
  "code": {status_code},
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": null
}}
```
"""

    return md


def update_summary(docs_path: str, new_entries: Dict[str, List[Tuple[str, str]]]):
    """SUMMARY.md에 새 항목 추가."""
    summary_path = os.path.join(docs_path, "SUMMARY.md")
    with open(summary_path, "r", encoding="utf-8") as f:
        content = f.read()

    for domain_dir, entries in new_entries.items():
        for title, filename in entries:
            entry_line = f"* [{title}]({domain_dir}/{filename})"
            if entry_line in content:
                continue

            # 해당 도메인 섹션의 마지막 항목 뒤에 삽입
            pattern = rf"(\* \[[^\]]+\]\({re.escape(domain_dir)}/[^)]+\))"
            matches = list(re.finditer(pattern, content))
            if matches:
                last_match = matches[-1]
                insert_pos = last_match.end()
                content = content[:insert_pos] + f"\n{entry_line}" + content[insert_pos:]
            else:
                # 섹션이 없으면 File 섹션 앞에 새 섹션 생성
                section_name = domain_dir.capitalize()
                new_section = (
                    f"\n## {section_name} <a href=\"#{domain_dir}\" id=\"{domain_dir}\"></a>\n\n"
                    f"{entry_line}\n"
                )
                if "## File" in content:
                    content = content.replace("## File", new_section + "\n## File")
                else:
                    content += new_section

    with open(summary_path, "w", encoding="utf-8") as f:
        f.write(content)


def main():
    if len(sys.argv) != 3:
        print("Usage: python sync-docs.py <surf-be-path> <surf-docs-path>")
        sys.exit(1)

    be_path = sys.argv[1]
    docs_path = sys.argv[2]
    src_path = os.path.join(be_path, "src", "main", "java", "com", "tavemakers", "surf", "domain")

    if not os.path.exists(src_path):
        print(f"❌ 소스 경로를 찾을 수 없습니다: {src_path}")
        sys.exit(1)

    if not os.path.exists(docs_path):
        print(f"❌ 문서 경로를 찾을 수 없습니다: {docs_path}")
        sys.exit(1)

    # 1. 모든 컨트롤러 파싱
    controllers = find_controllers(src_path)
    all_endpoints = []
    for ctrl in controllers:
        endpoints = parse_controller(ctrl)
        all_endpoints.extend(endpoints)

    print(f"📋 총 {len(all_endpoints)}개 엔드포인트 감지")

    # 2. DTO 파일 매핑 수집
    dto_files = find_dto_files(src_path)
    print(f"📦 총 {len(dto_files)}개 DTO 파일 감지")

    # 3. 기존 문서화된 엔드포인트 확인
    documented = get_documented_endpoints(docs_path)
    print(f"📄 기존 문서화된 엔드포인트: {len(documented)}개")

    # 4. 미문서화 엔드포인트 식별
    new_endpoints = []
    for ep in all_endpoints:
        key = f"{ep.http_method} {ep.path}"
        if key not in documented:
            new_endpoints.append(ep)

    if not new_endpoints:
        print("✅ 모든 엔드포인트가 문서화되어 있습니다. 변경사항 없음.")
        return

    print(f"🆕 미문서화 엔드포인트: {len(new_endpoints)}개")

    # 5. 신규 문서 생성
    new_summary_entries: Dict[str, List[Tuple[str, str]]] = {}
    created_files = []

    for ep in new_endpoints:
        domain_dir = DOMAIN_DIR_MAP.get(ep.domain, ep.domain)
        doc_dir = os.path.join(docs_path, domain_dir)
        os.makedirs(doc_dir, exist_ok=True)

        filename = method_name_to_filename(ep.method_name) + ".md"
        filepath = os.path.join(doc_dir, filename)

        # 파일명 충돌 시 HTTP 메서드 접두사 추가
        if os.path.exists(filepath):
            filename = f"{ep.http_method.lower()}-{method_name_to_filename(ep.method_name)}.md"
            filepath = os.path.join(doc_dir, filename)

        markdown = generate_markdown(ep, dto_files)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(markdown)

        title = ep.summary or ep.method_name
        if domain_dir not in new_summary_entries:
            new_summary_entries[domain_dir] = []
        new_summary_entries[domain_dir].append((title, filename))
        created_files.append(f"  ✏️  {domain_dir}/{filename} — {ep.http_method} {ep.path}")

    # 6. SUMMARY.md 업데이트
    if new_summary_entries:
        update_summary(docs_path, new_summary_entries)

    # 7. 결과 출력
    print()
    for line in created_files:
        print(line)
    print(f"\n🎉 {len(new_endpoints)}개 신규 API 스켈레톤 문서 생성 완료!")
    print("💡 생성된 문서는 스켈레톤입니다. PR 리뷰 시 상세 내용을 보완해주세요.")


if __name__ == "__main__":
    main()
