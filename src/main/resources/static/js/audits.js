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
        "</td><td><span class='pill'>" + escapeHtml(audit.status) + "</span></td><td>" + audit.percent + "%</td></tr>";
    }).join("");
    const narrowed = !!(departmentId || departmentName);
    $("#rows").html(rows || "<tr><td colspan='5'>" + (narrowed ? "No audits for this department" : "No audits yet") + "</td></tr>");
    const filter = $("#audit-filter");
    if (!filter.length || !narrowed) return;
    const named = departmentName || (filtered[0] && filtered[0].departmentName) || "";
    filter.prop("hidden", false).html(
      (named
        ? "Showing audits for <b dir='auto'>" + escapeHtml(named) + "</b>. "
        : "Showing audits for this department. ") +
      "<a href='/audits'>Show all audits</a>"
    );
  });
}

function escapeHtml(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

$(function () {
  if ($("#rows").length) loadAudits();
  if (!$("#create-audit").length) return;
  const today = new Date().toISOString().slice(0, 10);
  $("#auditDate").val(today);
  api("GET", "/api/departments").done(function (res) {
    res.data.forEach(function (department) {
      $("#departmentId").append("<option value='" + department.id + "'>" + escapeHtml(department.nameAr) + "</option>");
    });
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
      $("#form-error").text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "Could not create audit");
    });
  });
});
