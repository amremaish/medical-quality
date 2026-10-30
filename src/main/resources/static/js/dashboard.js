function icon(name) {
  return "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><use href='/icons/sprite.svg#" + name + "'></use></svg>";
}

function esc(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

function countPhrase(count, singular, plural, singularAr, pluralAr) {
  return mqCount(count, singular, plural, singularAr, pluralAr);
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
      mqText("Pending audits", "تدقيقات معلّقة"),
      auditAttentionNote(row),
      row.overdueAudits > 0),
    attentionPill(
      "/findings",
      row.findingsNeedingAction > 0 ? "red" : "orange",
      "alert",
      row.openFindings,
      mqText("Open findings", "ملاحظات مفتوحة"),
      findingAttentionNote(row),
      row.findingsNeedingAction > 0),
    attentionPill(
      "/capas",
      row.overdueCapas > 0 ? "red" : "orange",
      "capa",
      row.openCapas,
      mqText("Open CAPA", "إجراءات مفتوحة"),
      capaAttentionNote(row),
      row.overdueCapas > 0)
  ].join(""));
  const items = row.attention || [];
  if (!items.length) {
    $("#attention-rows").html("<tr><td colspan='4' class='empty'>" + esc(mqText(
      "Nothing is past due. Every open finding has a CAPA, and none has been open more than 30 days.",
      "لا شيء متأخر. كل ملاحظة مفتوحة لها إجراء، ولا توجد ملاحظة مفتوحة أكثر من 30 يوماً.")) + "</td></tr>");
    $("#attention-more").prop("hidden", true).text("");
    return;
  }
  $("#attention-rows").html(items.map(function (item) {
    const tone = item.tone === "danger" ? "danger" : "warning";
    const title = "<span class='kind " + tone + "'>" + esc(mqKind(item.kind)) + "</span><span class='item-title' dir='auto'>" +
      esc(mqStatus(item.title)) + "</span>";
    const cell = canOpen(item.href)
      ? "<a href='" + esc(item.href) + "' title='" + esc(item.title) + "'>" + title + "</a>"
      : title;
    return "<tr><td>" + cell + "</td><td dir='auto'>" + esc(item.department) + "</td><td dir='auto'>" +
      esc(mqPerson(item.owner)) + "</td><td class='nowrap'>" + esc(mqAge(item.age)) + "</td></tr>";
  }).join(""));
  const extra = (row.attentionTotal || items.length) - items.length;
  $("#attention-more").prop("hidden", extra <= 0).text(
    extra > 0 ? mqText("Showing " + items.length + " of " + row.attentionTotal + ".", "عرض " + items.length + " من " + row.attentionTotal + ".") : ""
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
    $("#compliance").html("<h2>" + esc(mqText("Compliance", "الالتزام")) + "</h2><p class='empty'>" +
      esc(mqText("No audits yet. The score appears after an audit is recorded.", "لا توجد تدقيقات بعد. تظهر الدرجة بعد تسجيل تدقيق.")) + "</p>");
    $("#by-department").html("<h2>" + esc(mqText("By department", "حسب القسم")) + "</h2><p class='empty'>" +
      esc(mqText("Department scores appear with the first audit.", "تظهر درجات الأقسام مع أول تدقيق.")) + "</p>");
    return;
  }
  const total = (row.met || 0) + (row.partial || 0) + (row.notMet || 0) + (row.notApplicable || 0) + (row.unscored || 0);
  const label = mqText("Element mix, separate from the mean: ", "مزيج العناصر، منفصل عن المتوسط: ") +
    countPhrase(row.met || 0, "met", "met", "مستوفى", "مستوفى") + ", " +
    countPhrase(row.partial || 0, "partial", "partial", "جزئي", "جزئي") + ", " +
    countPhrase(row.notMet || 0, "not met", "not met", "غير مستوفى", "غير مستوفى") + ", " +
    countPhrase(row.notApplicable || 0, "not applicable", "not applicable", "لا ينطبق", "لا ينطبق") + ", " +
    countPhrase(row.unscored || 0, "not scored", "not scored", "غير مُقيَّم", "غير مُقيَّم");
  $("#compliance").html(
    "<h2>" + esc(mqText("Compliance", "الالتزام")) + "</h2>" +
    "<p class='compliance-figure'><b>" + row.averageCompliance + "%</b><span>" + esc(mqText("mean", "المتوسط")) + "</span></p>" +
    "<details class='calc-note'><summary>" + esc(mqText("How this is calculated", "كيف تُحسب")) + "</summary>" +
    "<p>" + esc(mqText(
      "Mean of every audit. Met counts fully, partial counts half, and not-applicable elements are excluded. An audit with no scores counts as 0%.",
      "متوسط كل التدقيقات. المستوفى يُحتسب كاملاً، والجزئي نصفاً، وتُستبعد العناصر التي لا تنطبق. التدقيق بلا درجات يُحتسب 0%.")) + "</p></details>" +
    "<h3 class='mix-heading'>" + esc(mqText("Element mix", "مزيج العناصر")) + "</h3>" +
    "<div class='bar' role='img' aria-label='" + esc(label) + "'>" +
      segment(row.met, total, "met") +
      segment(row.partial, total, "partial") +
      segment(row.notMet, total, "not") +
      segment(row.notApplicable, total, "na") +
      segment(row.unscored, total, "unscored") +
    "</div>" +
    "<div class='legend'>" +
      legendItem("met", countPhrase(row.met || 0, "met", "met", "مستوفى", "مستوفى")) +
      legendItem("partial", countPhrase(row.partial || 0, "partial", "partial", "جزئي", "جزئي")) +
      legendItem("not", countPhrase(row.notMet || 0, "not met", "not met", "غير مستوفى", "غير مستوفى")) +
      legendItem("na", countPhrase(row.notApplicable || 0, "not applicable", "not applicable", "لا ينطبق", "لا ينطبق")) +
      legendItem("unscored", countPhrase(row.unscored || 0, "not scored", "not scored", "غير مُقيَّم", "غير مُقيَّم")) +
    "</div>"
  );
  const rows = row.departmentsByCompliance || [];
  if (!rows.length) {
    $("#by-department").html("<h2>" + esc(mqText("By department", "حسب القسم")) + "</h2><p class='empty'>" +
      esc(mqText("No department has an audit yet.", "لا يوجد قسم له تدقيق بعد.")) + "</p>");
    return;
  }
  const caption = row.scoredDepartments > rows.length
    ? mqText("Lowest " + rows.length + " of " + row.scoredDepartments + " departments with an audit.", "أدنى " + rows.length + " من " + row.scoredDepartments + " أقسام لها تدقيق.")
    : mqText("Departments with an audit, lowest score first.", "الأقسام التي لها تدقيق، الأدنى أولاً.");
  $("#by-department").html(
    "<h2>" + esc(mqText("By department", "حسب القسم")) + "</h2><p class='quiet-text'>" + esc(caption) + "</p><ul class='score-list'>" +
    rows.map(function (item) {
      const audits = countPhrase(item.audits, "audit", "audits", "تدقيق", "تدقيقات");
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
    totalLink("/departments", countPhrase(row.departments, "department", "departments", "قسم", "أقسام")),
    totalLink("/audits", countPhrase(row.audits, "audit", "audits", "تدقيق", "تدقيقات") + mqText(", ", "، ") + countPhrase(row.completedAudits, "completed", "completed", "مكتمل", "مكتملة")),
    totalLink("/evidence", countPhrase(row.evidence, "evidence file", "evidence files", "ملف دليل", "ملفات أدلة")),
    totalLink("/assessors", countPhrase(row.assessors, "assessor", "assessors", "مقيّم", "مقيّمون"))
  ].join("<span class='sep' aria-hidden='true'>·</span>"));
}

