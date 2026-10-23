let checklist = null;
let openStandard = null;
let chapterFilter = "ALL";
let statusFilter = "all";

function auditId() {
  return $("body").data("audit-id");
}

function base() {
  return "/api/audits/" + auditId();
}

function esc(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
    return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch];
  });
}

function counts() {
  let met = 0, partial = 0, not = 0, na = 0, total = 0;
  checklist.chapters.forEach(function (chapter) {
    chapter.standards.forEach(function (standard) {
      standard.elements.forEach(function (element) {
        total++;
        if (element.score === "met") met++;
        else if (element.score === "partial") partial++;
        else if (element.score === "not") not++;
        else if (element.score === "na") na++;
      });
    });
  });
  const denominator = total - na;
  const percent = denominator > 0 ? Math.round(((met + partial * 0.5) / denominator) * 100) : 0;
  return { met: met, partial: partial, notMet: not, na: na, total: total, unscored: total - met - partial - not - na, percent: percent };
}

function paintBar(view) {
  const total = view.total || 1;
  $("#bar-met").css("width", (view.met / total * 100) + "%");
  $("#bar-partial").css("width", (view.partial / total * 100) + "%");
  $("#bar-not").css("width", (view.notMet / total * 100) + "%");
  $("#bar-na").css("width", (view.na / total * 100) + "%");
  $("#score-line").text("Compliance " + view.percent + "% · Met " + view.met + " · Partial " + view.partial +
    " · Not met " + view.notMet + " · N/A " + view.na + " · Unscored " + view.unscored);
}

function findElement(elementId) {
  let found = null;
  checklist.chapters.forEach(function (chapter) {
    chapter.standards.forEach(function (standard) {
      standard.elements.forEach(function (element) {
        if (element.id === elementId) found = element;
      });
    });
  });
  return found;
}

function loadChecklist() {
  api("GET", base() + "/checklist").done(function (res) {
    checklist = res.data;
    render();
  });
}

function saveScore(elementId, value) {
  const element = findElement(elementId);
  const next = element.score === value ? null : value;
  element.score = next;
  paintBar(counts());
  api("PUT", base() + "/scores", { elementId: elementId, value: next }).done(function (res) {
    checklist.compliance = res.data;
    loadChecklist();
  });
}

function toneOf(standard) {
  let met = 0, partial = 0, not = 0, na = 0;
  standard.elements.forEach(function (element) {
    if (element.score === "met") met++;
    else if (element.score === "partial") partial++;
    else if (element.score === "not") not++;
    else if (element.score === "na") na++;
  });
  const total = standard.elements.length;
  if (not > 0) return "not";
  if (partial > 0) return "partial";
  if (met + partial + not + na === 0) return "unscored";
  if (met + na === total) return "met";
  return "mixed";
}

function visible(standard, chapterName) {
  if (chapterFilter !== "ALL" && chapterFilter !== chapterName) return false;
  const text = (standard.code + " " + standard.title + " " + standard.elements.map(function (e) { return e.text; }).join(" ")).toLowerCase();
  const q = ($("#search").val() || "").toLowerCase();
  if (q && text.indexOf(q) < 0) return false;
  const tone = toneOf(standard);
  if (statusFilter === "unscored") return tone === "unscored";
  if (statusFilter === "met") return tone === "met";
  if (statusFilter === "partial") return tone === "partial" || tone === "mixed";
  if (statusFilter === "not") return tone === "not";
  return true;
}

