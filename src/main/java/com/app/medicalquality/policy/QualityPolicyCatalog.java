package com.app.medicalquality.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * System default for every hospital policy setting. A hospital stores only the keys it changes.
 */
public final class QualityPolicyCatalog {

    public record Group(String id, String labelEn, String labelAr) {}

    public record Setting(
            String key,
            String group,
            String type,
            String defaultJson,
            Double min,
            Double max,
            String shape,
            String labelEn,
            String labelAr,
            String helpEn,
            String helpAr) {}

    public static final List<Group> GROUPS = List.of(
            new Group("assessors", "Assessor assignment", "تعيين المقيّمين"),
            new Group("planning", "Audit planning", "تخطيط التدقيق"),
            new Group("sampling", "Sampling", "أخذ العينات"),
            new Group("scoring", "Scoring", "التقييم"),
            new Group("findings", "Findings", "الملاحظات"),
            new Group("capa", "Corrective action", "الإجراء التصحيحي"),
            new Group("workflow", "Workflow", "سير العمل"),
            new Group("committees", "Roles and committees", "الأدوار واللجان"),
            new Group("standards", "Standards", "المعايير"),
            new Group("indicators", "Indicators", "المؤشرات"),
            new Group("incidents", "Incidents and risk", "الحوادث والمخاطر"),
            new Group("documents", "Documents", "الوثائق"),
            new Group("notifications", "Notifications", "التنبيهات"),
            new Group("security", "Security", "الأمان"));

    private static final String FINDING_LEVELS = """
            [{"code":"critical","labelEn":"Critical","labelAr":"حرج","definitionEn":"Immediate threat to patient safety","definitionAr":"تهديد مباشر لسلامة المريض","escalate":true,"evidenceRequired":true},{"code":"major","labelEn":"Major","labelAr":"جسيم","definitionEn":"Serious failure that needs a corrective action","definitionAr":"إخفاق جسيم يحتاج إجراءً تصحيحياً","escalate":false,"evidenceRequired":false},{"code":"minor","labelEn":"Minor","labelAr":"بسيط","definitionEn":"Limited failure with low direct risk","definitionAr":"إخفاق محدود بخطر مباشر منخفض","escalate":false,"evidenceRequired":false},{"code":"observation","labelEn":"Observation","labelAr":"ملاحظة","definitionEn":"Opportunity to improve, not a failure","definitionAr":"فرصة للتحسين وليست إخفاقاً","escalate":false,"evidenceRequired":false}]
            """.strip();

