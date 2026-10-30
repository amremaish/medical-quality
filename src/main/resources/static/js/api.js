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

let currentMe = null;

function hasPerm(name) {
  return holds(currentMe, name);
}

function holds(me, name) {
  if (!name) return false;
  if (me && me.superAdmin) return true;
  return !!(me && Array.isArray(me.permissions) && me.permissions.indexOf(name) >= 0);
}

function directoryAdmin(me) {
  if (!me) return false;
  return (me.superAdmin && !!openedOrg()) || me.roleCode === "HOSPITAL_OWNER";
}

function screenAllowed(path, me) {
  if (!me) return false;
  if (path === "/account/password" || path === "/account/setup" || path === "/login") return true;
  if (isPlatform(path)) return !!me.superAdmin;
  if (me.superAdmin && !openedOrg()) return false;
  if (path === "/users/new" || path.indexOf("/users/new/") === 0
      || path === "/departments/new" || path.indexOf("/departments/new/") === 0) {
    return directoryAdmin(me);
  }
  if (path === "/roles" || path.indexOf("/roles/") === 0) return directoryAdmin(me);
  if (path === "/users" || path.indexOf("/users/") === 0) {
    return directoryAdmin(me) || holds(me, "MANAGE_USER_PERMISSION_OVERRIDES");
  }
  if (path.indexOf("/assessors/") === 0) {
    return directoryAdmin(me) || holds(me, "MANAGE_ASSESSORS");
  }
  const required = permissionFor(path);
  if (required === null) return true;
  return holds(me, required);
}

function isPlatform(path) {
  return path === "/organizations" || path.indexOf("/organizations/") === 0
    || path === "/owners" || path.indexOf("/owners/") === 0
    || path === "/hospitals" || path.indexOf("/hospitals/") === 0;
}

function permissionFor(path) {
  if (path === "/dashboard" || path === "/dashboard/") return "VIEW_COMMAND_CENTER";
  if (path.indexOf("/dashboard/executive") === 0) return "VIEW_EXECUTIVE_DASHBOARD";
  if (path.indexOf("/dashboard/assessor") === 0) return "VIEW_ASSESSOR_DASHBOARD";
  if (path.indexOf("/dashboard/quality-director") === 0) return "VIEW_QUALITY_DIRECTOR_DASHBOARD";
  if (path.indexOf("/dashboard/") === 0) return "";
  if (path === "/audits/new" || path.indexOf("/audits/new/") === 0) return "CONDUCT_AUDIT";
  if (path === "/record-reviews/new" || path.indexOf("/record-reviews/new/") === 0) return "CONDUCT_AUDIT";
  if (path === "/capas/new" || path.indexOf("/capas/new/") === 0) return "MANAGE_CAPA";
  if (path === "/evidence/new" || path.indexOf("/evidence/new/") === 0) return "MANAGE_EVIDENCE";
  if (path === "/imports" || path.indexOf("/imports/") === 0) return "MANAGE_IMPORT";
  return null;
}

function listedPerms(el, attr) {
  const raw = el.getAttribute(attr) || "";
  return raw.split(",").map(function (name) { return name.trim(); }).filter(Boolean);
}

function firstDashboard() {
  if (hasPerm("VIEW_COMMAND_CENTER")) return "/dashboard";
  if (hasPerm("VIEW_EXECUTIVE_DASHBOARD")) return "/dashboard/executive";
  if (hasPerm("VIEW_ASSESSOR_DASHBOARD")) return "/dashboard/assessor";
  if (hasPerm("VIEW_QUALITY_DIRECTOR_DASHBOARD")) return "/dashboard/quality-director";
  return null;
}

function applyControls() {
  $("[data-perm]").each(function () {
    const names = listedPerms(this, "data-perm");
    $(this).prop("hidden", !names.some(hasPerm));
  });
  $("[data-perm-all]").each(function () {
    const names = listedPerms(this, "data-perm-all");
    $(this).prop("hidden", !(names.length && names.every(hasPerm)));
  });
  const homeDash = document.getElementById("home-dashboard");
  if (homeDash) {
    const target = firstDashboard();
    homeDash.hidden = !target;
    if (target) homeDash.setAttribute("href", target);
  }
  const dashLinks = $("#dash-group .subnav a");
  if (dashLinks.length) {
    const anyDash = dashLinks.filter(function () { return !this.hidden; }).length > 0;
    $("#dash-group").prop("hidden", !anyDash);
  }
}

function actingOrganizationId(me) {
  if (me.superAdmin) {
    const opened = openedOrg();
    return opened && opened.id != null ? opened.id : null;
  }
  return me.organizationId;
}

