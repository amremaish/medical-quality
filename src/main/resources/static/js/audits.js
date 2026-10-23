function loadAudits() {
  api("GET", "/api/audits").done(function (res) {
    const rows = res.data.map(function (audit) {
      return "<tr><td>" + audit.auditDate + "</td><td><a href='/audits/" + audit.id + "'>" +
        escapeHtml(audit.departmentName) + "</a></td><td>" + escapeHtml(audit.auditorName) +
        "</td><td><span class='pill'>" + escapeHtml(audit.status) + "</span></td><td>" + audit.percent + "%</td></tr>";
    }).join("");
    $("#rows").html(rows || "<tr><td colspan='5'>No audits yet</td></tr>");
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
