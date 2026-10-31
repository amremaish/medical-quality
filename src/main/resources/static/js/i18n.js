(function (window, document) {
  var KEY = "mq_lang";

  function storedChoice() {
    try {
      var lang = localStorage.getItem(KEY) || sessionStorage.getItem(KEY);
      return lang === "ar" || lang === "en" ? lang : "";
    } catch (e) {
      return "";
    }
  }

  function current() {
    return storedChoice() || "en";
  }

  function persist(lang) {
    try {
      localStorage.setItem(KEY, lang);
      sessionStorage.setItem(KEY, lang);
    } catch (e) {}
  }

  function applyDir() {
    var lang = current();
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
  }

  applyDir();

  var STRINGS = {
    "nav.skip": ["Skip to content", "تخطي إلى المحتوى"],
    "nav.platform": ["Quality platform", "منصة الجودة"],
    "nav.organizations": ["Organizations", "المؤسسات"],
    "nav.owners": ["Owners", "الملاك"],
    "nav.home": ["Home", "الرئيسية"],
    "nav.dashboards": ["Dashboards", "لوحات المعلومات"],
    "nav.command": ["Command center", "مركز القيادة"],
    "nav.executive": ["Executive", "التنفيذي"],
    "nav.assessor": ["Assessor", "المقيّم"],
    "nav.director": ["Quality director", "مدير الجودة"],
    "nav.work": ["Work", "العمل"],
    "nav.audits": ["GSR audits", "تدقيقات المعايير"],
    "nav.records": ["Medical records", "السجلات الطبية"],
    "nav.findings": ["Findings", "الملاحظات"],
    "nav.capa": ["CAPA", "الإجراءات التصحيحية"],
    "nav.capaTip": ["Corrective and preventive action", "إجراء تصحيحي ووقائي"],
    "nav.evidence": ["Evidence", "الأدلة"],
    "nav.organization": ["Organization", "المؤسسة"],
    "nav.departments": ["Departments", "الأقسام"],
    "nav.assessors": ["Assessors", "المقيّمون"],
    "nav.policy": ["Quality policy", "سياسة الجودة"],
    "nav.users": ["Users", "المستخدمون"],
    "nav.roles": ["Roles", "الأدوار"],
    "nav.tools": ["Tools", "الأدوات"],
    "nav.import": ["PDF import", "استيراد PDF"],
    "nav.signedIn": ["Signed in", "تم تسجيل الدخول"],
    "nav.quality": ["Quality", "الجودة"],
    "nav.logout": ["Log out", "تسجيل الخروج"],
    "nav.close": ["Close menu", "إغلاق القائمة"],
    "nav.viewing": ["Viewing", "عرض"],
    "nav.asAdmin": ["as platform admin", "بصلاحية مدير المنصة"],
    "nav.return": ["Return to platform", "العودة إلى المنصة"],
    "nav.menu": ["Menu", "القائمة"],
    "nav.search": ["Search this page", "ابحث في هذه الصفحة"],
    "nav.findingsTip": ["Findings needing attention", "ملاحظات تحتاج إلى متابعة"],
    "nav.capaAttention": ["CAPA needing attention", "إجراءات تحتاج إلى متابعة"],
    "nav.newAudit": ["New audit", "تدقيق جديد"],
    "nav.workspace": ["Quality workspace", "مساحة الجودة"],
    "nav.hospitalsLine": ["Quality platform for hospitals", "منصة جودة للمستشفيات"],

    "login.title": ["Sign in", "تسجيل الدخول"],
    "login.brand": ["Medical quality", "الجودة الطبية"],
    "login.lead": ["One platform for every hospital. Standards, records, findings, and follow-up in the same workspace.", "منصة واحدة لكل مستشفى. المعايير والسجلات والملاحظات والمتابعة في مساحة العمل نفسها."],
    "login.hint": ["Use your email and the password from your invitation.", "استخدم بريدك وكلمة المرور من الدعوة."],
    "login.user": ["Email or username", "البريد أو اسم المستخدم"],
    "login.password": ["Password", "كلمة المرور"],
    "login.submit": ["Sign in", "دخول"],
    "login.failed": ["Login failed", "تعذر تسجيل الدخول"],

    "setup.title": ["Choose a password", "اختيار كلمة المرور"],
    "setup.lead": ["Choose a password for your hospital account. This link works once.", "اختر كلمة مرور لحساب المستشفى. هذا الرابط يعمل مرة واحدة."],
    "setup.new": ["New password", "كلمة المرور الجديدة"],
    "setup.save": ["Save password", "حفظ كلمة المرور"],
    "setup.invalid": ["This invitation link is invalid or has already been used", "رابط الدعوة غير صالح أو استُخدم من قبل"],
    "setup.fail": ["Could not save the password", "تعذر حفظ كلمة المرور"],
    "password.lead": ["Your invitation used a temporary password. Set one only you know before continuing.", "الدعوة استخدمت كلمة مرور مؤقتة. عيّن كلمة لا يعرفها غيرك قبل المتابعة."],
    "password.current": ["Current password", "كلمة المرور الحالية"],
    "password.fail": ["Could not update the password", "تعذر تحديث كلمة المرور"],

    "home.welcome": ["Welcome", "أهلاً"],
    "home.morning": ["Good morning", "صباح الخير"],
    "home.afternoon": ["Good afternoon", "مساء الخير"],
    "home.evening": ["Good evening", "مساء الخير"],
    "home.auditsHint": ["Audits in this hospital", "تدقيقات هذا المستشفى"],
    "home.recordsHint": ["Reviews in this hospital", "مراجعات هذا المستشفى"],
    "home.complianceHint": ["Compliance for this hospital", "الالتزام في هذا المستشفى"],

    "page.audits": ["GSR audits", "تدقيقات المعايير"],
    "page.auditsLead": ["Standards scored with half credit. Open an audit to mark elements.", "تُقيَّم المعايير بنصف درجة عند الاستيفاء الجزئي. افتح التدقيق لتعليم العناصر."],
    "page.auditNew": ["New audit", "تدقيق جديد"],
    "page.auditNewLead": ["Name the round and the person scoring it. Link a department when it already exists.", "سمِّ الجولة والشخص الذي يقيّمها. اربط قسماً إذا كان موجوداً."],
    "page.checklist": ["GSR checklist", "قائمة المعايير"],
    "page.records": ["Medical records", "السجلات الطبية"],
    "page.recordsTitle": ["Medical record reviews", "مراجعات السجلات الطبية"],
    "page.recordsLead": ["Legible, accurate, and complete. A record counts only when all three are met.", "مقروء ودقيق ومكتمل. يُحتسب السجل فقط عند تحقق الثلاثة."],
    "page.reviewNew": ["New review", "مراجعة جديدة"],
    "page.reviewNewTitle": ["New record review", "مراجعة سجل جديدة"],
    "page.reviewLead": ["Start a review for one hospital, reviewer, and ward.", "ابدأ مراجعة لمستشفى ومراجع وقسم."],
    "page.review": ["Record review", "مراجعة السجل"],
    "page.findings": ["Findings", "الملاحظات"],
    "page.findingsLead": ["Gaps recorded against a standard, with risk and status.", "فجوات مسجلة على معيار، مع الخطورة والحالة."],
    "page.capa": ["CAPA", "الإجراءات التصحيحية"],
    "page.capaLead": ["Corrective actions tied to an open finding.", "إجراءات تصحيحية مرتبطة بملاحظة مفتوحة."],
    "page.capaNew": ["New CAPA", "إجراء تصحيحي جديد"],
    "page.capaNewLead": ["Attach a corrective action to a finding.", "اربط إجراءً تصحيحياً بملاحظة."],
    "page.evidence": ["Evidence", "الأدلة"],
    "page.evidenceLead": ["Files collected for an audit.", "ملفات جُمعت لتدقيق."],
    "page.upload": ["Upload", "رفع"],
    "page.uploadTitle": ["Upload evidence", "رفع دليل"],
    "page.uploadLead": ["Attach a file to an audit in this hospital.", "أرفق ملفاً بتدقيق في هذا المستشفى."],
    "page.departments": ["Departments", "الأقسام"],
    "page.departmentsLead": ["Clinical areas this hospital works with. Each hospital keeps its own list.", "المجالات السريرية التي يعمل معها هذا المستشفى. لكل مستشفى قائمته."],
    "page.departmentNew": ["New department", "قسم جديد"],
    "page.departmentLead": ["Add a clinical area this hospital knows. The platform assigns the code.", "أضف مجالاً سريرياً يعرفه هذا المستشفى. تعيّن المنصة الرمز."],
    "page.departmentEdit": ["Edit department", "تعديل القسم"],
    "page.departmentEditLead": ["Change this clinical area. The code was assigned when the department was created.", "عدّل هذا المجال السريري. الرمز عُيّن عند إنشاء القسم."],
    "page.assessors": ["Assessors", "المقيّمون"],
    "page.assessorsLead": ["People assigned to score departments.", "الأشخاص المعيَّنون لتقييم الأقسام."],
    "page.assessorEdit": ["Edit assessor", "تعديل المقيّم"],
    "page.loading": ["Loading…", "جارٍ التحميل…"],
    "page.orgs": ["Organizations", "المؤسسات"],
    "page.orgsLead": ["Hospitals and other facilities on the platform. Choose a name to work in that hospital.", "المستشفيات والمنشآت على المنصة. اختر اسماً للعمل داخل ذلك المستشفى."],
    "page.orgNew": ["New organization", "مؤسسة جديدة"],
    "page.orgLead": ["Choose a type and invite the first owner. Hospital is available now.", "اختر النوع وادعُ المالك الأول. المستشفى متاح الآن."],
    "page.owners": ["Owners", "الملاك"],
    "page.ownersLead": ["People invited to run an organization. Invited owners still need to choose a password.", "الأشخاص المدعوون لإدارة مؤسسة. الملاك المدعوون ما زالوا بحاجة لاختيار كلمة مرور."],
    "page.ownerNew": ["Invite owner", "دعوة مالك"],
    "page.ownerLead": ["The owner signs in with this email and must replace the temporary password.", "يسجّل المالك الدخول بهذا البريد ويجب أن يستبدل كلمة المرور المؤقتة."],
    "page.users": ["Users", "المستخدمون"],
    "page.usersLead": ["People who can sign in to this hospital.", "الأشخاص الذين يمكنهم الدخول إلى هذا المستشفى."],
    "page.invite": ["Invite user", "دعوة مستخدم"],
    "page.inviteLead": ["The account stays off until they open the setup link and choose a password.", "يبقى الحساب متوقفاً حتى يفتحوا رابط الإعداد ويختاروا كلمة مرور."],
    "page.userEdit": ["Edit user", "تعديل المستخدم"],
    "page.roles": ["Roles", "الأدوار"],
    "page.rolesLead": ["Built-in roles for this hospital, plus roles you add.", "أدوار جاهزة لهذا المستشفى، إضافة إلى الأدوار التي تنشئها."],
    "page.roleNew": ["New role", "دور جديد"],
    "page.roleLead": ["A custom role can receive only the permissions you already hold, and only ones this hospital can assign.", "الدور المخصص يستلم فقط الصلاحيات التي تملكها، والمسموح لهذا المستشفى بمنحها."],
    "page.import": ["PDF import", "استيراد PDF"],
    "page.importLead": ["Read a filled GSR checklist from a text PDF, review the extraction, then create the audit.", "اقرأ قائمة معايير معبأة من ملف PDF نصي، راجع الاستخراج، ثم أنشئ التدقيق."],
    "page.importNew": ["Import PDF", "استيراد PDF"],
    "page.importNewLead": ["Upload a text PDF. Correct the preview, then confirm to create the audit.", "ارفع ملف PDF نصياً. صحّح المعاينة، ثم أكّد لإنشاء التدقيق."],
    "page.policy": ["Quality policy", "سياسة الجودة"],
    "page.dash": ["Dashboard", "لوحة المعلومات"],

    "col.date": ["Date", "التاريخ"],
    "col.department": ["Department", "القسم"],
    "col.auditor": ["Auditor", "المدقّق"],
    "col.status": ["Status", "الحالة"],
    "col.compliance": ["Compliance", "الالتزام"],
    "col.hospital": ["Hospital", "المستشفى"],
    "col.reviewer": ["Reviewer", "المراجع"],
    "col.name": ["Name", "الاسم"],
    "col.type": ["Type", "النوع"],
    "col.owner": ["Owner", "المالك"],
    "col.email": ["Email", "البريد"],
    "col.person": ["Person", "الشخص"],
    "col.otherName": ["Arabic", "العربية"],
    "col.otherNameEn": ["English", "الإنجليزية"],
    "col.code": ["Code", "الرمز"],
    "col.head": ["Head", "رئيس القسم"],
    "col.assessor": ["Assessor", "المقيّم"],
    "col.gsr": ["GSR", "المعيار"],
    "col.risk": ["Risk", "الخطورة"],
    "col.description": ["Description", "الوصف"],
    "col.finding": ["Finding", "الملاحظة"],
    "col.progress": ["Progress", "التقدم"],
    "col.action": ["Action", "الإجراء"],
    "col.file": ["File", "الملف"],
    "col.size": ["Size", "الحجم"],
    "col.audit": ["Audit", "التدقيق"],
    "col.role": ["Role", "الدور"],
    "col.sees": ["Sees", "يرى"],
    "col.permissions": ["Permissions", "الصلاحيات"],
    "col.people": ["People", "الأشخاص"],
    "col.organization": ["Organization", "المؤسسة"],
    "col.item": ["Item", "العنصر"],
    "col.age": ["Age", "المدة"],

    "action.cancel": ["Cancel", "إلغاء"],
    "action.save": ["Save", "حفظ"],
    "action.createAudit": ["Create audit", "إنشاء التدقيق"],
    "action.createDept": ["Create department", "إنشاء القسم"],
    "action.createReview": ["Create review", "إنشاء المراجعة"],
    "action.addCapa": ["Add CAPA", "إضافة إجراء"],
    "action.upload": ["Upload", "رفع"],
    "action.edit": ["Edit", "تعديل"],
    "action.remove": ["Remove", "إزالة"],
    "action.add": ["Add", "إضافة"],
    "action.copy": ["Copy", "نسخ"],
    "action.copied": ["Copied", "تم النسخ"],
    "action.back": ["Back", "رجوع"],
    "action.newOrg": ["New organization", "مؤسسة جديدة"],
    "action.inviteOwner": ["Invite owner", "دعوة مالك"],
    "action.inviteUser": ["Invite user", "دعوة مستخدم"],
    "action.newRole": ["New role", "دور جديد"],
    "action.newDept": ["New department", "قسم جديد"],
    "action.createInvite": ["Create and invite owner", "إنشاء ودعوة المالك"],
    "action.sendInvite": ["Send invitation", "إرسال الدعوة"],
    "action.setupLink": ["Create setup link", "إنشاء رابط الإعداد"],
    "action.saveRole": ["Save role", "حفظ الدور"],
    "action.copyInvite": ["Copy invitation", "نسخ الدعوة"],
    "action.parse": ["Parse PDF", "قراءة الملف"],
    "action.savePreview": ["Save preview", "حفظ المعاينة"],
    "action.confirmAudit": ["Confirm and create audit", "تأكيد وإنشاء التدقيق"],
    "action.saveProfile": ["Save profile", "حفظ الملف"],
    "action.done": ["Done", "تم"],
    "action.savePerms": ["Save permissions", "حفظ الصلاحيات"],
    "action.exceptions": ["Exceptional permissions", "صلاحيات استثنائية"],

    "field.department": ["Department", "القسم"],
    "field.auditor": ["Auditor", "المدقّق"],
    "field.date": ["Date", "التاريخ"],
    "field.linked": ["Linked department", "القسم المرتبط"],
    "field.none": ["None", "بدون"],
    "field.code": ["Code", "الرمز"],
    "field.nameAr": ["Arabic name", "الاسم بالعربية"],
    "field.nameEn": ["English name", "الاسم بالإنجليزية"],
    "field.type": ["Type", "النوع"],
    "field.selectType": ["Select a type", "اختر النوع"],
    "field.head": ["Head", "رئيس القسم"],
    "field.coordinator": ["Coordinator", "المنسق"],
    "field.assessor": ["Assessor", "المقيّم"],
    "field.status": ["Status", "الحالة"],
    "field.units": ["Units", "الوحدات"],
    "field.unitsHint": ["One unit name per line", "اسم وحدة في كل سطر"],
    "field.note": ["Note", "ملاحظة"],
    "field.active": ["Active", "نشط"],
    "field.inactive": ["Inactive", "غير نشط"],
    "field.activeHint": ["Inactive assessors stay in the list and cannot be chosen for new work.", "المقيّمون غير النشطين يبقون في القائمة ولا يمكن اختيارهم لعمل جديد."],
    "field.hospital": ["Hospital", "المستشفى"],
    "field.reviewer": ["Reviewer", "المراجع"],
    "field.ward": ["Department / ward", "القسم / الجناح"],
    "field.findingId": ["Finding id", "رقم الملاحظة"],
    "field.corrective": ["Corrective action", "الإجراء التصحيحي"],
    "field.responsible": ["Responsible", "المسؤول"],
    "field.target": ["Target date", "تاريخ الاستحقاق"],
    "field.file": ["File", "الملف"],
    "field.auditId": ["Audit id", "رقم التدقيق"],
    "field.pdf": ["PDF", "PDF"],
    "field.first": ["First name", "الاسم الأول"],
    "field.last": ["Last name", "اسم العائلة"],
    "field.email": ["Email", "البريد"],
    "field.role": ["Role", "الدور"],
    "field.departments": ["Departments", "الأقسام"],
    "field.orgName": ["Name", "الاسم"],
    "field.ownerFirst": ["Owner first name", "الاسم الأول للمالك"],
    "field.ownerLast": ["Owner last name", "اسم عائلة المالك"],
    "field.ownerEmail": ["Owner email", "بريد المالك"],
    "field.scope": ["What this role can see", "ما يراه هذا الدور"],
    "field.permissions": ["Permissions", "الصلاحيات"],
    "field.signIn": ["Account can sign in", "يمكن للحساب تسجيل الدخول"],
    "field.signInHint": ["Turn this off to mark them Inactive. They stay in the directory and cannot sign in.", "أوقف هذا لتعليم الحساب غير نشط. يبقى في الدليل ولا يستطيع الدخول."],
    "field.reissue": ["Send a new setup link", "إرسال رابط إعداد جديد"],
    "field.reissueHint": ["The account stays off until they open the link and choose a password.", "يبقى الحساب متوقفاً حتى يفتحوا الرابط ويختاروا كلمة مرور."],
    "field.selectDept": ["Select a department", "اختر قسماً"],

    "dash.scope": ["All audits on record.", "كل التدقيقات المسجلة."],
    "dash.attention": ["Needs attention", "يحتاج إلى متابعة"],
    "dash.attentionRule": ["Audits past their date or due today, CAPAs past their target date, findings with no CAPA, and findings open more than 30 days.", "تدقيقات تجاوزت تاريخها أو تستحق اليوم، وإجراءات تجاوزت تاريخها، وملاحظات بلا إجراء، وملاحظات مفتوحة أكثر من 30 يوماً."],
    "dash.search": ["Search attention items", "ابحث في عناصر المتابعة"],
    "dash.unavailable": ["Dashboard unavailable", "لوحة المعلومات غير متاحة"],

    "policy.search": ["Search settings", "بحث في الإعدادات"],
    "policy.effective": ["Effective date", "تاريخ السريان"],
    "policy.effectiveCol": ["Effective", "السريان"],
    "policy.when": ["When", "الوقت"],
    "policy.reason": ["Reason", "السبب"],
    "policy.reasonHint": ["Why you are changing this", "لماذا تغيّر هذه السياسة"],
    "policy.reasonRequired": ["Write a reason before saving. It can be up to 1000 characters.", "اكتب سبب التغيير قبل الحفظ. يمكن أن يصل إلى 1000 حرف."],
    "policy.reasonLong": ["The reason must be at most 1000 characters.", "السبب يجب ألا يزيد عن 1000 حرف."],
    "policy.reviewSave": ["Review and save", "مراجعة وحفظ"],
    "policy.import": ["Import", "استيراد"],
    "policy.export": ["Export", "تصدير"],
    "policy.history": ["History", "السجل"],
    "policy.historyLead": ["Each published version records who changed it and the old and new value.", "كل إصدار منشور يسجل من غيّره والقيمة القديمة والجديدة."],
    "policy.violations": ["Open items that do not match", "عناصر مفتوحة لا تطابق القاعدة"],
    "policy.violationsLead": ["These stay on record. A new rule flags them and does not delete them.", "تبقى في السجل. القاعدة الجديدة تعلّمها ولا تحذفها."],
    "policy.version": ["Version", "الإصدار"],
    "policy.who": ["Who", "من"],
    "policy.changes": ["Changes", "التغييرات"],
    "policy.detail": ["Detail", "التفصيل"],
    "policy.drift": ["The group template changed after this version. Saving adopts the current template.", "تغيّر قالب المجموعة بعد هذا الإصدار. الحفظ يعتمد القالب الحالي."],
    "policy.confirm": ["Confirm policy change", "تأكيد تغيير السياسة"],
    "policy.sections": ["Policy sections", "أقسام السياسة"],

    "empty.audits": ["No audits yet", "لا توجد تدقيقات بعد"],
    "empty.deptAudits": ["No audits for this department", "لا توجد تدقيقات لهذا القسم"],
    "empty.findings": ["No findings", "لا توجد ملاحظات"],
    "empty.capa": ["No CAPA", "لا توجد إجراءات"],
    "empty.evidence": ["No evidence", "لا توجد أدلة"],
    "empty.reviews": ["No reviews yet", "لا توجد مراجعات بعد"],
    "empty.departments": ["This hospital has no departments yet", "لا توجد أقسام في هذا المستشفى بعد"],
    "empty.assessors": ["No assessors", "لا يوجد مقيّمون"],
    "empty.orgs": ["No organizations yet", "لا توجد مؤسسات بعد"],
    "empty.owners": ["No owners invited yet", "لم تُرسل دعوات للملاك بعد"],
    "empty.users": ["No users yet", "لا يوجد مستخدمون بعد"],
    "empty.roles": ["No roles yet", "لا توجد أدوار بعد"],
    "empty.openHospital": ["Open a hospital first", "افتح مستشفى أولاً"],

    "assessor.departments": ["Departments", "الأقسام"],
    "assessor.deptLead": ["Add or remove departments here. Save stores the profile and these departments together. A department has one assessor, so adding one replaces the person already there.", "أضف الأقسام أو أزلها هنا. الحفظ يخزن الملف وهذه الأقسام معاً. للقسم مقيّم واحد، فالإضافة تستبدل الشخص الحالي."],
    "assessor.none": ["No departments", "لا أقسام"],
    "assessor.noneAssigned": ["No departments are assigned to this assessor.", "لا توجد أقسام معيّنة لهذا المقيّم."],

    "user.setupTitle": ["Setup link", "رابط الإعداد"],
    "user.setupLead": ["Send this once. The account stays off until they open it and choose a password.", "أرسله مرة واحدة. يبقى الحساب متوقفاً حتى يفتحوه ويختاروا كلمة مرور."],
    "user.exceptional": ["Exceptional permissions", "صلاحيات استثنائية"],
    "user.exceptionLead": ["These choices apply only to this person. They do not change the shared role, and they do not change anyone else who has that role.", "هذه الاختيارات لهذا الشخص فقط. لا تغيّر الدور المشترك، ولا تغيّر أي شخص آخر يحمل الدور نفسه."],
    "user.noExceptions": ["No exceptions", "لا استثناءات"],
    "user.unsaved": ["Unsaved", "غير محفوظ"],
    "user.permission": ["Permission", "الصلاحية"],
    "user.exception": ["Exception", "الاستثناء"],
    "user.saved": ["Permissions saved.", "حُفظت الصلاحيات."],
    "invite.title": ["Owner invitation", "دعوة المالك"],
    "org.createFirst": ["Create an organization first", "أنشئ مؤسسة أولاً"],

    "scope.hospital": ["Whole hospital", "المستشفى كله"],
    "scope.assessor": ["Own assessments", "تقييماته فقط"],
    "scope.department": ["One department", "قسم واحد"],
    "scope.list": ["Assigned departments", "الأقسام المعيّنة"],
    "scope.limited": ["Limited", "محدود"],

    "group.audits": ["Audits", "التدقيقات"],
    "group.findings": ["Findings and CAPA", "الملاحظات والإجراءات"],
    "group.evidence": ["Evidence", "الأدلة"],
    "group.dashboards": ["Dashboards", "لوحات المعلومات"],
    "group.import": ["Import and archive", "الاستيراد والأرشفة"],
    "group.hospital": ["Hospital", "المستشفى"],
    "group.reports": ["Reports", "التقارير"],
    "group.system": ["System", "النظام"],
    "group.other": ["Other", "أخرى"]
  };

  var ROLES = {
    SYSTEM_ADMINISTRATOR: ["System administrator", "مدير النظام"],
    HOSPITAL_OWNER: ["Hospital owner", "مالك المستشفى"],
    QUALITY_DIRECTOR: ["Quality director", "مدير الجودة"],
    QUALITY_SUPERVISOR: ["Quality supervisor", "مشرف الجودة"],
    ASSESSOR: ["Assessor", "مقيّم"],
    DEPARTMENT_COORDINATOR: ["Department coordinator", "منسق القسم"],
    VIEW_ONLY: ["View only", "اطلاع فقط"]
  };

  var ROLE_NAMES = {
    "system administrator": ROLES.SYSTEM_ADMINISTRATOR,
    "hospital owner": ROLES.HOSPITAL_OWNER,
    "quality director": ROLES.QUALITY_DIRECTOR,
    "quality supervisor": ROLES.QUALITY_SUPERVISOR,
    "assessor": ROLES.ASSESSOR,
    "department coordinator": ROLES.DEPARTMENT_COORDINATOR,
    "view only": ROLES.VIEW_ONLY
  };

  var PERMS = {
    CONDUCT_AUDIT: ["Conduct audits", "إجراء التدقيقات", "Create audits and medical-record reviews, and score checklist items.", "إنشاء التدقيقات ومراجعات السجلات الطبية، وتقييم عناصر القائمة."],
    MANAGE_FINDINGS: ["Manage findings", "إدارة الملاحظات", "Record and update findings.", "تسجيل الملاحظات وتحديثها."],
    MANAGE_CAPA: ["Manage CAPA", "إدارة الإجراءات التصحيحية", "Create and update corrective actions.", "إنشاء الإجراءات التصحيحية وتحديثها."],
    MANAGE_EVIDENCE: ["Manage evidence", "إدارة الأدلة", "Upload and manage evidence files.", "رفع ملفات الأدلة وإدارتها."],
    VIEW_COMMAND_CENTER: ["Command center", "مركز القيادة", "Open the command center dashboard.", "فتح لوحة مركز القيادة."],
    VIEW_EXECUTIVE_DASHBOARD: ["Executive dashboard", "لوحة التنفيذي", "Open the executive dashboard.", "فتح لوحة التنفيذي."],
    VIEW_ASSESSOR_DASHBOARD: ["Assessor dashboard", "لوحة المقيّم", "Open the assessor dashboard.", "فتح لوحة المقيّم."],
    VIEW_QUALITY_DIRECTOR_DASHBOARD: ["Quality director dashboard", "لوحة مدير الجودة", "Open the quality director dashboard.", "فتح لوحة مدير الجودة."],
    MANAGE_IMPORT: ["Import PDFs", "استيراد ملفات PDF", "Import audit PDFs.", "استيراد ملفات PDF للتدقيق."],
    ARCHIVE_AUDIT: ["Archive audits", "أرشفة التدقيقات", "Archive and restore audits.", "أرشفة التدقيقات واستعادتها."],
    EDIT_AUDIT_LIFECYCLE: ["Edit audit lifecycle", "تعديل دورة التدقيق", "Move a submitted audit through its lifecycle.", "تحريك التدقيق المُرسل عبر دورته."],
    DELETE_AUDIT_PDF: ["Delete an audit PDF", "حذف ملف PDF للتدقيق", "Delete the PDF attached to an audit.", "حذف ملف PDF المرفق بالتدقيق."],
    DELETE_AUDIT_ONLY: ["Delete an audit", "حذف تدقيق", "Delete an audit while leaving related records.", "حذف التدقيق مع الإبقاء على السجلات المرتبطة."],
    DELETE_AUDIT_EVERYTHING: ["Delete an audit and related records", "حذف التدقيق والسجلات المرتبطة", "Delete an audit and its related records.", "حذف التدقيق وسجلاته المرتبطة."],
    MANAGE_USER_PERMISSION_OVERRIDES: ["Manage exceptional permissions", "إدارة الصلاحيات الاستثنائية", "Grant or deny individual permissions for one person.", "منح صلاحيات فردية أو منعها لشخص واحد."],
    MANAGE_USERS: ["Manage users", "إدارة المستخدمين", "", ""],
    MANAGE_DEPARTMENTS: ["Manage departments", "إدارة الأقسام", "", ""],
    MANAGE_ASSESSORS: ["Manage assessors", "إدارة المقيّمين", "", ""],
    VIEW_ALL_ASSESSORS_DATA: ["See every assessor's work", "رؤية عمل كل المقيّمين", "", ""],
    GENERATE_REPORTS: ["Generate reports", "إنشاء التقارير", "", ""],
    MANAGE_EXPORT: ["Export data", "تصدير البيانات", "", ""],
    MANAGE_NOTIFICATIONS: ["Manage notifications", "إدارة الإشعارات", "", ""],
    MANAGE_SETTINGS: ["Manage settings", "إدارة الإعدادات", "", ""],
    VIEW_SYSTEM_LOGS: ["View system logs", "عرض سجلات النظام", "", ""],
    MANAGE_BACKUP: ["Manage backups", "إدارة النسخ الاحتياطي", "", ""],
    MANAGE_RESTORE: ["Restore backups", "استعادة النسخ الاحتياطي", "", ""],
    RESET_DATA: ["Reset data", "إعادة ضبط البيانات", "", ""],
    CREATE_ROLE: ["Create roles", "إنشاء الأدوار", "", ""],
    VIEW_ROLES: ["View roles", "عرض الأدوار", "", ""],
    UPDATE_ROLE: ["Update roles", "تحديث الأدوار", "", ""],
    DELETE_ROLE: ["Delete roles", "حذف الأدوار", "", ""],
    VIEW_PERMISSIONS: ["View permissions", "عرض الصلاحيات", "", ""],
    QUALITY_POLICY_VIEW: ["View quality policy", "عرض سياسة الجودة", "", ""],
    QUALITY_POLICY_EDIT: ["Edit quality policy", "تعديل سياسة الجودة", "", ""]
  };

  var STATUS = {
    Active: ["Active", "نشط"],
    Inactive: ["Inactive", "غير نشط"],
    Open: ["Open", "مفتوح"],
    Closed: ["Closed", "مغلق"],
    Draft: ["Draft", "مسودة"],
    "In Progress": ["In Progress", "قيد التنفيذ"],
    "Pending Review": ["Pending Review", "بانتظار المراجعة"],
    Submitted: ["Submitted", "مُرسل"],
    Approved: ["Approved", "معتمد"],
    Rejected: ["Rejected", "مرفوض"],
    Hospital: ["Hospital", "مستشفى"],
    HOSPITAL: ["Hospital", "مستشفى"],
    High: ["High", "عالٍ"],
    Medium: ["Medium", "متوسط"],
    Low: ["Low", "منخفض"],
    Critical: ["Critical", "حرج"],
    Major: ["Major", "جسيم"],
    Minor: ["Minor", "بسيط"],
    Observation: ["Observation", "ملاحظة"]
  };

  var SCORES = {
    met: ["Met", "مستوفى"],
    partial: ["Partial", "جزئي"],
    not: ["Not met", "غير مستوفى"],
    na: ["N/A", "لا ينطبق"],
    all: ["All", "الكل"],
    unscored: ["Unscored", "غير مُقيَّم"],
    ALL: ["All", "الكل"]
  };

  var KINDS = {
    "Past-due CAPA": ["Past-due CAPA", "إجراء تجاوز تاريخه"],
    "Finding with no CAPA": ["Finding with no CAPA", "ملاحظة بلا إجراء"],
    "Audit past its date": ["Audit past its date", "تدقيق تجاوز تاريخه"],
    "Open more than 30 days": ["Open more than 30 days", "مفتوحة أكثر من 30 يوماً"],
    "Audit due today": ["Audit due today", "تدقيق يستحق اليوم"]
  };

  function pair(key) {
    return STRINGS[key] || null;
  }

  function t(key) {
    var row = pair(key);
    if (!row) return "";
    return current() === "ar" ? row[1] : row[0];
  }

  function text(en, ar) {
    return current() === "ar" ? ar : en;
  }

  function fill(el, value) {
    var nodes = el.childNodes;
    var elementChild = false;
    var textNode = null;
    var i;
    for (i = 0; i < nodes.length; i++) {
      if (nodes[i].nodeType === 1) elementChild = true;
      if (!textNode && nodes[i].nodeType === 3 && String(nodes[i].textContent).trim()) textNode = nodes[i];
    }
    if (elementChild) {
      if (textNode) textNode.textContent = value;
      else el.insertBefore(document.createTextNode(value), el.firstChild);
      return;
    }
    el.textContent = value;
  }

  function apply(root) {
    var scope = root && root.querySelectorAll ? root : document;
    var list = scope.querySelectorAll("[data-i18n]");
    var i;
    for (i = 0; i < list.length; i++) fill(list[i], t(list[i].getAttribute("data-i18n")));
    list = scope.querySelectorAll("[data-i18n-placeholder]");
    for (i = 0; i < list.length; i++) list[i].setAttribute("placeholder", t(list[i].getAttribute("data-i18n-placeholder")));
    list = scope.querySelectorAll("[data-i18n-aria]");
    for (i = 0; i < list.length; i++) list[i].setAttribute("aria-label", t(list[i].getAttribute("data-i18n-aria")));
    list = scope.querySelectorAll("[data-i18n-title]");
    for (i = 0; i < list.length; i++) {
      var title = t(list[i].getAttribute("data-i18n-title"));
      list[i].setAttribute("title", title);
      if (list[i].dataset) list[i].dataset.restTitle = title;
    }
    var titleEl = document.querySelector("title[data-i18n]");
    if (titleEl && (!root || root === document || root === document.documentElement)) {
      document.title = t(titleEl.getAttribute("data-i18n"));
    }
    list = document.querySelectorAll("[data-lang-toggle]");
    var switchLabel = current() === "ar" ? "English" : "العربية";
    var switchName = current() === "ar" ? "Switch to English" : "التبديل إلى العربية";
    for (i = 0; i < list.length; i++) {
      list[i].textContent = switchLabel;
      list[i].setAttribute("aria-label", switchName);
      list[i].setAttribute("title", switchName);
    }
    var other = document.getElementById("col-other-name");
    if (other) other.textContent = current() === "ar" ? t("col.otherNameEn") : t("col.otherName");
  }

  function setLang(lang) {
    lang = lang === "ar" ? "ar" : "en";
    if (current() === lang && document.documentElement.dir === (lang === "ar" ? "rtl" : "ltr")) {
      persist(lang);
    }
    persist(lang);
    applyDir();
    apply(document);
    var event;
    try {
      event = new CustomEvent("mq-lang", { detail: { lang: lang } });
    } catch (e) {
      event = document.createEvent("Event");
      event.initEvent("mq-lang", true, true);
    }
    document.dispatchEvent(event);
  }

  function role(code, name) {
    var known = code && ROLES[code];
    if (!known && name) known = ROLE_NAMES[String(name).trim().toLowerCase()];
    if (known) return text(known[0], known[1]);
    return name || code || "";
  }

  function perm(code, fallback) {
    var row = PERMS[code];
    return row ? text(row[0], row[1]) : (fallback || code || "");
  }

  function permHelp(code, fallback) {
    var row = PERMS[code];
    if (!row) return fallback || "";
    var value = text(row[2], row[3]);
    return value || fallback || "";
  }

  function status(value) {
    var row = STATUS[value];
    return row ? text(row[0], row[1]) : (value == null ? "" : String(value));
  }

  function score(value) {
    var row = SCORES[value];
    return row ? text(row[0], row[1]) : (value == null ? "" : String(value));
  }

  function kind(value) {
    var row = KINDS[value];
    return row ? text(row[0], row[1]) : (value == null ? "" : String(value));
  }

  function age(value) {
    var textValue = value == null ? "" : String(value);
    if (textValue === "Today") return text("Today", "اليوم");
    if (textValue === "Unknown") return text("Unknown", "غير معروف");
    if (textValue === "1 day") return text("1 day", "يوم واحد");
    var match = /^(\d+) days$/.exec(textValue);
    if (match) return text(match[1] + " days", match[1] + " أيام");
    return textValue;
  }

  function person(value) {
    if (value === "No owner") return text("No owner", "بدون مسؤول");
    return value == null ? "" : String(value);
  }

  function count(n, one, many, oneAr, manyAr) {
    n = Number(n) || 0;
    if (current() === "ar") return n + " " + (n === 1 ? oneAr : manyAr);
    return n === 1 ? "1 " + one : n + " " + many;
  }

  window.mqLang = current;
  window.mqHasChoice = function () { return !!storedChoice(); };
  window.mqSetLang = setLang;
  window.mqT = t;
  window.mqText = text;
  window.mqApplyI18n = apply;
  window.mqRole = role;
  window.mqPerm = perm;
  window.mqPermHelp = permHelp;
  window.mqStatus = status;
  window.mqScore = score;
  window.mqKind = kind;
  window.mqAge = age;
  window.mqPerson = person;
  window.mqCount = count;

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", function () { apply(document); });
  else apply(document);

  document.addEventListener("click", function (event) {
    var button = event.target && event.target.closest ? event.target.closest("[data-lang-toggle]") : null;
    if (!button) return;
    event.preventDefault();
    setLang(current() === "ar" ? "en" : "ar");
  });
})(window, document);