function boardTitle(raw) {
  if (raw === "Executive view") return mqText("Executive view", "العرض التنفيذي");
  if (raw === "Assessor view") return mqText("Assessor view", "عرض المقيّم");
  if (raw === "Quality director view") return mqText("Quality director view", "عرض مدير الجودة");
  return mqText("Command center", "مركز القيادة");
}

function paint(row) {
  const title = boardTitle($("#board").data("title") || "Command center");
  const asOf = formatAsOf(new Date());
  document.title = title;
  $("#topbar-title").text(title).prop("hidden", false);
  $("#board-scope").text(mqText("All audits on record, as of ", "كل التدقيقات المسجلة، حتى ") + asOf + ".");
  $("#global-search").attr("placeholder", mqText("Search attention items", "ابحث في عناصر المتابعة"));
  paintAttention(row);
  paintCompliance(row);
  paintTotals(row);
}

let boardRow = null;

function loadBoard() {
  $.when(api("GET", "/api/dashboard"), api("GET", "/api/auth/me")).done(function (dash, me) {
    if (me[0] && me[0].data) currentMe = me[0].data;
    boardRow = dash[0].data;
    paint(boardRow);
  }).fail(function (xhr) {
    $("#err").text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : mqText("Dashboard unavailable", "لوحة المعلومات غير متاحة"));
  });
}

document.addEventListener("mq-lang", function () {
  if (boardRow) paint(boardRow);
  loadBoard();
});

$(function () { loadBoard(); });
