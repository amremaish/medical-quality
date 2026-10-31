function authHeaders() {
  const headers = {
    Authorization: "Bearer " + (sessionStorage.getItem("mq_token") || ""),
    "Accept-Language": window.mqLang ? mqLang() : "en"
  };
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

function screenPath(path) {
  const value = path || "";
  const cut = value.search(/[?#]/);
  return cut >= 0 ? value.slice(0, cut) : value;
}

function screenAllowed(path, me) {
  path = screenPath(path);
  if (!me) return false;
  if (path === "/account/password" || path === "/account/setup" || path === "/login") return true;
  if (isPlatform(path)) return !!me.superAdmin;
  if (me.superAdmin && !openedOrg()) return false;
  if (path === "/users/new" || path.indexOf("/users/new/") === 0
      || path === "/departments/new" || path.indexOf("/departments/new/") === 0
      || /^\/departments\/\d+$/.test(path)) {
    return directoryAdmin(me);
  }
  if (path === "/roles" || path.indexOf("/roles/") === 0) return directoryAdmin(me);
  if (path === "/users" || path.indexOf("/users/") === 0) {
    return directoryAdmin(me) || holds(me, "MANAGE_USER_PERMISSION_OVERRIDES");
  }
  if (path === "/assessors" || path.indexOf("/assessors/") === 0) {
    return directoryAdmin(me) || holds(me, "MANAGE_ASSESSORS");
  }
  if (path === "/quality-policy") {
    return holds(me, "QUALITY_POLICY_VIEW") || holds(me, "QUALITY_POLICY_EDIT");
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
  if (path === "/audits" || path.indexOf("/audits/") === 0) return "CONDUCT_AUDIT";
  if (path === "/record-reviews" || path.indexOf("/record-reviews/") === 0) return "CONDUCT_AUDIT";
  if (path === "/findings" || path.indexOf("/findings/") === 0) return "MANAGE_FINDINGS";
  if (path === "/capas" || path.indexOf("/capas/") === 0) return "MANAGE_CAPA";
  if (path === "/evidence" || path.indexOf("/evidence/") === 0) return "MANAGE_EVIDENCE";
  if (path === "/departments" || path.indexOf("/departments/") === 0) return "MANAGE_DEPARTMENTS";
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
  if (path === "/dashboard/executive") return mqText("Executive view", "العرض التنفيذي");
  if (path === "/dashboard/assessor") return mqText("Assessor view", "عرض المقيّم");
  if (path === "/dashboard/quality-director") return mqText("Quality director view", "عرض مدير الجودة");
  return mqText("Command center", "مركز القيادة");
}

function auditAttentionNote(row) {
  if (!row.pendingAudits) return mqText("None pending", "لا شيء معلّق");
  if (!row.overdueAudits) return mqText("None past the audit date", "لا شيء تجاوز تاريخ التدقيق");
  return mqCount(row.overdueAudits, "past the audit date", "past the audit date", "تجاوز تاريخ التدقيق", "تجاوزت تاريخ التدقيق");
}

function findingAttentionNote(row) {
  if (!row.openFindings) return mqText("None open", "لا شيء مفتوح");
  const count = row.findingsNeedingAction || 0;
  if (!count) return mqText("Each open finding has a CAPA", "كل ملاحظة مفتوحة لها إجراء");
  return count === 1
    ? mqText("1 needs a CAPA or has been open more than 30 days", "واحدة تحتاج إجراء أو مفتوحة أكثر من 30 يوماً")
    : mqText(count + " need a CAPA or have been open more than 30 days", count + " تحتاج إجراء أو مفتوحة أكثر من 30 يوماً");
}

function capaAttentionNote(row) {
  if (!row.openCapas) return mqText("None open", "لا شيء مفتوح");
  if (!row.overdueCapas) return mqText("None past the target date", "لا شيء تجاوز تاريخ الاستحقاق");
  return mqCount(row.overdueCapas, "past the target date", "past the target date", "تجاوز تاريخ الاستحقاق", "تجاوزت تاريخ الاستحقاق");
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
  const months = mqLang() === "ar"
    ? ["يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"]
    : ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
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
  if (!row.audits) return mqText("No audits yet", "لا توجد تدقيقات بعد");
  if (!row.pendingAudits) return mqCount(row.audits, "audit, none pending", "audits, none pending", "تدقيق، لا شيء معلّق", "تدقيقات، لا شيء معلّق");
  if (row.pendingAudits === row.audits) return mqCount(row.pendingAudits, "audit in progress", "audits in progress", "تدقيق قيد التنفيذ", "تدقيقات قيد التنفيذ");
  return mqText(row.pendingAudits + " pending of " + row.audits, row.pendingAudits + " معلّق من " + row.audits);
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
  const board = firstDashboard();
  const compliance = row.audits ? row.averageCompliance + "%" : "—";
  const complianceLabel = row.audits ? mqText("Compliance", "الالتزام") : mqText("No score yet", "لا توجد درجة بعد");
  const pills = [];
  if (screenAllowed("/audits", currentMe)) {
    pills.push(statusPill("/audits", "blue", "clipboard", row.pendingAudits, pluralWord(row.pendingAudits, mqText("Pending audit", "تدقيق معلّق"), mqText("Pending audits", "تدقيقات معلّقة")), auditAttentionNote(row), row.overdueAudits > 0));
  }
  if (screenAllowed("/findings", currentMe)) {
    pills.push(statusPill("/findings", row.findingsNeedingAction > 0 ? "red" : "orange", "alert", row.openFindings, mqText("Open findings", "ملاحظات مفتوحة"), findingAttentionNote(row), row.findingsNeedingAction > 0));
  }
  if (screenAllowed("/capas", currentMe)) {
    pills.push(statusPill("/capas", row.overdueCapas > 0 ? "red" : "orange", "capa", row.openCapas, mqText("Open CAPA", "إجراءات مفتوحة"), capaAttentionNote(row), row.overdueCapas > 0));
  }
  if (board) pills.push(statusPill(board, "blue", "chart", compliance, complianceLabel));
  const complianceLine = row.audits
    ? mqText(row.averageCompliance + "% compliance", row.averageCompliance + "% التزام")
    : mqText("No compliance score yet", "لا توجد درجة التزام بعد");
  if (!pills.length) {
    $("#home-status").prop("hidden", true).empty();
    $("#home-audits").text(auditLine(row));
    $("#home-compliance").text(complianceLine);
    return;
  }
  $("#home-status").prop("hidden", false).html(pills.join(""));
  $("#home-audits").text(auditLine(row));
  $("#home-compliance").text(complianceLine);
}

function loadHospitalPulse() {
  if (!document.body.classList.contains("is-org")) return;
  if (firstDashboard()) {
    api("GET", "/api/dashboard").done(function (res) {
      const row = res.data;
      const findingsUrgent = row.findingsNeedingAction || 0;
      const capasLate = row.overdueCapas || 0;
      setShellCount("#findings-link", "#findings-count", findingsUrgent,
        findingsUrgent ? mqCount(findingsUrgent, "finding needs action", "findings need action", "ملاحظة تحتاج متابعة", "ملاحظات تحتاج متابعة") : mqText("Findings", "الملاحظات"),
        "warning");
      setShellCount("#capa-link", "#capa-count", capasLate,
        capasLate ? mqCount(capasLate, "CAPA past its target date", "CAPAs past their target date", "إجراء تجاوز تاريخه", "إجراءات تجاوزت تاريخها") : mqText("CAPA", "الإجراءات التصحيحية"),
        "danger");
      paintHome(row);
    });
  }
  if ($("#home-records").length) {
    api("GET", "/api/record-reviews").done(function (res) {
      const count = (res.data || []).length;
      $("#home-records").text(count ? mqCount(count, "review", "reviews", "مراجعة", "مراجعات") : mqText("No reviews yet", "لا توجد مراجعات بعد"));
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
    $("#brand-org").text(mqText("Quality platform", "منصة الجودة"));
    $("#org-line").text(mqText("Quality platform for hospitals", "منصة جودة للمستشفيات"));
    $(".brand").attr("href", "/organizations");
  } else {
    document.body.classList.add("is-org");
    $("#brand-org").text(me.organizationName || mqText("Quality platform", "منصة الجودة"));
    $("#org-line").text(me.organizationName || mqText("Quality platform for hospitals", "منصة جودة للمستشفيات"));
  }
  applyControls();
  if (directoryAdmin(me)) $(".side-link[href='/assessors']").prop("hidden", false);
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
    const rtl = document.documentElement.dir === "rtl";
    menu.style.insetInlineStart = (rtl ? (window.innerWidth - sidebar.left + 8) : (sidebar.right + 8)) + "px";
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
  const role = window.mqRole ? mqRole(me && me.roleCode, me && me.roleName) : (me && me.roleName ? me.roleName : "");
  if (me && me.superAdmin && openedOrg()) return role ? role + mqText(" (viewing)", " (عرض)") : mqText("Viewing", "عرض");
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
    $("#today-pill").text(mqText("As of ", "حتى ") + formatAsOf(today));
    api("GET", "/api/auth/me").done(function (res) {
      const name = res.data.username || mqText("Signed in", "تم تسجيل الدخول");
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
  document.addEventListener("mq-lang", function () {
    if ($("#today-pill").length) $("#today-pill").text(mqText("As of ", "حتى ") + formatAsOf(new Date()));
    if (currentMe) {
      const name = currentMe.username || mqText("Signed in", "تم تسجيل الدخول");
      $("#profile-name").text(name);
      const roleText = profileRole(currentMe);
      if (roleText) $("#profile-role").text(roleText);
      showNav(currentMe);
    }
    syncNavTips();
    placeDashFlyout();
  });
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
