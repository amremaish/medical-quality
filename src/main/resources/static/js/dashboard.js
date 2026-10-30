function icon(name) {
  return "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><use href='/icons/sprite.svg#" + name + "'></use></svg>";
}

function esc(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

function countPhrase(count, singular, plural) {
  return count + " " + (count === 1 ? singular : plural);
}

function canOpen(href) {
  return typeof screenAllowed === "function" && screenAllowed(href, currentMe);
}

function attentionPill(href, tone, name, value, label, note, late) {
  const open = canOpen(href);
  const tag = open ? "a" : "div";
  return "<" + tag + " class='stat-pill'" + (open ? " href='" + href + "'" : "") + "><span class='disc " + tone + "'>" + icon(name) +
    "</span><div><b>" + value + "</b><small>" + esc(label) + "</small><small" +
    (late ? " class='late'" : "") + ">" + esc(note) + "</small></div></" + tag + ">";
}

function paintAttention(row) {
  $("#attention-pills").html([
    attentionPill(
      "/audits",
      "blue",
      "clipboard",
      row.pendingAudits,
      "Pending audits",
      auditAttentionNote(row),
      row.overdueAudits > 0),
    attentionPill(
      "/findings",
      row.findingsNeedingAction > 0 ? "red" : "orange",
      "alert",
      row.openFindings,
      "Open findings",
      findingAttentionNote(row),
      row.findingsNeedingAction > 0),
    attentionPill(
      "/capas",
      row.overdueCapas > 0 ? "red" : "orange",
      "capa",
      row.openCapas,
      "Open CAPA",
      capaAttentionNote(row),
      row.overdueCapas > 0)
  ].join(""));
  const items = row.attention || [];
  if (!items.length) {
    $("#attention-rows").html("<tr><td colspan='4' class='empty'>Nothing is past due. Every open finding has a CAPA, and none has been open more than 30 days.</td></tr>");
    $("#attention-more").prop("hidden", true).text("");
    return;
  }
  $("#attention-rows").html(items.map(function (item) {
    const tone = item.tone === "danger" ? "danger" : "warning";
    const title = "<span class='kind " + tone + "'>" + esc(item.kind) + "</span><span class='item-title' dir='auto'>" +
      esc(item.title) + "</span>";
    const cell = canOpen(item.href)
      ? "<a href='" + esc(item.href) + "' title='" + esc(item.title) + "'>" + title + "</a>"
      : title;
    return "<tr><td>" + cell + "</td><td dir='auto'>" + esc(item.department) + "</td><td dir='auto'>" +
      esc(item.owner) + "</td><td class='nowrap'>" + esc(item.age) + "</td></tr>";
  }).join(""));
  const extra = (row.attentionTotal || items.length) - items.length;
  $("#attention-more").prop("hidden", extra <= 0).text(
    extra > 0 ? "Showing " + items.length + " of " + row.attentionTotal + "." : ""
  );
}

function segment(count, total, css) {
  if (!count || !total) return "";
  return "<span class='" + css + "' style='width:" + (count / total * 100) + "%'></span>";
}

function legendItem(css, label) {
  return "<span><i class='" + css + "'></i>" + esc(label) + "</span>";
}

function paintCompliance(row) {
  if (!row.audits) {
    $("#compliance").html("<h2>Compliance</h2><p class='empty'>No audits yet. The score appears after an audit is recorded.</p>");
    $("#by-department").html("<h2>By department</h2><p class='empty'>Department scores appear with the first audit.</p>");
    return;
  }
  const total = (row.met || 0) + (row.partial || 0) + (row.notMet || 0) + (row.notApplicable || 0) + (row.unscored || 0);
  const label = "Element mix, separate from the mean: " +
    countPhrase(row.met || 0, "met", "met") + ", " +
    countPhrase(row.partial || 0, "partial", "partial") + ", " +
    countPhrase(row.notMet || 0, "not met", "not met") + ", " +
    countPhrase(row.notApplicable || 0, "not applicable", "not applicable") + ", " +
    countPhrase(row.unscored || 0, "not scored", "not scored");
  $("#compliance").html(
    "<h2>Compliance</h2>" +
    "<p class='compliance-figure'><b>" + row.averageCompliance + "%</b><span>mean</span></p>" +
    "<details class='calc-note'><summary>How this is calculated</summary>" +
    "<p>Mean of every audit. Met counts fully, partial counts half, and not-applicable elements are excluded. An audit with no scores counts as 0%.</p></details>" +
    "<h3 class='mix-heading'>Element mix</h3>" +
    "<div class='bar' role='img' aria-label='" + esc(label) + "'>" +
      segment(row.met, total, "met") +
      segment(row.partial, total, "partial") +
      segment(row.notMet, total, "not") +
      segment(row.notApplicable, total, "na") +
      segment(row.unscored, total, "unscored") +
    "</div>" +
    "<div class='legend'>" +
      legendItem("met", (row.met || 0) + " met") +
      legendItem("partial", (row.partial || 0) + " partial") +
      legendItem("not", (row.notMet || 0) + " not met") +
      legendItem("na", (row.notApplicable || 0) + " not applicable") +
      legendItem("unscored", (row.unscored || 0) + " not scored") +
    "</div>"
  );
  const rows = row.departmentsByCompliance || [];
  if (!rows.length) {
    $("#by-department").html("<h2>By department</h2><p class='empty'>No department has an audit yet.</p>");
    return;
  }
  const caption = row.scoredDepartments > rows.length
    ? "Lowest " + rows.length + " of " + row.scoredDepartments + " departments with an audit."
    : "Departments with an audit, lowest score first.";
  $("#by-department").html(
    "<h2>By department</h2><p class='quiet-text'>" + esc(caption) + "</p><ul class='score-list'>" +
    rows.map(function (item) {
      const audits = countPhrase(item.audits, "audit", "audits");
      const href = item.departmentId != null && item.departmentId !== ""
        ? "/audits?departmentId=" + encodeURIComponent(item.departmentId) + "&department=" + encodeURIComponent(item.name || "")
        : "/audits?department=" + encodeURIComponent(item.name || "");
      const bar = item.percent < row.averageCompliance ? "score below" : "score";
      const body = "<span class='name' dir='auto' title='" + esc(item.name) + "'>" +
        esc(item.name) + "<small>" + esc(audits) + "</small></span><b>" + item.percent + "%</b>" +
        "<span class='bar' aria-hidden='true'><span class='" + bar + "' style='width:" +
        Math.max(0, Math.min(100, item.percent)) + "%'></span></span>";
      const rowHtml = canOpen(href)
        ? "<a class='score-row' href='" + href + "'>" + body + "</a>"
        : "<div class='score-row'>" + body + "</div>";
      return "<li>" + rowHtml + "</li>";
    }).join("") +
    "</ul>"
  );
}

function totalLink(href, label) {
  const text = esc(label);
  return canOpen(href) ? "<a href='" + href + "'>" + text + "</a>" : "<span>" + text + "</span>";
}

function paintTotals(row) {
  $("#totals").html([
    totalLink("/departments", countPhrase(row.departments, "department", "departments")),
    totalLink("/audits", countPhrase(row.audits, "audit", "audits") + ", " + countPhrase(row.completedAudits, "completed", "completed")),
    totalLink("/evidence", countPhrase(row.evidence, "evidence file", "evidence files")),
    totalLink("/assessors", countPhrase(row.assessors, "assessor", "assessors"))
  ].join("<span class='sep' aria-hidden='true'>·</span>"));
}

function paint(row) {
  const title = $("#board").data("title") || "Command center";
  const asOf = formatAsOf(new Date());
  $("#topbar-title").text(title).prop("hidden", false);
  $("#board-scope").text("All audits on record, as of " + asOf + ".");
  $("#global-search").attr("placeholder", "Search attention items");
  paintAttention(row);
  paintCompliance(row);
  paintTotals(row);
}

$(function () {
  $.when(api("GET", "/api/dashboard"), api("GET", "/api/auth/me")).done(function (dash, me) {
    if (me[0] && me[0].data) currentMe = me[0].data;
    paint(dash[0].data);
  }).fail(function (xhr) {
    $("#err").text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "Dashboard unavailable");
  });
});