    private static final List<Setting> SETTINGS = List.of(
            integer("assessors.minPerAudit", "assessors", 1, 0, 20,
                    "Minimum assessors per audit", "الحد الأدنى للمقيّمين في التدقيق",
                    "Smallest team allowed on one audit.", "أصغر فريق مسموح به في تدقيق واحد."),
            integer("assessors.maxPerAudit", "assessors", 5, 1, 20,
                    "Maximum assessors per audit", "الحد الأقصى للمقيّمين في التدقيق",
                    "Largest team allowed on one audit.", "أكبر فريق مسموح به في تدقيق واحد."),
            integer("assessors.minPerDepartment", "assessors", 1, 0, 20,
                    "Minimum assessors per department", "الحد الأدنى للمقيّمين للقسم",
                    "Lower bound used when a department has no own number.", "الحد الأدنى عندما لا يوجد رقم خاص بالقسم."),
            integer("assessors.maxPerDepartmentDefault", "assessors", 5, 1, 20,
                    "Maximum assessors per department", "الحد الأقصى للمقيّمين للقسم",
                    "Applies to every department that is not named below.", "ينطبق على كل قسم غير مذكور أدناه."),
            json("assessors.maxPerDepartmentByName", "assessors", "{}", "object",
                    "Maximum by department", "الحد الأقصى حسب القسم",
                    "Choose a department and its own maximum. Other departments keep the maximum above.",
                    "اختر القسم وحدّه الخاص. باقي الأقسام تبقى على الحد أعلاه."),
            integer("assessors.maxConcurrent", "assessors", 3, 1, 20,
                    "Maximum concurrent audits per assessor", "الحد الأقصى للتدقيقات المتزامنة",
                    "Open audits one assessor may sit on at once.", "عدد التدقيقات المفتوحة التي يمكن للمقيّم المشاركة فيها معاً."),
            bool("assessors.leadRequired", "assessors", true,
                    "Lead assessor required", "رئيس الفريق مطلوب",
                    "Applied on submit only when team enforcement is on.", "يُطبّق عند الإرسال عندما يكون إلزام الفريق مفعّلاً."),
            integer("assessors.minTeamHighRisk", "assessors", 2, 1, 20,
                    "Minimum team for high-risk areas", "الحد الأدنى للفريق في المناطق عالية الخطورة",
                    "Stored until departments carry a risk tier.", "يُحفظ إلى أن تحمل الأقسام مستوى خطورة."),
            bool("assessors.allowOwnDepartment", "assessors", false,
                    "Allow audit of own department", "السماح بتدقيق القسم نفسه",
                    "When off, the department's own assessor cannot join that department's audit.",
                    "عند الإيقاف لا ينضم مقيّم القسم إلى تدقيق قسمه."),
            bool("assessors.coiOwnDepartment", "assessors", true,
                    "Own-department conflict", "تعارض قسم المقيّم",
                    "Combined with the declaration rule below.", "يُقرأ مع قاعدة الإفصاح أدناه."),
            bool("assessors.coiManager", "assessors", true,
                    "Manager or subordinate conflict", "تعارض الرئيس أو المرؤوس",
                    "Stored. The directory has no reporting line yet, so this does not block anyone.",
                    "محفوظ. لا يوجد خط إداري في الدليل بعد، لذلك لا يمنع أحداً."),
            bool("assessors.coiRecentConsulting", "assessors", true,
                    "Recent consulting conflict", "تعارض الاستشارة الحديثة",
                    "Stored. Consulting history is not recorded yet.", "محفوظ. سجل الاستشارات غير موجود بعد."),
            bool("assessors.coiDeclarationRequired", "assessors", true,
                    "Conflict declaration required", "إفصاح التعارض مطلوب",
                    "Assignment is refused until the assessor declares conflicts.", "يُرفض التعيين إلى أن يفصح المقيّم عن التعارض."),
            integer("assessors.maxConsecutiveDepartment", "assessors", 3, 1, 20,
                    "Max consecutive audits of one department", "الحد الأقصى للتدقيقات المتتالية للقسم",
                    "After this many audits of the same department in a row, the assessor must rotate.",
                    "بعد هذا العدد المتتالي لنفس القسم يجب تدوير المقيّم."),
            integer("assessors.coolingOffDays", "assessors", 180, 0, 3650,
                    "Cooling-off days", "أيام الابتعاد",
                    "Days before the same assessor may audit that department again. Zero disables the wait.",
                    "الأيام قبل أن يدقق المقيّم القسم نفسه مرة أخرى. الصفر يلغي الانتظار."),
            bool("assessors.requireCertification", "assessors", false,
                    "Certification required", "الشهادة مطلوبة",
                    "Stored. Assessor certificates are not recorded yet.", "محفوظ. شهادات المقيّمين غير مسجلة بعد."),
            integer("assessors.certificateValidityDays", "assessors", 365, 1, 3650,
                    "Certificate validity days", "صلاحية الشهادة بالأيام",
                    "Used when certificates exist.", "تُستخدم عند وجود الشهادات."),
            integer("assessors.minExperienceYears", "assessors", 1, 0, 50,
                    "Minimum experience years", "الحد الأدنى لسنوات الخبرة",
                    "Stored until experience is recorded on the assessor.", "محفوظ إلى أن تُسجل الخبرة على المقيّم."),
            integer("assessors.refresherMonths", "assessors", 12, 1, 120,
                    "Refresher frequency months", "تكرار التجديد بالأشهر",
                    "Stored until training records exist.", "محفوظ إلى أن توجد سجلات التدريب."),
            bool("assessors.allowExternal", "assessors", false,
                    "Allow external assessors", "السماح بمقيّمين خارجيين",
                    "An assessor marked external can join only when this is on.", "المقيّم المعلم كخارجي ينضم فقط عند التفعيل."),
            bool("assessors.allowCrossHospital", "assessors", false,
                    "Allow cross-hospital assessors", "السماح بمقيّمين من مستشفى آخر",
                    "A platform admin may assign an assessor from another hospital only when this is on.",
                    "يمكن لمدير المنصة تعيين مقيّم من مستشفى آخر فقط عند التفعيل."),
            bool("assessors.enforceTeamOnSubmit", "assessors", false,
                    "Enforce team size on submit", "فرض حجم الفريق عند الإرسال",
                    "Off by default because audits created before teams have no members.",
                    "متوقف افتراضياً لأن التدقيقات الأقدم بلا أعضاء فريق."),
            integer("planning.frequencyMonthsDefault", "planning", 12, 1, 60,
                    "Default audit frequency months", "تكرار التدقيق بالأشهر",
                    "How often a department is audited when frequency enforcement is on.",
                    "كل كم شهر يُدقق القسم عندما يكون فرض التكرار مفعّلاً."),
            json("planning.frequencyByRisk", "planning", "{\"high\":6,\"medium\":12,\"low\":24}", "object",
                    "Frequency by risk", "التكرار حسب الخطورة",
                    "Months for high, medium, and low. Departments do not have a risk tier yet.",
                    "الأشهر للمستويات العالي والمتوسط والمنخفض. الأقسام بلا مستوى خطورة بعد."),
            json("planning.frequencyByDepartment", "planning", "{}", "object",
                    "Frequency by department", "التكرار حسب القسم",
                    "Choose a department and the months between its audits.", "اختر القسم والأشهر بين تدقيقاته."),
            bool("planning.enforceFrequency", "planning", false,
                    "Enforce frequency", "فرض التكرار",
                    "When on, a department cannot be audited again before its frequency has elapsed.",
                    "عند التفعيل لا يُدقق القسم قبل انقضاء مدة التكرار."),
            integer("planning.maxAuditsPerMonth", "planning", 100, 1, 500,
                    "Maximum audits per month", "الحد الأقصى للتدقيقات في الشهر",
                    "Counted for the hospital when an audit is created.", "يُحسب للمستشفى عند إنشاء التدقيق."),
            json("planning.blackoutDates", "planning", "[]", "array",
                    "Blackout dates", "تواريخ الإيقاف",
                    "Dates when no audit may be scheduled. Add each date.",
                    "تواريخ لا يُجدول فيها تدقيق. أضف كل تاريخ."),
            integer("planning.requiredCoveragePercent", "planning", 100, 0, 100,
                    "Required coverage percent", "نسبة التغطية المطلوبة",
                    "Target share of standards or departments in a cycle. Warns when a selected framework expects more.",
                    "النسبة المستهدفة من المعايير أو الأقسام في الدورة. يظهر تنبيه إذا تطلب الإطار أكثر."),
            bool("planning.allowAnnounced", "planning", true,
                    "Announced audits allowed", "التدقيق المعلن مسموح",
                    "Stored until audits record announced or unannounced.", "محفوظ إلى أن يسجل التدقيق كونه معلناً أو مفاجئاً."),
            bool("planning.allowUnannounced", "planning", true,
                    "Unannounced audits allowed", "التدقيق المفاجئ مسموح",
                    "Stored until audits record announced or unannounced.", "محفوظ إلى أن يسجل التدقيق كونه معلناً أو مفاجئاً."),
            bool("planning.patientTracer", "planning", false,
                    "Patient tracer", "تتبع المريض",
                    "Enables patient-tracer methodology for this hospital.", "يفعّل منهجية تتبع المريض لهذا المستشفى."),
            bool("planning.systemTracer", "planning", false,
                    "System tracer", "تتبع النظام",
                    "Enables system-tracer methodology for this hospital.", "يفعّل منهجية تتبع النظام لهذا المستشفى."),
            text("sampling.mode", "sampling", "fixed",
                    "Sample size mode", "طريقة حجم العينة",
                    "fixed, percent, or confidence.", "fixed أو percent أو confidence."),
            integer("sampling.fixedSize", "sampling", 10, 1, 500,
                    "Fixed sample size", "حجم العينة الثابت",
                    "Used when the mode is fixed.", "يُستخدم عندما تكون الطريقة ثابتة."),
            integer("sampling.percent", "sampling", 10, 1, 100,
                    "Sample percent", "نسبة العينة",
                    "Used when the mode is percent.", "تُستخدم عندما تكون الطريقة نسبة."),
            integer("sampling.confidencePercent", "sampling", 95, 50, 99,
                    "Confidence level", "مستوى الثقة",
                    "Used when the mode is confidence.", "يُستخدم عندما تكون الطريقة مستوى ثقة."),
            integer("sampling.minRecords", "sampling", 5, 0, 500,
                    "Minimum records", "الحد الأدنى للسجلات",
                    "Minimum records to review for a standard.", "أقل عدد سجلات لمراجعة معيار."),
            integer("sampling.minObservations", "sampling", 3, 0, 100,
                    "Minimum observations", "الحد الأدنى للمشاهدات",
                    "Minimum observations per standard.", "أقل عدد مشاهدات لكل معيار."),
            integer("sampling.minInterviews", "sampling", 2, 0, 100,
                    "Minimum interviews", "الحد الأدنى للمقابلات",
                    "Minimum interviews per standard.", "أقل عدد مقابلات لكل معيار."),
            integer("sampling.lookBackDays", "sampling", 90, 1, 3650,
                    "Look-back days", "نافذة الرجوع بالأيام",
                    "How far back records are reviewed.", "إلى أي مدى تُراجع السجلات."),
            text("scoring.scale", "scoring", "met_partial_not_na",
                    "Scoring scale", "مقياس التقييم",
                    "met_partial_not_na matches the checklist. percent is stored for a later score type.",
                    "met_partial_not_na يطابق قائمة التحقق. percent محفوظ لنوع تقييم لاحق."),
            decimal("scoring.partialWeight", "scoring", 0.5, 0, 1,
                    "Partial score weight", "وزن التقدير الجزئي",
                    "Weight of Partially Met in the compliance percent. The default 0.5 matches today's formula.",
                    "وزن التقدير الجزئي في نسبة الامتثال. القيمة 0.5 تطابق المعادلة الحالية."),
            integer("scoring.minPassPercent", "scoring", 80, 0, 100,
                    "Minimum percent to pass", "أقل نسبة للنجاح",
                    "Checked on submit when pass enforcement is on.", "تُفحص عند الإرسال عندما يكون فرض النجاح مفعّلاً."),
            integer("scoring.capaTriggerPercent", "scoring", 90, 0, 100,
                    "Percent that requires a CAPA", "النسبة التي توجب إجراءً تصحيحياً",
                    "Checked on submit when CAPA-threshold enforcement is on.", "تُفحص عند الإرسال عندما يكون فرض عتبة الإجراء مفعّلاً."),
            bool("scoring.enforcePassThreshold", "scoring", false,
                    "Block submit below the pass percent", "منع الإرسال تحت نسبة النجاح",
                    "Off until a hospital chooses to block a low score.", "متوقف إلى أن يختار المستشفى منع الدرجة المنخفضة."),
            bool("scoring.enforceCapaThreshold", "scoring", false,
                    "Require a finding below the CAPA percent", "اشتراط ملاحظة تحت نسبة الإجراء",
                    "Off until a hospital turns it on.", "متوقف إلى أن يفعّله المستشفى."),
            json("scoring.mustPassCodes", "scoring", "[]", "array",
                    "Must-pass standard codes", "رموز المعايير الحاسمة",
                    "GSR codes that must be Met before submit.", "رموز يجب أن تكون مستوفاة قبل الإرسال."),
            bool("scoring.naRequiresApproval", "scoring", false,
                    "N/A needs approval", "غير منطبق يحتاج اعتماداً",
                    "When on, only the roles below can set Not applicable.", "عند التفعيل فقط الأدوار أدناه تحدد غير منطبق."),
            json("scoring.naApproverRoleCodes", "scoring", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Roles that may approve N/A", "الأدوار التي تعتمد غير منطبق",
                    "Role codes allowed to mark an element Not applicable.", "رموز الأدوار المسموح لها بتعليم العنصر غير منطبق."),
            json("findings.levels", "findings", FINDING_LEVELS, "array",
                    "Severity levels", "مستويات الشدة",
                    "Each level needs code, escalate, and evidenceRequired.", "كل مستوى يحتاج رمزاً وescalate وevidenceRequired."),
            json("findings.legacyAliases", "findings", "{\"High\":\"major\",\"Medium\":\"minor\",\"Low\":\"observation\"}", "object",
                    "Legacy severity names", "أسماء الشدة القديمة",
                    "Maps the current High, Medium, and Low labels onto the levels above.",
                    "يربط تسميات High وMedium وLow الحالية بالمستويات أعلاه."),
            json("capa.deadlineBySeverity", "capa", "{}", "object",
                    "Extra deadlines by severity", "مُهل إضافية حسب الشدة",
                    "Overrides the four deadlines below for a matching code.", "تستبدل المُهل الأربع أدناه للرمز المطابق."),
            integer("capa.deadline.critical", "capa", 7, 1, 365,
                    "Critical deadline days", "مهلة الحرج بالأيام",
                    "Calendar days unless working-day deadlines are on.", "أيام تقويمية ما لم تُفعّل أيام العمل."),
            integer("capa.deadline.major", "capa", 30, 1, 365,
                    "Major deadline days", "مهلة الجسيم بالأيام",
                    "Used for major findings, including the legacy High label.", "تُستخدم للملاحظات الجسيمة، ومنها التسمية High."),
            integer("capa.deadline.minor", "capa", 60, 1, 365,
                    "Minor deadline days", "مهلة البسيط بالأيام",
                    "Used for minor findings, including the legacy Medium label.", "تُستخدم للملاحظات البسيطة، ومنها التسمية Medium."),
            integer("capa.deadline.observation", "capa", 90, 1, 365,
                    "Observation deadline days", "مهلة الملاحظة بالأيام",
                    "Used for observations and the legacy Low label.", "تُستخدم للملاحظات والتسمية Low."),
            json("capa.rootCauseSeverities", "capa", "[\"critical\"]", "array",
                    "Severities that need a root cause", "الشدّة التي توجب سبباً جذرياً",
                    "A CAPA for these severities must include a root cause.", "الإجراء لهذه الشدّة يجب أن يتضمن سبباً جذرياً."),
            json("capa.assigneeRoleCodes", "capa", "[]", "array",
                    "Roles that may own an action", "الأدوار التي تملك الإجراء",
                    "Empty means any user who can manage CAPA. The directory stores a name, not a user, on the action.",
                    "الفارغ يعني أي مستخدم يدير الإجراءات. السجل يحفظ اسماً لا مستخدماً."),
            bool("capa.closeOnlyAfterVerification", "capa", false,
                    "Close the finding only after verification", "إغلاق الملاحظة بعد التحقق فقط",
                    "When on, a CAPA cannot close the finding until someone verifies it.",
                    "عند التفعيل لا يغلق الإجراء الملاحظة قبل التحقق."),
            json("capa.verifierRoleCodes", "capa", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Roles that may verify", "الأدوار التي تتحقق",
                    "Checked when closure requires verification.", "تُفحص عندما يتطلب الإغلاق تحققاً."),
            integer("capa.reauditAfterDays", "capa", 30, 0, 365,
                    "Re-audit after days", "إعادة التدقيق بعد أيام",
                    "Stored for the follow-up audit. Zero means no automatic follow-up.",
                    "محفوظ لتدقيق المتابعة. الصفر يعني عدم جدولة تلقائية."),
            integer("capa.maxExtensions", "capa", 2, 0, 10,
                    "Maximum extensions", "الحد الأقصى للتمديدات",
                    "How many times a target date may move later.", "كم مرة يمكن تأجيل تاريخ الاستحقاق."),
            json("capa.extensionApproverRoleCodes", "capa", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Roles that may extend", "الأدوار التي تمدد",
                    "A later target date is refused for anyone outside this list.", "يُرفض التاريخ الأبعد لمن هو خارج هذه القائمة."),
            bool("capa.extensionReasonRequired", "capa", true,
                    "Extension reason required", "سبب التمديد مطلوب",
                    "A later target date must include a reason.", "التاريخ الأبعد يجب أن يرافقه سبب."),
            json("workflow.approvalChain", "workflow",
                    "{\"auditPlan\":[\"QUALITY_DIRECTOR\"],\"report\":[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"],\"capaClosure\":[\"QUALITY_DIRECTOR\"]}",
                    "object",
                    "Approval chain", "سلسلة الاعتماد",
                    "Role codes for audit plans, reports, and CAPA closure.", "رموز الأدوار لخطط التدقيق والتقارير وإغلاق الإجراءات."),
            bool("workflow.policyApprovalRequired", "workflow", false,
                    "Policy changes need a second approver", "تغيير السياسة يحتاج معتمداً ثانياً",
                    "When on, the author cannot activate the version.", "عند التفعيل لا يستطيع المؤلف تفعيل النسخة."),
            json("workflow.escalation", "workflow",
                    "[{\"afterDays\":0,\"role\":\"DEPARTMENT_COORDINATOR\"},{\"afterDays\":7,\"role\":\"QUALITY_DIRECTOR\"},{\"afterDays\":14,\"role\":\"HOSPITAL_OWNER\"}]",
                    "array",
                    "Escalation ladder", "سلم التصعيد",
                    "Who is notified after a deadline, in days overdue.", "من يُنبّه بعد الاستحقاق، بالأيام المتأخرة."),
            json("workflow.reminders", "workflow", "[7,3,1]", "array",
                    "Reminder days before a deadline", "أيام التذكير قبل الاستحقاق",
                    "Positive numbers. There is no outbound mailer yet; the schedule is what a mailer must use.",
                    "أرقام موجبة. لا يوجد مرسل بريد بعد؛ هذا هو الجدول الذي يجب أن يستخدمه."),
            integer("workflow.auditeeResponseDays", "workflow", 10, 1, 90,
                    "Auditee response window", "مهلة رد الجهة المدققة",
                    "Days to accept or dispute a report.", "أيام قبول التقرير أو الاعتراض عليه."),
            json("committees.composition", "committees",
                    "{\"requiredRoles\":[\"QUALITY_DIRECTOR\"],\"meetingEveryMonths\":1,\"coordinatorRequired\":true}",
                    "object",
                    "Quality committee", "لجنة الجودة",
                    "Composition and how often it meets. Committee meetings are not scheduled in this version.",
                    "التكوين ووتيرة الاجتماع. الاجتماعات لا تُجدول في هذه النسخة."),
            bool("committees.coordinatorRequired", "committees", true,
                    "Department coordinator required", "منسق القسم مطلوب",
                    "Stored beside the department coordinator name.", "محفوظ إلى جانب اسم منسق القسم."),
            json("committees.checklistEditors", "committees", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Who may edit standards", "من يعدّل المعايير",
                    "Role codes allowed to change checklists. Checklist editing is still a platform catalog today.",
                    "رموز الأدوار المسموح لها بتعديل القوائم. تعديل القوائم ما زال على مستوى المنصة."),
            json("standards.frameworks", "standards", "[]", "array",
                    "Active frameworks", "الأطر المعتمدة",
                    "GAHAR, JCI, ISO_9001, ISO_15189, or OTHER. Known floors warn, they do not silently change values.",
                    "GAHAR أو JCI أو ISO_9001 أو ISO_15189 أو OTHER. الحدود المعروفة تنبّه ولا تغيّر القيمة بصمت."),
            text("standards.edition", "standards", "",
                    "Edition or version", "الإصدار",
                    "Free text, such as GAHAR 2025.", "نص حر، مثل GAHAR 2025."),
            bool("standards.customItemsRequireApproval", "standards", true,
                    "Checklist changes need approval", "تعديل القائمة يحتاج اعتماداً",
                    "Stored for hospital-specific checklist items.", "محفوظ لبنود القائمة الخاصة بالمستشفى."),
            json("standards.applicability", "standards", "{}", "object",
                    "Applicability map", "خريطة الانطباق",
                    "Standard code to a list of department codes. Not used to hide checklist rows yet.",
                    "رمز المعيار ثم قائمة رموز الأقسام. لا يُخفي صفوف القائمة بعد."),
            json("indicators.items", "indicators", "[]", "array",
                    "KPI list", "قائمة المؤشرات",
                    "Each item: code, name, target, warnBelow, department, frequency, owner. No KPI module records values yet.",
                    "كل بند: code وname وtarget وwarnBelow وdepartment وfrequency وowner. لا توجد قيم مؤشرات بعد."),
            json("incidents.classes", "incidents",
                    "[{\"code\":\"sentinel\",\"investigateDays\":1},{\"code\":\"major\",\"investigateDays\":7},{\"code\":\"minor\",\"investigateDays\":30}]",
                    "array",
                    "Incident classes", "تصنيف الحوادث",
                    "Investigation deadlines by class. Incident reporting is not a module yet.",
                    "مُهل التحقيق حسب التصنيف. الإبلاغ عن الحوادث ليس وحدة بعد."),
            json("incidents.riskMatrix", "incidents", "{\"likelihood\":5,\"impact\":5,\"reviewMonths\":12}", "object",
                    "Risk matrix", "مصفوفة المخاطر",
                    "Likelihood and impact scales, and how often the register is reviewed.",
                    "مقياسا الاحتمال والأثر، ووتيرة مراجعة السجل."),
            integer("documents.reviewMonths", "documents", 24, 1, 120,
                    "Policy review cycle months", "دورة مراجعة السياسة بالأشهر",
                    "How often a controlled document should be reviewed.", "كل كم شهر تُراجع الوثيقة."),
            integer("documents.auditRetentionYears", "documents", 5, 1, 30,
                    "Audit record retention years", "حفظ سجلات التدقيق بالسنوات",
                    "Retention for audits, findings, and CAPA.", "مدة حفظ التدقيق والملاحظات والإجراءات."),
            integer("documents.evidenceRetentionYears", "documents", 5, 1, 30,
                    "Evidence retention years", "حفظ الأدلة بالسنوات",
                    "Retention for uploaded evidence.", "مدة حفظ الأدلة المرفوعة."),
            json("documents.approvalLevels", "documents", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Document approval levels", "مستويات اعتماد الوثائق",
                    "Role codes, in order.", "رموز الأدوار بالترتيب."),
            json("notifications.channels", "notifications",
                    "{\"inApp\":true,\"email\":false,\"sms\":false,\"whatsapp\":false}",
                    "object",
                    "Channels", "قنوات التنبيه",
                    "In-app is the only channel that exists today. The others are preferences for a later sender.",
                    "داخل التطبيق هي القناة الموجودة اليوم. الباقي تفضيلات لمرسل لاحق."),
            json("notifications.recipients", "notifications", "{}", "object",
                    "Recipients by event", "المستلمون حسب الحدث",
                    "Event name to a list of role codes.", "اسم الحدث ثم قائمة رموز الأدوار."),
            json("notifications.reports", "notifications", "[]", "array",
                    "Required reports", "التقارير المطلوبة",
                    "Each item: code, frequency. Report generation is unchanged.", "كل بند: code وfrequency. إصدار التقارير كما هو."),
            text("notifications.language", "notifications", "en",
                    "Language", "اللغة",
                    "en or ar. The policy screen can show either.", "en أو ar. شاشة السياسة تعرض أياً منهما."),
            text("notifications.calendar", "notifications", "gregorian",
                    "Calendar", "التقويم",
                    "gregorian or hijri. Deadline arithmetic stays Gregorian and uses the working week below.",
                    "gregorian أو hijri. حساب المُهل يبقى ميلادياً ويستخدم أسبوع العمل أدناه."),
            json("notifications.workingDays", "notifications", "[\"SUN\",\"MON\",\"TUE\",\"WED\",\"THU\"]", "array",
                    "Working days", "أيام العمل",
                    "Used when deadlines count working days. SUN through SAT.", "تُستخدم عندما تُحسب المُهل بأيام العمل."),
            json("notifications.holidays", "notifications", "[]", "array",
                    "Holidays", "العطل",
                    "ISO dates skipped when deadlines count working days.", "تواريخ ISO تُستبعد عندما تُحسب المُهل بأيام العمل."),
            bool("notifications.deadlineUsesWorkingDays", "notifications", false,
                    "Count CAPA deadlines in working days", "حساب مُهل الإجراءات بأيام العمل",
                    "Off uses calendar days, which matches dates already entered by hand.",
                    "الإيقاف يستخدم الأيام التقويمية، بما يطابق التواريخ المدخلة يدوياً."),
            json("security.confidentialityLevels", "security", "[\"internal\",\"restricted\"]", "array",
                    "Confidentiality levels", "مستويات السرية",
                    "Labels for evidence and reports.", "تسميات للأدلة والتقارير."),
            json("security.reportViewerRoleCodes", "security", "[\"QUALITY_DIRECTOR\",\"HOSPITAL_OWNER\"]", "array",
                    "Who may view reports", "من يرى التقارير",
                    "Role codes. Report screens still follow their existing permissions.",
                    "رموز الأدوار. شاشات التقارير ما زالت تتبع صلاحياتها الحالية."),
            bool("security.lockClosedAudits", "security", true,
                    "Lock a closed audit", "قفل التدقيق المغلق",
                    "A closed audit becomes read-only and cannot be reopened.", "التدقيق المغلق يصبح للقراءة فقط ولا يُعاد فتحه."));

    private static final Map<String, Setting> BY_KEY = index();
    private static final Map<String, Group> GROUPS_BY_ID = groups();

    private QualityPolicyCatalog() {}

    public static List<Setting> settings() {
        return SETTINGS;
    }

    public static Setting setting(String a_key) {
        return BY_KEY.get(a_key);
    }

    public static Group group(String a_id) {
        return GROUPS_BY_ID.get(a_id);
    }

    public static ObjectNode defaults(ObjectMapper a_mapper) {
        ObjectNode node = a_mapper.createObjectNode();
        for (Setting setting : SETTINGS) {
            try {
                node.set(setting.key(), a_mapper.readTree(setting.defaultJson()));
            } catch (Exception ex) {
                throw new IllegalStateException("Default for " + setting.key() + " is not valid JSON", ex);
            }
        }
        return node;
    }

    public static JsonNode defaultValue(ObjectMapper a_mapper, String a_key) {
        Setting setting = BY_KEY.get(a_key);
        if (setting == null) {
            return null;
        }
        try {
            return a_mapper.readTree(setting.defaultJson());
        } catch (Exception ex) {
            throw new IllegalStateException("Default for " + a_key + " is not valid JSON", ex);
        }
    }

    private static Map<String, Setting> index() {
        Map<String, Setting> map = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) {
            map.put(setting.key(), setting);
        }
        return Map.copyOf(map);
    }

    private static Map<String, Group> groups() {
        Map<String, Group> map = new LinkedHashMap<>();
        for (Group group : GROUPS) {
            map.put(group.id(), group);
        }
        return Map.copyOf(map);
    }

    private static Setting integer(
            String a_key, String a_group, int a_value, int a_min, int a_max,
            String a_en, String a_ar, String a_helpEn, String a_helpAr) {
        return new Setting(a_key, a_group, "int", Integer.toString(a_value), (double) a_min, (double) a_max, null,
                a_en, a_ar, a_helpEn, a_helpAr);
    }

    private static Setting bool(
            String a_key, String a_group, boolean a_value,
            String a_en, String a_ar, String a_helpEn, String a_helpAr) {
        return new Setting(a_key, a_group, "bool", a_value ? "true" : "false", null, null, null,
                a_en, a_ar, a_helpEn, a_helpAr);
    }

    private static Setting decimal(
            String a_key, String a_group, double a_value, double a_min, double a_max,
            String a_en, String a_ar, String a_helpEn, String a_helpAr) {
        return new Setting(a_key, a_group, "decimal", Double.toString(a_value), a_min, a_max, null,
                a_en, a_ar, a_helpEn, a_helpAr);
    }

    private static Setting text(
            String a_key, String a_group, String a_value,
            String a_en, String a_ar, String a_helpEn, String a_helpAr) {
        return new Setting(a_key, a_group, "text", "\"" + a_value + "\"", null, null, null,
                a_en, a_ar, a_helpEn, a_helpAr);
    }

    private static Setting json(
            String a_key, String a_group, String a_json, String a_shape,
            String a_en, String a_ar, String a_helpEn, String a_helpAr) {
        return new Setting(a_key, a_group, "json", a_json, null, null, a_shape,
                a_en, a_ar, a_helpEn, a_helpAr);
    }
}