function sprite(name) {
  return "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><use href='/icons/sprite.svg#" + name + "'></use></svg>";
}

function countLabel(count, singular, plural) {
  if (count === 1) return "1 " + singular;
  return count + " " + plural;
}

function pluralWord(count, singular, plural) {
  return count === 1 ? singular : plural;
}

function auditLine(row) {
  if (!row.audits) return "No audits yet";
  if (!row.pendingAudits) return countLabel(row.audits, "audit, none pending", "audits, none pending");
  if (row.pendingAudits === row.audits) return countLabel(row.pendingAudits, "audit in progress", "audits in progress");
  return row.pendingAudits + " pending of " + row.audits;
}

function statusPill(href, tone, icon, value, label) {
  return "<a class='stat-pill' href='" + href + "'><span class='disc " + tone + "'>" + sprite(icon) +
    "</span><div><b>" + value + "</b><small>" + label + "</small></div></a>";
}

function setShellCount(linkId, countId, count, label) {
  const badge = $(countId);
  const link = $(linkId);
  if (count > 0) {
    badge.text(count > 99 ? "99+" : String(count)).prop("hidden", false);
    link.attr("aria-label", label + ", " + countLabel(count, "open", "open"));
  } else {
    badge.prop("hidden", true).text("");
    link.attr("aria-label", label);
  }
}

function paintHome(row) {
  if (!$("#home-status").length) return;
  const board = firstDashboard() || "/dashboard";
  const compliance = row.audits ? row.averageCompliance + "%" : "—";
  const complianceLabel = row.audits ? "Compliance" : "No score yet";
  $("#home-status").prop("hidden", false).html([
    statusPill("/audits", "blue", "clipboard", row.pendingAudits, pluralWord(row.pendingAudits, "Pending audit", "Pending audits")),
    statusPill("/findings", "orange", "alert", row.openFindings, "Open findings"),
    statusPill("/capas", "green", "capa", row.openCapas, "Open CAPA"),
    statusPill(board, "blue", "chart", compliance, complianceLabel)
  ].join(""));
  $("#home-audits").text(auditLine(row));
  $("#home-compliance").text(row.audits ? row.averageCompliance + "% compliance" : "No compliance score yet");
}

function loadHospitalPulse() {
  if (!document.body.classList.contains("is-org")) return;
  if (firstDashboard()) {
    api("GET", "/api/dashboard").done(function (res) {
      const row = res.data;
      setShellCount("#findings-link", "#findings-count", row.openFindings, "Findings");
      setShellCount("#capa-link", "#capa-count", row.openCapas, "CAPA");
      paintHome(row);
    });
  }
  if ($("#home-records").length) {
    api("GET", "/api/record-reviews").done(function (res) {
      const count = (res.data || []).length;
      $("#home-records").text(count ? countLabel(count, "review", "reviews") : "No reviews yet");
    });
  }
}

function showNav(me) {
  currentMe = me || null;
  const opened = openedOrg();
  const path = window.location.pathname;
  document.body.classList.remove("is-super", "is-org");
  $(".directory-nav").prop("hidden", !directoryAdmin(me));
  $(".users-nav").prop("hidden", !(directoryAdmin(me) || hasPerm("MANAGE_USER_PERMISSION_OVERRIDES")));
  if (me.superAdmin && opened && !isPlatform(path)) {
    document.body.classList.add("is-super", "is-org");
    $("#brand-org, #org-banner-name, #org-line").text(opened.name);
    $(".brand").attr("href", "/");
  } else if (me.superAdmin) {
    document.body.classList.add("is-super");
    $("#brand-org").text("Quality platform");
    $("#org-line").text("Quality platform for hospitals");
    $(".brand").attr("href", "/organizations");
  } else {
    document.body.classList.add("is-org");
    $("#brand-org").text(me.organizationName || "Quality platform");
    $("#org-line").text(me.organizationName || "Quality platform for hospitals");
  }
  applyControls();
  loadHospitalPulse();
  if (!screenAllowed(path, me)) {
    const target = me.superAdmin && !opened ? "/organizations" : "/";
    if (path !== target) window.location.href = target;
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
  if ($("body").data("page") !== "login" && $("body").data("page") !== "setup") {
    requireAuth();
    markNav();
    if (!$("main tbody").length) $(".search").attr("hidden", true);
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
      $(document).trigger("mq-ready");
    }).fail(function (xhr) {
      if (xhr.status !== 401) window.location.href = "/login";
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
