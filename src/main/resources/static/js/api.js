function authHeaders() {
  const headers = { Authorization: "Bearer " + (sessionStorage.getItem("mq_token") || "") };
  const opened = openedOrg();
  if (opened && opened.id != null) headers["X-Organization-Id"] = String(opened.id);
  return headers;
}

function api(method, url, body) {
  return $.ajax({
    url: url,
    method: method,
    contentType: body === undefined ? undefined : "application/json",
    data: body === undefined ? undefined : JSON.stringify(body),
    headers: authHeaders()
  });
}

function requireAuth() {
  if (!sessionStorage.getItem("mq_token")) {
    window.location.href = "/login";
  }
}

function openedOrg() {
  try {
    return JSON.parse(sessionStorage.getItem("mq_org") || "null");
  } catch (e) {
    return null;
  }
}

function platformPath(path) {
  return path === "/organizations" || path.indexOf("/organizations/") === 0
    || path === "/owners" || path.indexOf("/owners/") === 0
    || path === "/account/password" || path === "/hospitals";
}

function showNav(me) {
  const opened = openedOrg();
  const path = window.location.pathname;
  if (me.superAdmin && opened) {
    document.body.classList.add("is-super", "is-org");
    $("#brand-org, #org-banner-name, #org-line").text(opened.name);
    $(".brand").attr("href", "/");
    return;
  }
  if (me.superAdmin) {
    document.body.classList.add("is-super");
    $("#brand-org").text("Quality platform");
    $("#org-line").text("Quality platform for hospitals");
    $(".brand").attr("href", "/organizations");
    if (!platformPath(path)) window.location.href = "/organizations";
    return;
  }
  document.body.classList.add("is-org");
  $("#brand-org").text(me.organizationName || "Quality platform");
  $("#org-line").text(me.organizationName || "Quality platform for hospitals");
  if (path === "/organizations" || path.indexOf("/organizations/") === 0
      || path === "/owners" || path.indexOf("/owners/") === 0 || path === "/hospitals") {
    window.location.href = "/";
  }
}

function markNav() {
  const path = window.location.pathname;
  $(".side-link").each(function () {
    const href = $(this).attr("href");
    if (!href) return;
    const on = href === "/" ? path === "/" : path === href || path.indexOf(href + "/") === 0;
    $(this).toggleClass("active", on);
  });
  $(".subnav a").each(function () {
    $(this).toggleClass("active", $(this).attr("href") === path);
  });
  if (path.indexOf("/dashboard") === 0) $("#dash-group").addClass("open");
}

$(function () {
  if ($("body").data("page") !== "login") {
    requireAuth();
    markNav();
    const today = new Date();
    $("#today-pill").text(today.toLocaleDateString(undefined, { month: "short", day: "numeric" }));
    api("GET", "/api/auth/me").done(function (res) {
      const name = res.data.username || "Signed in";
      $("#profile-name").text(name);
      $("#profile-initial").text(name.slice(0, 1).toUpperCase());
      if (res.data.roleName) $("#profile-role").text(res.data.roleName);
      $("#who").text(name);
      if (res.data.mustChangePassword && window.location.pathname !== "/account/password") {
        window.location.href = "/account/password";
        return;
      }
      showNav(res.data);
    });
  }
  $("#logout").on("click", function () {
    sessionStorage.removeItem("mq_token");
    sessionStorage.removeItem("mq_org");
    window.location.href = "/login";
  });
  $("#leave-org").on("click", function () {
    sessionStorage.removeItem("mq_org");
    window.location.href = "/organizations";
  });
  $("#menu-toggle").on("click", function () {
    $(".app-shell").toggleClass("nav-open");
  });
  $("#dash-toggle").on("click", function () {
    $("#dash-group").toggleClass("open");
  });
  $("#global-search").on("input", function () {
    const query = $(this).val().toLowerCase();
    if ($("#search").length) {
      $("#search").val($(this).val()).trigger("input");
    }
    $("tbody tr").each(function () {
      $(this).toggle($(this).text().toLowerCase().indexOf(query) >= 0);
    });
  });
  $(document).ajaxError(function (_event, xhr) {
    if (xhr.status === 401 && $("body").data("page") !== "login") {
      sessionStorage.removeItem("mq_token");
      window.location.href = "/login";
    }
  });
});
