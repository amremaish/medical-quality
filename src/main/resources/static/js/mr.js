let review = null;
let current = "MR.1";

function reviewId() { return $("body").data("review-id"); }

function esc(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

function loadReview() {
  api("GET", "/api/record-reviews/" + reviewId()).done(function (res) {
    review = res.data;
    render();
  });
}

function recordByCode(code) {
  return review.records.filter(function (row) { return row.code === code; })[0];
}

function answerFor(record, itemId) {
  return record.answers.filter(function (row) { return row.itemId === itemId; })[0];
}

function saveAnswer(itemId, patch) {
  const record = recordByCode(current);
  const answer = answerFor(record, itemId);
  const body = {
    recordCode: current,
    itemId: itemId,
    legible: answer.legible,
    accurate: answer.accurate,
    complete: answer.complete,
    na: answer.na,
    comment: answer.comment || ""
  };
  Object.keys(patch).forEach(function (key) { body[key] = patch[key]; });
  api("PUT", "/api/record-reviews/" + reviewId() + "/answers", body).done(function (res) {
    review = res.data;
    render();
  });
}

function canReview() {
  return typeof hasPerm === "function" && hasPerm("CONDUCT_AUDIT");
}

function render() {
  const record = recordByCode(current);
  let html = "<h1>" + esc(mqText("Patient medical record review", "مراجعة السجل الطبي للمريض")) + "</h1>";
  html += "<p>" + esc(review.hospitalName) + " · " + esc(review.reviewerName) + " · " + review.reviewDate + " · " + esc(review.departmentName) + "</p>";
  html += "<div class='row'>";
  review.records.forEach(function (row) {
    html += "<div class='rec-card" + (row.code === current ? " on" : "") + "' data-code='" + row.code + "'><div>" + row.code + "</div><b>" + row.stats.overall + "%</b></div>";
  });
  html += "</div>";
  html += "<p>" + record.stats.compliant + "/" + record.stats.applicable + " " + esc(mqText("fully compliant", "مطابق بالكامل")) + " · " +
    esc(mqText("Legible", "مقروء")) + " " + record.stats.percentLegible + "% · " + esc(mqText("Accurate", "دقيق")) + " " +
    record.stats.percentAccurate + "% · " + esc(mqText("Complete", "مكتمل")) + " " + record.stats.percentComplete + "%</p>";
  if (canReview()) {
    html += "<p class='no-print'>" + esc(mqText("Patient identifier", "معرّف المريض")) + " <input id='patient-id' value='" + esc(record.patientIdentifier || "") + "'></p>";
  } else if (record.patientIdentifier) {
    html += "<p>" + esc(mqText("Patient identifier", "معرّف المريض")) + " " + esc(record.patientIdentifier) + "</p>";
  }
  html += "<p class='no-print'><button type='button' id='export-csv' class='ghost'>" + esc(mqText("Export CSV", "تصدير CSV")) + "</button> <button type='button' id='print-review' class='ghost'>" + esc(mqText("Print", "طباعة")) + "</button>";
  if (canReview()) html += " <button type='button' id='reset-review' class='danger'>" + esc(mqText("Reset", "إعادة ضبط")) + "</button>";
  html += "</p>";
  html += "<table><thead><tr><th>" + esc(mqText("Standard", "المعيار")) + "</th><th>" + esc(mqText("Evidence", "الدليل")) +
    "</th><th>" + esc(mqText("Legible", "مقروء")) + "</th><th>" + esc(mqText("Accurate", "دقيق")) + "</th><th>" +
    esc(mqText("Complete", "مكتمل")) + "</th></tr></thead><tbody>";
  let section = "";
  review.items.forEach(function (item) {
    if (item.section !== section) {
      section = item.section;
      html += "<tr><td colspan='5'><b>" + esc(section) + "</b></td></tr>";
    }
    const answer = answerFor(record, item.id);
    html += "<tr class='" + answer.tone + "'><td>" + esc(item.standardCode) + "<br>EOC " + esc(item.eocNo) + "</td><td>" + esc(item.text);
    if (canReview()) {
      html += "<div><button type='button' class='ghost na' data-item='" + item.id + "'>" + esc(answer.na ? mqText("Marked N/A", "مُعلَّم لا ينطبق") : mqText("Mark N/A", "تعليم لا ينطبق")) + "</button></div>";
    }
    html += "</td>";
    ["legible", "accurate", "complete"].forEach(function (key) {
      html += "<td><input type='checkbox' data-item='" + item.id + "' data-key='" + key + "'" + (answer[key] ? " checked" : "") + (answer.na || !canReview() ? " disabled" : "") + "></td>";
    });
    html += "</tr><tr><td colspan='5'><textarea data-comment='" + item.id + "'" + (canReview() ? "" : " readonly") + ">" + esc(answer.comment || "") + "</textarea></td></tr>";
  });
  html += "</tbody></table><h2>" + esc(mqText("Overall", "الإجمالي")) + "</h2><table><thead><tr><th>" +
    esc(mqText("Record", "السجل")) + "</th><th>" + esc(mqText("Identifier", "المعرّف")) + "</th><th>" +
    esc(mqText("Legible", "مقروء")) + "</th><th>" + esc(mqText("Accurate", "دقيق")) + "</th><th>" +
    esc(mqText("Complete", "مكتمل")) + "</th><th>" + esc(mqText("Score", "الدرجة")) + "</th></tr></thead><tbody>";
  review.records.forEach(function (row) {
    html += "<tr><td>" + row.code + "</td><td>" + esc(row.patientIdentifier || "") + "</td><td>" + row.stats.percentLegible +
      "%</td><td>" + row.stats.percentAccurate + "%</td><td>" + row.stats.percentComplete + "%</td><td>" + row.stats.overall + "%</td></tr>";
  });
  html += "</tbody></table>";
  $("#mr-app").html(html);
}

$(document).on("click", ".rec-card", function () { current = $(this).data("code"); render(); });
$(document).on("change", "input[type=checkbox]", function () {
  if (!canReview()) return;
  const patch = {};
  patch[$(this).data("key")] = $(this).is(":checked");
  saveAnswer(Number($(this).data("item")), patch);
});
$(document).on("click", ".na", function () {
  if (!canReview()) return;
  const record = recordByCode(current);
  const answer = answerFor(record, Number($(this).data("item")));
  saveAnswer(answer.itemId, { na: !answer.na });
});
$(document).on("change", "textarea[data-comment]", function () {
  if (!canReview()) return;
  saveAnswer(Number($(this).data("comment")), { comment: $(this).val() });
});
$(document).on("change", "#patient-id", function () {
  if (!canReview()) return;
  api("PUT", "/api/record-reviews/" + reviewId() + "/records/" + current, { patientIdentifier: $(this).val() }).done(function (res) {
    review = res.data;
  });
});
$(document).on("click", "#print-review", function () { window.print(); });
$(document).on("click", "#reset-review", function () {
  if (!canReview()) return;
  if (confirm(mqText("Reset all ten records?", "إعادة ضبط السجلات العشرة؟"))) api("POST", "/api/record-reviews/" + reviewId() + "/reset").done(function (res) {
    review = res.data;
    render();
  });
});
$(document).on("click", "#export-csv", function () {
  const lines = [[
    mqText("Record", "السجل"), mqText("Identifier", "المعرّف"), mqText("Standard", "المعيار"), "EOC",
    mqText("Evidence", "الدليل"), mqText("Legible", "مقروء"), mqText("Accurate", "دقيق"),
    mqText("Complete", "مكتمل"), mqText("N/A", "لا ينطبق"), mqText("Comment", "تعليق")
  ]];
  review.records.forEach(function (record) {
    review.items.forEach(function (item) {
      const answer = answerFor(record, item.id);
      lines.push([record.code, record.patientIdentifier || "", item.standardCode, item.eocNo, item.text,
        answer.legible ? mqText("Yes", "نعم") : mqText("No", "لا"),
        answer.accurate ? mqText("Yes", "نعم") : mqText("No", "لا"),
        answer.complete ? mqText("Yes", "نعم") : mqText("No", "لا"),
        answer.na ? mqText("Yes", "نعم") : mqText("No", "لا"), answer.comment || ""]);
    });
  });
  const csv = lines.map(function (row) { return row.map(function (cell) { return '"' + String(cell).replace(/"/g, '""') + '"'; }).join(","); }).join("\n");
  const blob = new Blob([csv], { type: "text/csv" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "MR_review_checklist_results.csv";
  link.click();
});

$(document).on("mq-ready", function () { if (review) render(); });
document.addEventListener("mq-lang", function () { if (review) render(); });
$(function () { loadReview(); });