function render() {
  const audit = checklist.audit;
  const view = checklist.compliance || counts();
  let html = "<h1>" + esc(audit.departmentName) + "</h1>";
  html += "<p>" + esc(audit.auditorName) + " · " + audit.auditDate + " · " + audit.status + "</p>";
  html += "<div class='panel no-print'><div class='bar'><span id='bar-met' class='met'></span>";
  html += "<span id='bar-partial' class='partial'></span><span id='bar-not' class='not'></span>";
  html += "<span id='bar-na' class='na'></span></div><p id='score-line'></p>";
  html += "<div class='row chips'><input id='search' placeholder='Search standards'>";
  html += "<button type='button' data-ch='ALL' class='ch'>All</button>";
  checklist.chapters.forEach(function (chapter) {
    html += "<button type='button' class='ch' data-ch='" + esc(chapter.name) + "'>" + esc(chapter.name) + "</button>";
  });
  html += "</div><div class='chips'>";
  ["all", "unscored", "met", "partial", "not"].forEach(function (name) {
    html += "<button type='button' class='st' data-st='" + name + "'>" + name + "</button>";
  });
  html += "<button type='button' id='clear-scores' class='ghost'>Clear scores</button>";
  html += "<button type='button' id='submit-audit'>Submit</button>";
  html += "<button type='button' id='print-audit' class='ghost'>Print</button>";
  html += "<button type='button' id='archive-audit' class='ghost'>Archive</button>";
  html += "<button type='button' id='restore-audit' class='ghost'>Restore</button>";
  html += "<button type='button' id='delete-audit' class='danger'>Delete</button></div></div>";
  checklist.chapters.forEach(function (chapter) {
    chapter.standards.forEach(function (standard) {
      if (!visible(standard, chapter.name)) return;
      const tone = toneOf(standard);
      html += "<article class='gsr-card " + tone + "'><div class='gsr-head' data-open='" + standard.id + "'>";
      html += "<strong>" + esc(standard.code) + "</strong> " + esc(standard.title);
      html += " <span>" + esc(standard.handbookRef) + "</span></div>";
      html += "<div class='gsr-body" + (openStandard === standard.id ? " open" : "") + "'>";
      standard.elements.forEach(function (element) {
        html += "<p><b>" + element.sortOrder + ".</b> " + esc(element.text) + "</p><p>";
        ["met", "partial", "not", "na"].forEach(function (value) {
          const on = element.score === value ? " on" : "";
          html += "<button type='button' class='score-btn " + value + on + "' data-el='" + element.id + "' data-value='" + value + "'>" + value + "</button>";
        });
        html += "</p><textarea data-comment='" + element.id + "' placeholder='Comment'>" + esc(element.comment || "") + "</textarea>";
        if (element.score === "partial" || element.score === "not") {
          html += "<p><button type='button' class='ghost finding' data-code='" + esc(standard.code) + "' data-idx='" + element.sortOrder + "' data-el='" + element.id + "'>Open finding</button></p>";
        }
      });
      html += "<p>Mark all ";
      ["met", "partial", "not", "na"].forEach(function (value) {
        html += "<button type='button' class='bulk' data-std='" + standard.id + "' data-value='" + value + "'>" + value + "</button> ";
      });
      html += "</p></div></article>";
    });
  });
  $("#gsr-app").html(html);
  paintBar(view);
  $("#search").val(window._q || "");
}

$(document).on("click", ".gsr-head", function () {
  const id = Number($(this).data("open"));
  openStandard = openStandard === id ? null : id;
  render();
});

$(document).on("click", ".score-btn", function () {
  saveScore(Number($(this).data("el")), $(this).data("value"));
});

$(document).on("change", "textarea[data-comment]", function () {
  api("PUT", base() + "/comments", { elementId: Number($(this).data("comment")), text: $(this).val() });
});

$(document).on("click", ".bulk", function () {
  api("POST", base() + "/scores/bulk", { standardId: Number($(this).data("std")), value: $(this).data("value") })
    .done(loadChecklist);
});

$(document).on("click", ".ch", function () { chapterFilter = $(this).data("ch"); render(); });
$(document).on("click", ".st", function () { statusFilter = $(this).data("st"); render(); });
$(document).on("input", "#search", function () { window._q = $(this).val(); render(); });
$(document).on("click", "#clear-scores", function () {
  if (confirm("Reset all scores and comments?")) api("DELETE", base() + "/scores").done(loadChecklist);
});
$(document).on("click", "#submit-audit", function () {
  api("POST", base() + "/submit").done(function () { alert("Submitted"); loadChecklist(); })
    .fail(function (xhr) { alert(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "Submit failed"); });
});
$(document).on("click", "#print-audit", function () { window.print(); });
$(document).on("click", "#archive-audit", function () {
  api("POST", base() + "/archive").done(function () { alert("Archived"); loadChecklist(); });
});
$(document).on("click", "#restore-audit", function () {
  api("POST", base() + "/restore").done(function () { alert("Restored"); loadChecklist(); });
});
$(document).on("click", "#delete-audit", function () {
  const mode = window.prompt("Delete mode: pdf, audit, or everything", "audit");
  if (!mode) return;
  const typed = mode === "pdf" ? "" : window.prompt("Type DELETE to confirm");
  if (mode !== "pdf" && typed !== "DELETE") return;
  api("POST", base() + "/remove?mode=" + encodeURIComponent(mode) + "&confirm=" + encodeURIComponent(typed || ""))
    .done(function () { window.location.href = "/audits"; });
});
$(document).on("click", ".finding", function () {
  const element = findElement(Number($(this).data("el")));
  api("POST", "/api/findings", {
    auditId: Number(auditId()),
    gsrCode: $(this).data("code"),
    elementIndex: Number($(this).data("idx")),
    description: element.text,
    riskLevel: element.score === "not" ? "High" : "Medium",
    category: "Practice"
  }).done(function () { alert("Finding saved"); });
});

$(function () { loadChecklist(); });
