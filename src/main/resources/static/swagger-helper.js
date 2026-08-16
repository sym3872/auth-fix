/*
 * Swagger UI를 한국어로 보기 쉽게 정리하는 작은 화면 도우미다.
 * API 주소, JWT, JSON 입력값은 번역하지 않고 화면 설명만 바꾼다.
 */
window.addEventListener("load", function () {
  "use strict";

  // Swagger가 직접 만드는 기본 영문 문구만 한국어로 바꾼다.
  // API 이름·설명·입력 예시는 컨트롤러에서 이미 한국어로 작성했으므로 여기서 다시 관리하지 않는다.
  const translations = {
    Authorize: "토큰 입력",
    "Available authorizations": "인증 설정",
    "Try it out": "직접 실행",
    Execute: "실행",
    Clear: "지우기",
    Cancel: "취소",
    Close: "닫기",
    Parameters: "매개변수",
    "No parameters": "매개변수 없음",
    "Request body": "요청 본문",
    Responses: "응답",
    "Response body": "응답 본문",
    "Response headers": "응답 헤더",
    "Server response": "서버 응답",
    Code: "코드",
    Description: "설명",
    Details: "상세",
    "Example Value": "예시 값",
    Schema: "구조",
    Model: "모델",
    "Media type": "미디어 유형",
    Required: "필수",
    Optional: "선택",
    Download: "다운로드",
    Links: "링크",
    "Request samples": "요청 예시",
    "Response samples": "응답 예시",
    OK: "성공",
    Created: "생성됨",
    "No Content": "내용 없음",
    Undocumented: "문서에 없는 응답",
    Auth: "인증",
    Post: "게시글"
  };

  // 게시글 수정 도우미에 표시할 한국어 안내 문구다.
  const editMessages = {
    title: "수정 전: 현재 글 불러오기",
    guide: "① 직접 실행 클릭 → ② 글 번호 입력 → ③ 아래 버튼 클릭 → ④ 채워진 제목·본문 수정",
    button: "현재 글 불러오기",
    enableTry: "먼저 직접 실행을 누른 뒤 글 번호를 입력해 주세요.",
    invalidId: "수정할 게시글 번호를 숫자로 입력해 주세요.",
    loading: "현재 글을 불러오는 중입니다...",
    loaded: "현재 제목과 본문을 채웠습니다. 필요한 부분만 바꾼 뒤 실행하세요.",
    notFound: "게시글을 찾을 수 없습니다.",
    failed: "게시글을 불러오지 못했습니다."
  };

  /** cURL, URL, API 경로처럼 화면을 복잡하게 만드는 요소를 숨긴다. */
  function addStyle() {
    if (document.getElementById("secure-blog-swagger-style")) {
      return;
    }

    const style = document.createElement("style");
    style.id = "secure-blog-swagger-style";
    style.textContent = ".swagger-ui .curl-command,"
      + ".swagger-ui .request-url,"
      + ".swagger-ui .opblock-summary-path { display: none !important; }"
      + ".swagger-ui .secure-blog-edit-helper {"
      + "margin: 16px 0; padding: 14px; border: 2px solid #49cc90;"
      + "border-radius: 4px; background: #f1fff7; color: #1b1b1b;"
      + "}"
      + ".swagger-ui .secure-blog-edit-helper strong { display: block; margin-bottom: 6px; }"
      + ".swagger-ui .secure-blog-edit-helper p { margin: 0 0 10px; line-height: 1.5; }"
      + ".swagger-ui .secure-blog-edit-helper .secure-blog-edit-status { margin-left: 10px; }";
    document.head.appendChild(style);
  }

  /** JSON, JWT, URL, HTTP 메서드처럼 절대 바꾸면 안 되는 영역인지 확인한다. */
  function skipTranslation(node) {
    const parent = node.parentElement;
    return !parent || parent.closest(
      "pre, code, textarea, input, .microlight, .opblock-summary-path, "
      + ".opblock-summary-method, .secure-blog-edit-helper"
    );
  }

  /** 공백을 유지하면서 정확히 일치하는 Swagger 기본 문구를 한국어로 바꾼다. */
  function translateText(text) {
    const trimmed = text.trim();
    const translated = translations[trimmed];
    if (!translated) {
        return text;
    }

    const start = text.indexOf(trimmed);
    return text.slice(0, start) + translated + text.slice(start + trimmed.length);
  }

  /** 현재 Swagger 화면에 이미 만들어진 기본 영문 문구를 한국어로 바꾼다. */
  function translateSwagger() {
    const swagger = document.querySelector(".swagger-ui");
    if (!swagger) {
      return;
    }

    const walker = document.createTreeWalker(swagger, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) {
      nodes.push(walker.currentNode);
    }

    nodes.forEach(function (node) {
      if (skipTranslation(node)) {
        return;
      }

      node.nodeValue = translateText(node.nodeValue);
    });
  }

  /** 응답 본문은 남기고, 길고 자주 필요하지 않은 응답 헤더 블록만 숨긴다. */
  function hideResponseHeaders() {
    document.querySelectorAll(".swagger-ui h5").forEach(function (title) {
      if (["Response headers", "응답 헤더"].includes(title.textContent.trim())) {
        title.parentElement.hidden = true;
      }
    });
  }

  /** 게시글 수정 API 카드만 찾아 현재 내용을 불러올 위치인지 확인한다. */
  function updateOperation() {
    return Array.from(document.querySelectorAll(".opblock.opblock-put")).find(function (block) {
      const path = block.querySelector(".opblock-summary-path");
      return path && path.textContent.replace(/\s+/g, "") === "/api/posts/{id}";
    });
  }

  /** 수정 도우미의 상태 문구를 다시 표시할 수 있게 키로 저장한다. */
  function setEditStatus(status, key) {
    status.dataset.messageKey = key;
    status.textContent = editMessages[key];
  }

  /** 공개 상세 조회 API로 현재 제목과 본문을 읽어 Swagger 요청 본문에 넣는다. */
  async function loadCurrentPost(block, status, button) {
    const idInput = block.querySelector(".parameters input");
    const textarea = block.querySelector("textarea");
    const id = idInput ? idInput.value.trim() : "";

    if (!idInput || idInput.disabled || !textarea) {
      setEditStatus(status, "enableTry");
      return;
    }
    if (!/^[1-9]\d*$/.test(id)) {
      setEditStatus(status, "invalidId");
      return;
    }

    button.disabled = true;
    setEditStatus(status, "loading");
    try {
      const response = await fetch("/api/posts/" + id);
      if (!response.ok) {
        setEditStatus(status, response.status === 404 ? "notFound" : "failed");
        return;
      }

      const post = await response.json();
      const valueSetter = Object.getOwnPropertyDescriptor(
        window.HTMLTextAreaElement.prototype, "value"
      ).set;
      valueSetter.call(textarea, JSON.stringify({ title: post.title, content: post.content }, null, 2));
      textarea.dispatchEvent(new Event("input", { bubbles: true }));
      textarea.dispatchEvent(new Event("change", { bubbles: true }));
      setEditStatus(status, "loaded");
    } catch {
      setEditStatus(status, "failed");
    } finally {
      button.disabled = false;
    }
  }

  /** PUT /api/posts/{id} 카드에 현재 글을 채우는 버튼과 안내를 추가한다. */
  function addEditHelper() {
    const block = updateOperation();
    if (!block || block.querySelector(".secure-blog-edit-helper")) {
      return;
    }

    const body = block.querySelector(".opblock-body");
    if (!body) {
      return;
    }

    const helper = document.createElement("div");
    helper.className = "secure-blog-edit-helper";

    const title = document.createElement("strong");
    title.textContent = editMessages.title;
    const guide = document.createElement("p");
    guide.textContent = editMessages.guide;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn try-out__btn";
    button.textContent = editMessages.button;
    const status = document.createElement("span");
    status.className = "secure-blog-edit-status";

    button.addEventListener("click", function () {
      loadCurrentPost(block, status, button);
    });
    helper.append(title, guide, button, status);
    body.prepend(helper);
  }

  /** Swagger가 새 카드를 그릴 때도 한국어 문구와 수정 도우미를 다시 적용한다. */
  function refreshSwaggerUi() {
    translateSwagger();
    hideResponseHeaders();
    addEditHelper();
  }

  addStyle();
  refreshSwaggerUi();
  new MutationObserver(refreshSwaggerUi).observe(document.body, { childList: true, subtree: true });
});
