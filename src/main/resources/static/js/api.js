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

function dashboardLabel(path) {
  if (path === "/dashboard/executive") return "Executive view";
  if (path === "/dashboard/assessor") return "Assessor view";
  if (path === "/dashboard/quality-director") return "Quality director view";
  return "Command center";
}

function auditAttentionNote(row) {
  if (!row.pendingAudits) return "None pending";
  if (!row.overdueAudits) return "None past the audit date";
  return countLabel(row.overdueAudits, "past the audit date", "past the audit date");
}

function findingAttentionNote(row) {
  if (!row.openFindings) return "None open";
  const count = row.findingsNeedingAction || 0;
  if (!count) return "Each open finding has a CAPA";
  return count === 1
    ? "1 needs a CAPA or has been open more than 30 days"
    : count + " need a CAPA or have been open more than 30 days";
}

function capaAttentionNote(row) {
  if (!row.openCapas) return "None open";
  if (!row.overdueCapas) return "None past the target date";
  return countLabel(row.overdueCapas, "past the target date", "past the target date");
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
    if (target) {
      homeDash.setAttribute("href", target);
      const name = document.getElementById("home-dashboard-name");
      if (name) name.textContent = dashboardLabel(target);
    }
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

function formatAsOf(date) {
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  return date.getDate() + " " + months[date.getMonth()] + " " + date.getFullYear();
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

function statusPill(href, tone, icon, value, label, note, late) {
  const detail = note
    ? "<small" + (late ? " class='late'" : "") + ">" + labelText(note) + "</small>"
    : "";
  return "<a class='stat-pill' href='" + href + "'><span class='disc " + tone + "'>" + sprite(icon) +
    "</span><div><b>" + value + "</b><small>" + labelText(label) + "</small>" + detail + "</div></a>";
}

function labelText(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

function setShellCount(linkId, countId, count, label, tone) {
  const badge = $(countId);
  const link = $(linkId);
  badge.removeClass("is-warning is-danger");
  if (count > 0) {
    badge.text(count > 99 ? "99+" : String(count)).prop("hidden", false).addClass(tone === "danger" ? "is-danger" : "is-warning");
    link.attr("aria-label", label).attr("title", label);
  } else {
    badge.prop("hidden", true).text("");
    link.attr("aria-label", label).removeAttr("title");
  }
}

function paintHome(row) {
  if (!$("#home-status").length) return;
  const board = firstDashboard() || "/dashboard";
  const compliance = row.audits ? row.averageCompliance + "%" : "—";
  const complianceLabel = row.audits ? "Compliance" : "No score yet";
  $("#home-status").prop("hidden", false).html([
    statusPill("/audits", "blue", "clipboard", row.pendingAudits, pluralWord(row.pendingAudits, "Pending audit", "Pending audits"), auditAttentionNote(row), row.overdueAudits > 0),
    statusPill("/findings", row.findingsNeedingAction > 0 ? "red" : "orange", "alert", row.openFindings, "Open findings", findingAttentionNote(row), row.findingsNeedingAction > 0),
    statusPill("/capas", row.overdueCapas > 0 ? "red" : "orange", "capa", row.openCapas, "Open CAPA", capaAttentionNote(row), row.overdueCapas > 0),
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
      const findingsUrgent = row.findingsNeedingAction || 0;
      const capasLate = row.overdueCapas || 0;
      setShellCount("#findings-link", "#findings-count", findingsUrgent,
        findingsUrgent ? countLabel(findingsUrgent, "finding needs action", "findings need action") : "Findings",
        "warning");
      setShellCount("#capa-link", "#capa-count", capasLate,
        capasLate ? countLabel(capasLate, "CAPA past its target date", "CAPAs past their target date") : "CAPA",
        "danger");
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
  $(".nav-section").each(function () {
    const visible = $(this).children("a.side-link").filter(function () { return !this.hidden; }).length;
    $(this).prop("hidden", visible === 0);
  });
  loadHospitalPulse();
  if (!screenAllowed(path, me)) {
    const target = me.superAdmin && !opened ? "/organizations" : "/";
    if (path !== target) window.location.href = target;
  }
}

function syncNavTips() {
  const rail = window.matchMedia("(max-width: 1023px) and (min-width: 768px)").matches;
  const collapsed = rail;
  $(".side-link, #dash-toggle").each(function () {
    const label = $(this).find(".label").first().text().trim();
    if (!label) return;
    if (!this.dataset.tipReady) {
      this.dataset.tipReady = "1";
      this.dataset.restTitle = this.getAttribute("title") || "";
    }
    const tip = this.dataset.restTitle || label;
    if (collapsed) {
      this.setAttribute("title", tip);
      this.setAttribute("aria-label", tip);
    } else {
      if (this.dataset.restTitle) this.setAttribute("title", this.dataset.restTitle);
      else this.removeAttribute("title");
      this.removeAttribute("aria-label");
    }
  });
}

function markNav() {
  const path = window.location.pathname;
  $(".side-link").each(function () {
    const href = $(this).attr("href");
    if (!href) return;
    const on = href === "/" ? path === "/" : path === href || path.indexOf(href + "/") === 0;
    $(this).toggleClass("active", on);
    if (on) this.setAttribute("aria-current", "page");
    else this.removeAttribute("aria-current");
  });
  $(".subnav a").each(function () {
    const on = $(this).attr("href") === path;
    $(this).toggleClass("active", on);
    if (on) this.setAttribute("aria-current", "page");
    else this.removeAttribute("aria-current");
  });
  const dashOpen = path.indexOf("/dashboard") === 0;
  if (dashOpen) $("#dash-group").addClass("open");
  $("#dash-toggle").attr("aria-expanded", $("#dash-group").hasClass("open") ? "true" : "false");
  placeDashFlyout();
}

function placeDashFlyout() {
  const menu = document.querySelector("#dash-group .subnav");
  const button = document.getElementById("dash-toggle");
  const group = document.getElementById("dash-group");
  if (!menu || !button || !group) return;
  const rail = window.matchMedia("(max-width: 1023px) and (min-width: 768px)").matches;
  if (rail && group.classList.contains("open")) {
    const rect = button.getBoundingClientRect();
    const sidebar = button.closest(".sidebar").getBoundingClientRect();
    menu.style.top = Math.max(8, rect.top) + "px";
    menu.style.insetInlineStart = (sidebar.right + 8) + "px";
  } else {
    menu.style.top = "";
    menu.style.insetInlineStart = "";
  }
}

function setNavOpen(open) {
  $(".app-shell").toggleClass("nav-open", open);
  $("#menu-toggle").attr("aria-expanded", open ? "true" : "false");
  $("#nav-scrim").prop("hidden", !open);
}

function profileRole(me) {
  const role = me && me.roleName ? me.roleName : "";
  if (me && me.superAdmin && openedOrg()) return role ? role + " (viewing)" : "Viewing";
  return role;
}

$(function () {
  if ($("body").data("page") !== "login" && $("body").data("page") !== "setup") {
    requireAuth();
    const main = document.querySelector("main");
    if (main && !main.id) main.id = "main";
    markNav();
    syncNavTips();
    const navQuery = window.matchMedia("(max-width: 1023px)");
    if (navQuery.addEventListener) navQuery.addEventListener("change", function () { syncNavTips(); placeDashFlyout(); });
    else if (navQuery.addListener) navQuery.addListener(function () { syncNavTips(); placeDashFlyout(); });
    window.addEventListener("resize", placeDashFlyout);
    $(".side-scroll").on("scroll", placeDashFlyout);
    if (!$("main tbody").length) $(".search").attr("hidden", true);
    const today = new Date();
    $("#today-pill").text("As of " + formatAsOf(today));
    api("GET", "/api/auth/me").done(function (res) {
      const name = res.data.username || "Signed in";
      $("#profile-name").text(name);
      $("#profile-initial").text(name.slice(0, 1).toUpperCase());
      const roleText = profileRole(res.data);
      if (roleText) $("#profile-role").text(roleText);
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
    setNavOpen(!$(".app-shell").hasClass("nav-open"));
    syncNavTips();
  });
  $("#nav-scrim").on("click", function () {
    setNavOpen(false);
    syncNavTips();
  });
  $(document).on("keydown", function (event) {
    if (event.key === "Escape" && $(".app-shell").hasClass("nav-open")) {
      setNavOpen(false);
      syncNavTips();
      $("#menu-toggle").trigger("focus");
    }
  });
  $("#dash-toggle").on("click", function () {
    const open = $("#dash-group").toggleClass("open").hasClass("open");
    $(this).attr("aria-expanded", open ? "true" : "false");
    placeDashFlyout();
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
