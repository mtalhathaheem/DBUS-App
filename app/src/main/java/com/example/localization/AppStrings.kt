package com.example.localization

import com.example.model.AppLanguage
import com.example.model.UserRole

object AppStrings {
    fun appTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "DBUS School"
        AppLanguage.URDU -> "ڈی بس اسکول (DBUS)"
    }

    fun appSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "School Cloud System • Pakistan"
        AppLanguage.URDU -> "اسکول کلاؤڈ سسٹم • پاکستان"
    }

    fun welcomeRole(lang: AppLanguage, role: UserRole, name: String): String = when (lang) {
        AppLanguage.ENGLISH -> "Welcome, ${role.englishName}, $name"
        AppLanguage.URDU -> "خوش آمدید، ${role.urduName}، $name"
    }

    fun login(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Login"
        AppLanguage.URDU -> "لاگ ان"
    }

    fun signup(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.URDU -> "سائن اپ"
    }

    fun emailLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Email Address"
        AppLanguage.URDU -> "ای میل پتہ"
    }

    fun passwordLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.URDU -> "پاس ورڈ"
    }

    fun forgotPassword(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Forgot password?"
        AppLanguage.URDU -> "پاس ورڈ بھول گئے؟"
    }

    fun phoneLogin(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Phone Number with OTP"
        AppLanguage.URDU -> "فون نمبر بمعہ ون ٹائم پاس ورڈ (OTP)"
    }

    fun orContinueWith(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Or continue with"
        AppLanguage.URDU -> "یا جاری رکھیں بذریعہ"
    }

    fun studentApprovalNotice(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "A parent or school creates or approves a student account."
        AppLanguage.URDU -> "طالب علم کا اکاؤنٹ والدین یا اسکول انتظامیہ بناتی ہے یا منظور کرتی ہے۔"
    }

    fun permanentStudentId(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Permanent Student ID"
        AppLanguage.URDU -> "مستقل اسٹوڈنٹ آئی ڈی"
    }

    fun permanentIdNotice(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "This ID never changes and is never reused."
        AppLanguage.URDU -> "یہ آئی ڈی کبھی تبدیل نہیں ہوتی اور نہ ہی دوبارہ استعمال ہوتی ہے۔"
    }

    fun rowLevelSecurityActive(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Row Level Security Active"
        AppLanguage.URDU -> "رو لیول سیکیورٹی (RLS) فعال ہے"
    }

    fun rlsDescription(lang: AppLanguage, role: UserRole): String = when (lang) {
        AppLanguage.ENGLISH -> when (role) {
            UserRole.SuperAdmin -> "RLS Policy: Platform-wide governance, multi-institution oversight & subscriptions."
            UserRole.Parent -> "RLS Policy: You can only see your own children."
            UserRole.Teacher -> "RLS Policy: You can only see your assigned students."
            UserRole.Student -> "RLS Policy: You can only view your own student records."
            UserRole.Administrator -> "RLS Policy: Full administrative control and user assignment."
        }
        AppLanguage.URDU -> when (role) {
            UserRole.SuperAdmin -> "پالیسی: تمام تعلیمی اداروں کی نگرانی، سبسکرپشنز اور مجموعی سیکیورٹی۔"
            UserRole.Parent -> "پالیسی: آپ صرف اپنے بچوں کا ڈیٹا دیکھ سکتے ہیں۔"
            UserRole.Teacher -> "پالیسی: آپ صرف اپنے مقررہ طلباء کا ڈیٹا دیکھ سکتے ہیں۔"
            UserRole.Student -> "پالیسی: آپ صرف اپنا ریکارڈ دیکھ سکتے ہیں۔"
            UserRole.Administrator -> "پالیسی: مکمل انتظامی اختیارات اور طلباء و اساتذہ کی تعیناتی۔"
        }
    }

    // Navigation and Settings
    fun dashboard(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Dashboard"
        AppLanguage.URDU -> "ڈیش بورڈ"
    }

    fun settings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.URDU -> "ترتیبات"
    }

    fun account(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Account"
        AppLanguage.URDU -> "اکاؤنٹ"
    }

    fun privacyAndSecurity(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Privacy and Security"
        AppLanguage.URDU -> "پرائیویسی اور سیکیورٹی"
    }

    fun appearance(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Appearance"
        AppLanguage.URDU -> "ظاہری شکل (Appearance)"
    }

    fun notifications(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Notifications"
        AppLanguage.URDU -> "اطلاعات"
    }

    fun languageAndRegion(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Language and Region"
        AppLanguage.URDU -> "زبان اور علاقہ"
    }

    fun helpAndAbout(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Help and About DBUS"
        AppLanguage.URDU -> "مدد اور ڈی بس (DBUS) کے متعلق"
    }

    fun creditsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Project Credits & Leadership"
        AppLanguage.URDU -> "پروجیکٹ کی قیادت اور کریڈٹس"
    }

    fun ideaAndDesign(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Idea and Design by:"
        AppLanguage.URDU -> "خیال اور ڈیزائن:"
    }

    fun principalAndOwner(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Principal & Owner:"
        AppLanguage.URDU -> "پرنسپل اور مالک:"
    }

    fun developer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Developer:"
        AppLanguage.URDU -> "ڈیولپر:"
    }

    fun logout(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign Out"
        AppLanguage.URDU -> "سائن آؤٹ"
    }

    // Module 3: Attendance Strings
    fun attendance(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Attendance"
        AppLanguage.URDU -> "حاضری"
    }

    fun dailyAttendance(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Daily Attendance"
        AppLanguage.URDU -> "روزانہ کی حاضری"
    }

    fun monthlySummary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Monthly Summary"
        AppLanguage.URDU -> "ماہانہ خلاصہ"
    }

    fun yearlySummary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Academic Year Summary"
        AppLanguage.URDU -> "تعلیمی سال کا خلاصہ"
    }

    fun present(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Present"
        AppLanguage.URDU -> "حاضر"
    }

    fun absent(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Absent"
        AppLanguage.URDU -> "غیر حاضر"
    }

    fun late(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Late"
        AppLanguage.URDU -> "دیر سے"
    }

    fun leave(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Leave"
        AppLanguage.URDU -> "رخصت"
    }

    fun attendanceRate(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Attendance Rate"
        AppLanguage.URDU -> "حاضری کا تناسب"
    }

    fun calendarView(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Calendar View"
        AppLanguage.URDU -> "کیلنڈر ویو"
    }

    fun chartView(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Attendance Trend Chart"
        AppLanguage.URDU -> "حاضری کا گراف چارٹ"
    }

    fun exportAttendance(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Export Attendance"
        AppLanguage.URDU -> "حاضری ریکارڈ ایکسپورٹ"
    }

    fun changeHistory(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Change Log (Audit Trail)"
        AppLanguage.URDU -> "تبدیلیوں کی تاریخ (آڈٹ لاگ)"
    }

    fun sharedNote(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Shared Note (Visible to Parent & Student)"
        AppLanguage.URDU -> "مشترکہ نوٹ (والدین اور طالب علم پڑھ سکتے ہیں)"
    }

    fun privateNote(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Private Note (Teacher & Administrator Only)"
        AppLanguage.URDU -> "خفیہ نوٹ (صرف استاد اور منتظم کیلئے)"
    }

    fun privateNoteHiddenNotice(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Private notes are strictly confidential to faculty."
        AppLanguage.URDU -> "خفیہ نوٹس صرف اساتذہ اور انتظامیہ تک محدود ہیں۔"
    }

    fun schoolIdOrEmailLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "School ID or Email"
        AppLanguage.URDU -> "اسکول آئی ڈی یا ای میل"
    }

    fun noStudentsAssignedYet(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No students assigned yet"
        AppLanguage.URDU -> "ابھی کوئی طالب علم تفویض نہیں کیا گیا"
    }

    fun manageAccounts(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Manage Accounts"
        AppLanguage.URDU -> "اکاؤنٹس کا انتظام"
    }

    fun createAccount(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create Account"
        AppLanguage.URDU -> "نیا اکاؤنٹ بنائیں"
    }

    fun schoolCalendar(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "School Calendar"
        AppLanguage.URDU -> "اسکول کیلنڈر"
    }
}
