function icon(name) {
  return "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><use href='/icons/sprite.svg#" + name + "'></use></svg>";
}

function bars(completed, pending) {
  const max = Math.max(completed, pending, 1);
  const height = function (value) { return Math.max(8, Math.round(value / max * 120)); };
  return "<div class='bars'><div><i style='height:" + height(completed) + "px'></i><b>" + completed + "</b><span>Completed</span></div>" +
    "<div><i class='soft' style='height:" + height(pending) + "px'></i><b>" + pending + "</b><span>Pending</span></div></div>";
}

function ring(percent) {
  const radius = 58;
  const length = 2 * Math.PI * radius;
  const dash = (Math.max(0, Math.min(100, percent)) / 100) * length;
  return "<svg class='ring' viewBox='0 0 140 140'><circle cx='70' cy='70' r='" + radius +
    "' fill='none' stroke='#e6edf5' stroke-width='12'/><circle cx='70' cy='70' r='" + radius +
    "' fill='none' stroke='#1f6fe5' stroke-width='12' stroke-linecap='round' stroke-dasharray='" +
    dash + " " + length + "' transform='rotate(-90 70 70)'/><text x='70' y='76' text-anchor='middle' font-size='28'>" +
    percent + "%</text></svg>";
}

function paint(row) {
  const title = $("#board").data("title") || "Command center";
  const name = $("#profile-name").text() || "there";
  $("#hero").html(
    "<div><p>Good morning</p><h1>" + name + "</h1><p>" + title + ". Your quality work is ready.</p>" +
    "<div class='pills'>" +
    pill("blue", "calendar", row.pendingAudits, "Pending audits") +
    pill("orange", "alert", row.openFindings, "Open findings") +
    pill("green", "shield", row.openCapas, "Open CAPA") +
    "</div></div><div class='hero-art'><span>" + icon("stethoscope") + "</span><span>" + icon("clipboard") +
    "</span><span>" + icon("heart") + "</span></div>"
  );
  $("#kpis").html([
    ["users", "Departments", row.departments, "/departments"],
    ["clipboard", "Audits", row.audits, "/audits"],
    ["calendar", "Completed", row.completedAudits, "/audits"],
    ["chart", "Compliance", row.averageCompliance + "%", "/dashboard"]
  ].map(function (card) {
    return "<a class='kpi' href='" + card[3] + "'><span class='disc blue'>" + icon(card[0]) +
      "</span><div><b>" + card[2] + "</b><span>" + card[1] + "</span></div></a>";
  }).join(""));
  $("#icon-stats").html([
    ["clipboard", row.audits, "Audits", "/audits"],
    ["stethoscope", "View", "Assessors", "/assessors"],
    ["users", row.departments, "Departments", "/departments"],
    ["alert", row.openFindings, "Findings", "/findings"],
    ["shield", row.openCapas, "CAPA", "/capas"],
    ["card", row.evidence, "Evidence", "/evidence"]
  ].map(function (card) {
    return "<a class='icon-stat' href='" + card[3] + "'><span class='disc'>" + icon(card[0]) +
      "</span><b>" + card[1] + "</b><span>" + card[2] + "</span></a>";
  }).join(""));
  $("#bars").html("<h2>Audit progress</h2>" + bars(row.completedAudits, row.pendingAudits));
  $("#ring").html("<h2>Average compliance</h2>" + ring(row.averageCompliance));
  $("#mini").html([
    ["alert", row.openFindings, "Open findings"],
    ["shield", row.openCapas, "Open CAPA"],
    ["card", row.evidence, "Evidence files"]
  ].map(function (card) {
    return "<div class='kpi'><span class='disc orange'>" + icon(card[0]) + "</span><div><b>" + card[1] +
      "</b><span>" + card[2] + "</span></div></div>";
  }).join(""));
}

function pill(tone, name, value, label) {
  return "<div class='stat-pill'><span class='disc " + tone + "'>" + icon(name) + "</span><div><b>" + value +
    "</b><small>" + label + "</small></div></div>";
}

$(function () {
  $.when(api("GET", "/api/dashboard"), api("GET", "/api/auth/me")).done(function (dash, me) {
    const name = me[0].data.username || "there";
    $("#profile-name").text(name);
    $("#profile-initial").text(name.slice(0, 1).toUpperCase());
    paint(dash[0].data);
  }).fail(function (xhr) {
    $("#err").text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "Dashboard unavailable");
  });
});
