/*
 * Swagger UI를 한국어로 보기 쉽게 정리하는 작은 화면 도우미다.
 * API 주소, JWT, JSON 입력값은 번역하지 않고 화면 설명만 바꾼다.
 */
window.addEventListener("load", function () {
  "use strict";

  // Swagger 기본 문구와 이 프로젝트의 문구를 한 쌍으로 관리한다.
  const translationPairs = [
    { en: "Authorize", ko: "토큰 입력" },
    { en: "Available authorizations", ko: "인증 설정" },
    { en: "Try it out", ko: "직접 실행" },
    { en: "Execute", ko: "실행" },
    { en: "Clear", ko: "지우기" },
    { en: "Cancel", ko: "취소" },
    { en: "Close", ko: "닫기" },
    { en: "Parameters", ko: "매개변수" },
    { en: "No parameters", ko: "매개변수 없음" },
    { en: "Request body", ko: "요청 본문" },
    { en: "Responses", ko: "응답" },
    { en: "Response body", ko: "응답 본문" },
    { en: "Response headers", ko: "응답 헤더" },
    { en: "Server response", ko: "서버 응답" },
    { en: "Code", ko: "코드" },
    { en: "Description", ko: "설명" },
    { en: "Details", ko: "상세" },
    { en: "Example Value", ko: "예시 값" },
    { en: "Schema", ko: "구조" },
    { en: "Model", ko: "모델" },
    { en: "Media type", ko: "미디어 유형" },
    { en: "Required", ko: "필수" },
    { en: "Optional", ko: "선택" },
    { en: "Download", ko: "다운로드" },
    { en: "Links", ko: "링크" },
    { en: "Request samples", ko: "요청 예시" },
    { en: "Response samples", ko: "응답 예시" },
    { en: "OK", ko: "성공" },
    { en: "Created", ko: "생성됨" },
    { en: "No Content", ko: "내용 없음" },
    { en: "Undocumented", ko: "문서에 없는 응답" },
    { en: "Auth", ko: "인증" },
    { en: "Post", ko: "게시글" },
    { en: "A learning blog API that uses a JWT access token.", ko: "JWT Access Token을 사용하는 학습용 블로그 API입니다." },
    { en: "Sign-up and login API", ko: "회원가입과 로그인 API" },
    { en: "Post CRUD API", ko: "게시글 CRUD API" },
    { en: "Sign up", ko: "회원가입" },
    { en: "Log in", ko: "로그인" },
    { en: "Create post", ko: "게시글 작성" },
    { en: "List posts", ko: "게시글 목록 조회" },
    { en: "View post", ko: "게시글 상세 조회" },
    { en: "Edit post", ko: "게시글 수정" },
    { en: "Delete post", ko: "게시글 삭제" },
    { en: "After logging in, paste only the JWT string into Authorize at the top right.", ko: "로그인 후 받은 JWT 문자열만 오른쪽 위 토큰 입력 버튼에 한 번 붙여넣고 작성하세요." },
    { en: "1. Click Try it out. 2. Enter the post ID. 3. Click Load current post. 4. Edit only what you need and run.", ko: "① 직접 실행을 누릅니다. ② 수정할 글 번호를 입력합니다. ③ '현재 글 불러오기'를 누르면 아래 제목과 본문이 채워집니다. ④ 필요한 부분만 바꾼 뒤 실행하세요." },
    { en: "Sign-up successful", ko: "회원가입 성공" },
    { en: "Email already in use", ko: "이미 사용 중인 이메일" },
    { en: "Login successful - the entire response body is the JWT access token.", ko: "로그인 성공 - 본문 전체가 JWT Access Token입니다." },
    { en: "On successful login, copy the entire JWT line shown in the response. In the Authorize dialog at the top right, paste only the token; Swagger adds Bearer automatically.", ko: "성공 응답에 표시되는 JWT 한 줄 전체만 복사하세요. 오른쪽 위 토큰 입력 버튼에는 토큰만 붙여넣으면 Bearer는 Swagger가 자동으로 붙입니다." },
    { en: "Input is invalid", ko: "입력값이 올바르지 않음" },
    { en: "Incorrect email or password", ko: "이메일 또는 비밀번호가 올바르지 않음" },
    { en: "Post created successfully", ko: "게시글 작성 성공" },
    { en: "Post retrieved successfully", ko: "게시글 조회 성공" },
    { en: "Post updated successfully", ko: "게시글 수정 성공" },
    { en: "Post deleted successfully", ko: "게시글 삭제 성공" },
    { en: "The title or content is invalid", ko: "제목 또는 본문 입력이 올바르지 않음" },
    { en: "JWT token is missing or invalid", ko: "JWT 토큰이 없거나 올바르지 않음" },
    { en: "Only the author can change this post", ko: "작성자 본인만 수정하거나 삭제할 수 있음" },
    { en: "Post not found", ko: "게시글을 찾을 수 없음" },
    { en: "Post title", ko: "게시글 제목" },
    { en: "Post content", ko: "게시글 본문" },
    { en: "Post ID", ko: "게시글 번호" },
    { en: "Author ID", ko: "작성자 회원 번호" },
    { en: "Author nickname", ko: "작성자 닉네임" },
    { en: "Email to use for login", ko: "로그인에 사용할 이메일" },
    { en: "Password with at least 4 characters", ko: "4글자 이상 비밀번호" },
    { en: "Nickname to display", ko: "화면에 표시할 닉네임" },
    { en: "Registered email", ko: "회원가입한 이메일" },
    { en: "Password used during sign-up", ko: "회원가입 때 입력한 비밀번호" }
  ];

  // 영어와 한국어 어느 쪽 문구가 와도 반대 언어를 찾을 수 있게 Map으로 만든다.
  const translationMap = new Map();
  translationPairs.forEach(function (pair) {
    translationMap.set(pair.en, pair);
    translationMap.set(pair.ko, pair);
  });

  // React가 같은 글자 노드를 다시 그릴 때 원래 문구를 잃지 않게 보관한다.
  const originalTexts = new WeakMap();

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

  /** cURL, URL, 응답 헤더, API 경로처럼 화면을 복잡하게 만드는 요소를 숨긴다. */
  function addStyle() {
    if (document.getElementById("secure-blog-swagger-style")) {
      return;
    }

    const style = document.createElement("style");
    style.id = "secure-blog-swagger-style";
    style.textContent = ".swagger-ui .curl-command,"
      + ".swagger-ui .request-url,"
      + ".swagger-ui .opblock-summary-path,"
      + ".swagger-ui .live-responses-table .response-col_description > div:has(> h5 + pre.microlight)"
      + "{ display: none !important; }"
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
    const pair = translationMap.get(trimmed);
    if (!pair) {
      return text;
    }

    const start = text.indexOf(trimmed);
    return text.slice(0, start) + pair.ko + text.slice(start + trimmed.length);
  }

  /** 현재 Swagger 화면에 이미 만들어진 일반 문구를 선택 언어로 바꾼다. */
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

      if (!originalTexts.has(node)) {
        originalTexts.set(node, node.nodeValue);
      }
      node.nodeValue = translateText(originalTexts.get(node));
    });
  }

  /** 게시글 수정 API 카드만 찾아 현재 내용을 불러올 위치인지 확인한다. */
  function updateOperation() {
    return Array.from(document.querySelectorAll(".opblock.opblock-put")).find(function (block) {
      const path = block.querySelector(".opblock-summary-path");
      return path && path.textContent.replace(/\s+/g, "") === "/api/posts/{id}";
    });
  }

  /** 수정 도우미의 제목·안내·버튼을 한국어로 채운다. */
  function renderEditHelper() {
    const helper = document.querySelector(".secure-blog-edit-helper");
    if (!helper) {
      return;
    }

    helper.querySelector("[data-role=edit-title]").textContent = editMessages.title;
    helper.querySelector("[data-role=edit-guide]").textContent = editMessages.guide;
    helper.querySelector("[data-role=edit-button]").textContent = editMessages.button;

    const status = helper.querySelector(".secure-blog-edit-status");
    if (status.dataset.messageKey) {
      status.textContent = editMessages[status.dataset.messageKey];
    }
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
    if (!/^\d+$/.test(id)) {
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
    } catch (error) {
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
    title.dataset.role = "edit-title";
    const guide = document.createElement("p");
    guide.dataset.role = "edit-guide";
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn try-out__btn";
    button.dataset.role = "edit-button";
    const status = document.createElement("span");
    status.className = "secure-blog-edit-status";

    button.addEventListener("click", function () {
      loadCurrentPost(block, status, button);
    });
    helper.append(title, guide, button, status);
    body.prepend(helper);
    renderEditHelper();
  }

  /** Swagger가 새 카드를 그릴 때도 한국어 문구와 수정 도우미를 다시 적용한다. */
  function refreshSwaggerUi() {
    translateSwagger();
    addEditHelper();
    renderEditHelper();
  }

  addStyle();
  refreshSwaggerUi();
  new MutationObserver(refreshSwaggerUi).observe(document.body, { childList: true, subtree: true });
});
