function loadAudits() {
  const params = new URLSearchParams(window.location.search);
  const departmentId = params.get("departmentId");
  const departmentName = params.get("department");
  api("GET", "/api/audits").done(function (res) {
    const filtered = (res.data || []).filter(function (audit) {
      if (departmentId) return String(audit.departmentId) === String(departmentId);
      if (departmentName) return (audit.departmentName || "") === departmentName;
      return true;
    });
    const rows = filtered.map(function (audit) {
      return "<tr><td>" + audit.auditDate + "</td><td><a href='/audits/" + audit.id + "' dir='auto'>" +
        escapeHtml(audit.departmentName) + "</a></td><td dir='auto'>" + escapeHtml(audit.auditorName) +
        "</td><td><span class='pill'>" + escapeHtml(mqStatus(audit.status)) + "</span></td><td>" + audit.percent + "%</td></tr>";
    }).join("");
    const narrowed = !!(departmentId || departmentName);
    $("#rows").html(rows || "<tr><td colspan='5'>" + escapeHtml(narrowed ? mqText("No audits for this department", "لا توجد تدقيقات لهذا القسم") : mqText("No audits yet", "لا توجد تدقيقات بعد")) + "</td></tr>");
    const filter = $("#audit-filter");
    if (!filter.length || !narrowed) return;
    const named = departmentName || (filtered[0] && filtered[0].departmentName) || "";
    filter.prop("hidden", false).html(
      (named
        ? mqText("Showing audits for ", "عرض تدقيقات ") + "<b dir='auto'>" + escapeHtml(named) + "</b>. "
        : mqText("Showing audits for this department. ", "عرض تدقيقات هذا القسم. ")) +
      "<a href='/audits'>" + escapeHtml(mqText("Show all audits", "عرض كل التدقيقات")) + "</a>"
    );
  });
}

function escapeHtml(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

let auditDepartments = [];
function departmentLabel(row) {
  return mqLang() === "ar" ? (row.nameAr || row.nameEn || row.code || "") : (row.nameEn || row.nameAr || row.code || "");
}
function paintAuditDepartments() {
  if (!$("#departmentId").length) return;
  const current = $("#departmentId").val();
  const previous = $("#departmentId option:selected").text();
  const none = "<option value=''>" + escapeHtml(mqText("None", "بدون")) + "</option>";
  $("#departmentId").html(none + auditDepartments.map(function (department) {
    return "<option value='" + department.id + "'>" + escapeHtml(departmentLabel(department)) + "</option>";
  }).join(""));
  if (current) $("#departmentId").val(current);
  if (current && $("#departmentName").val() === previous) {
    $("#departmentName").val($("#departmentId option:selected").text());
  }
}

document.addEventListener("mq-lang", function () {
  if ($("#rows").length && window.location.pathname === "/audits") loadAudits();
  if (auditDepartments.length) paintAuditDepartments();
});

$(function () {
  if ($("#rows").length && !$("#create-audit").length) loadAudits();
  if (!$("#create-audit").length) return;
  const today = new Date().toISOString().slice(0, 10);
  $("#auditDate").val(today);
  api("GET", "/api/departments").done(function (res) {
    auditDepartments = res.data || [];
    paintAuditDepartments();
  });
  $("#departmentId").on("change", function () {
    const text = $(this).find("option:selected").text();
    if ($(this).val()) {
      $("#departmentName").val(text);
    }
  });
  $("#create-audit").on("submit", function (event) {
    event.preventDefault();
    const departmentId = $("#departmentId").val();
    api("POST", "/api/audits", {
      departmentName: $("#departmentName").val(),
      auditorName: $("#auditorName").val(),
      auditDate: $("#auditDate").val(),
      departmentId: departmentId ? Number(departmentId) : null
    }).done(function (res) {
      window.location.href = "/audits/" + res.data.id;
    }).fail(function (xhr) {
      $("#form-error").text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : mqText("Could not create audit", "تعذر إنشاء التدقيق"));
    });
  });
});
