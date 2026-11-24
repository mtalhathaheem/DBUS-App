-- ==============================================================================
-- DBUS School Management System (Pakistan)
-- Supabase PostgreSQL Schema & Row Level Security (RLS) Configuration
-- ==============================================================================
-- Strict Access Control Matrix:
--   - Student: Own data only
--   - Parent: Own children only
--   - Teacher: Assigned students only
--   - Administrator / SuperAdmin: All data (full access)
--   - Private Notes: Teacher and Administrator ONLY (Parents & Students blocked)
--   - Storage Objects: Private buckets with matching folder isolation
--   - Client Key: ANON key ONLY (service_role strictly prohibited in client apps)
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- 2. AUTOMATIC RLS ENFORCEMENT ON FUTURE TABLES
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.enable_rls_on_new_table()
RETURNS event_trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT objid::regclass::text AS table_name
             FROM pg_event_trigger_ddl_commands()
             WHERE command_tag = 'CREATE TABLE'
               AND schema_name = 'public'
    LOOP
        EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY;', r.table_name);
        EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY;', r.table_name);
        RAISE NOTICE 'RLS automatically enabled and forced on new table: %', r.table_name;
    END LOOP;
END;
$$;

DROP EVENT TRIGGER IF EXISTS trg_enable_rls_on_new_table;
CREATE EVENT TRIGGER trg_enable_rls_on_new_table
ON ddl_command_end
WHEN TAG IN ('CREATE TABLE')
EXECUTE FUNCTION public.enable_rls_on_new_table();

-- ==============================================================================
-- 3. CORE & MODULE TABLES DEFINITION
-- ==============================================================================

-- 3.1 Profiles Table (Users & Roles)
CREATE TABLE IF NOT EXISTS public.profiles (
    id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    login_id text UNIQUE,
    name text NOT NULL,
    email text NOT NULL,
    phone text,
    role text NOT NULL CHECK (role IN ('Student', 'Parent', 'Teacher', 'Administrator', 'SuperAdmin')),
    permanent_student_id text UNIQUE,
    approval_status text NOT NULL DEFAULT 'Approved' CHECK (approval_status IN ('Approved', 'Pending Approval', 'Rejected', 'Archived')),
    linked_parent_id uuid REFERENCES auth.users(id),
    avatar_color_hex bigint NOT NULL DEFAULT 1784687,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3.2 Students Table (Permanent Student Record)
CREATE TABLE IF NOT EXISTS public.students (
    permanent_student_id text PRIMARY KEY,
    auth_user_id uuid REFERENCES auth.users(id),
    full_name text NOT NULL,
    urdu_name text NOT NULL DEFAULT '',
    father_name text NOT NULL,
    b_form_number text NOT NULL DEFAULT '', -- Sensitive: Pakistan National B-Form
    grade text NOT NULL,
    section text NOT NULL,
    parent_id uuid REFERENCES auth.users(id),
    parent_media_consent_given boolean NOT NULL DEFAULT true,
    is_archived boolean NOT NULL DEFAULT false,
    avatar_color_hex bigint NOT NULL DEFAULT 1784687,
    photo_uri text,
    issued_date date NOT NULL DEFAULT CURRENT_DATE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3.3 Student Teachers Junction Table (Teacher Assignments)
CREATE TABLE IF NOT EXISTS public.student_teachers (
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    teacher_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (student_permanent_id, teacher_id)
);

-- 3.4 Skill Assessments Table (10 Core Skills & Shared Notes)
CREATE TABLE IF NOT EXISTS public.skill_assessments (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    assessed_by_teacher_id uuid REFERENCES auth.users(id),
    assessed_by_teacher_name text NOT NULL,
    assessment_date date NOT NULL DEFAULT CURRENT_DATE,
    scores jsonb NOT NULL,
    shared_notes jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3.5 Skill Assessment Private Notes Table (Strict Faculty Isolation)
CREATE TABLE IF NOT EXISTS public.skill_assessment_private_notes (
    assessment_id text NOT NULL REFERENCES public.skill_assessments(id) ON DELETE CASCADE,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    skill_name text NOT NULL,
    private_note text NOT NULL,
    created_by uuid NOT NULL REFERENCES auth.users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (assessment_id, skill_name)
);

-- 3.6 Attendance Records Table
CREATE TABLE IF NOT EXISTS public.attendance_records (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    date date NOT NULL,
    status text NOT NULL CHECK (status IN ('Present', 'Absent', 'Late', 'Leave')),
    note text NOT NULL DEFAULT '',
    marked_by text NOT NULL,
    last_modified date NOT NULL DEFAULT CURRENT_DATE,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT unique_student_attendance_per_day UNIQUE (student_permanent_id, date)
);

-- 3.7 Attendance Change Logs Table (Audit trail)
CREATE TABLE IF NOT EXISTS public.attendance_change_logs (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendance_record_id text NOT NULL REFERENCES public.attendance_records(id) ON DELETE CASCADE,
    timestamp timestamptz NOT NULL DEFAULT now(),
    modified_by text NOT NULL,
    previous_status text NOT NULL,
    new_status text NOT NULL,
    note text NOT NULL DEFAULT ''
);

-- 3.8 Class Activity Media Table (Photos, Videos, Audio)
CREATE TABLE IF NOT EXISTS public.class_activity_media (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    media_type text NOT NULL CHECK (media_type IN ('PHOTO', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    file_path text NOT NULL,
    caption text NOT NULL DEFAULT '',
    activity_name text NOT NULL,
    teacher_name text NOT NULL,
    teacher_id uuid NOT NULL REFERENCES auth.users(id),
    date date NOT NULL DEFAULT CURRENT_DATE,
    file_size_bytes bigint NOT NULL DEFAULT 0,
    duration_seconds integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.9 Student Assignments Table
CREATE TABLE IF NOT EXISTS public.student_assignments (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    title text NOT NULL,
    subject text NOT NULL,
    due_date date NOT NULL,
    submission_date date,
    media_type text CHECK (media_type IN ('PHOTO', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    file_path text,
    file_name text,
    file_size_bytes bigint NOT NULL DEFAULT 0,
    is_submitted boolean NOT NULL DEFAULT false,
    grade text,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.10 School Holidays Table
CREATE TABLE IF NOT EXISTS public.school_holidays (
    id text PRIMARY KEY,
    name text NOT NULL,
    urdu_name text NOT NULL DEFAULT '',
    date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.11 Attendance Alerts Table
CREATE TABLE IF NOT EXISTS public.attendance_alerts (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    student_name text NOT NULL,
    alert_type text NOT NULL CHECK (alert_type IN ('ABSENT_OR_LATE', 'LOW_ATTENDANCE')),
    message text NOT NULL,
    urdu_message text NOT NULL DEFAULT '',
    date date NOT NULL,
    is_read boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.12 School Calendar Events Table (Assignments, Quizzes, Exams, Holidays, Other)
CREATE TABLE IF NOT EXISTS public.school_calendar_events (
    id text PRIMARY KEY,
    title text NOT NULL,
    urdu_title text NOT NULL DEFAULT '',
    date date NOT NULL,
    event_type text NOT NULL CHECK (event_type IN ('ASSIGNMENT', 'QUIZ', 'EXAM', 'HOLIDAY', 'OTHER')),
    description text NOT NULL DEFAULT '',
    created_by uuid REFERENCES auth.users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3.13 Subjects Table
CREATE TABLE IF NOT EXISTS public.subjects (
    id text PRIMARY KEY,
    name text NOT NULL,
    urdu_name text NOT NULL DEFAULT '',
    code text NOT NULL,
    grade text NOT NULL,
    teacher_name text NOT NULL DEFAULT '',
    weekly_hours integer NOT NULL DEFAULT 4,
    description text NOT NULL DEFAULT '',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.14 Exam Schedules Table
CREATE TABLE IF NOT EXISTS public.exam_schedules (
    id text PRIMARY KEY,
    exam_name text NOT NULL,
    subject text NOT NULL,
    date date NOT NULL,
    time text NOT NULL DEFAULT '',
    room text NOT NULL DEFAULT '',
    total_marks integer NOT NULL DEFAULT 100,
    passing_marks integer NOT NULL DEFAULT 40,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.15 Student Exam Results Table
CREATE TABLE IF NOT EXISTS public.student_exam_results (
    id text PRIMARY KEY,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    exam_name text NOT NULL,
    term text NOT NULL,
    subject text NOT NULL,
    marks_obtained integer NOT NULL,
    total_marks integer NOT NULL DEFAULT 100,
    grade text NOT NULL,
    remarks text NOT NULL DEFAULT '',
    date date NOT NULL DEFAULT CURRENT_DATE,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.16 Parent-Teacher Announcements & Comments Tables
CREATE TABLE IF NOT EXISTS public.parent_announcements (
    id text PRIMARY KEY,
    title text NOT NULL,
    urdu_title text NOT NULL DEFAULT '',
    message text NOT NULL,
    urdu_message text NOT NULL DEFAULT '',
    author_name text NOT NULL,
    author_role text NOT NULL,
    target_grade text NOT NULL DEFAULT 'All Grades',
    date date NOT NULL DEFAULT CURRENT_DATE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.announcement_comments (
    id text PRIMARY KEY,
    announcement_id text NOT NULL REFERENCES public.parent_announcements(id) ON DELETE CASCADE,
    author_id uuid REFERENCES auth.users(id),
    author_name text NOT NULL,
    author_role text NOT NULL,
    text text NOT NULL,
    timestamp text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.17 Skill Evidence Table
CREATE TABLE IF NOT EXISTS public.skill_evidence (
    id text PRIMARY KEY,
    skill text NOT NULL,
    student_permanent_id text NOT NULL REFERENCES public.students(permanent_student_id) ON DELETE CASCADE,
    title text NOT NULL,
    description text NOT NULL DEFAULT '',
    media_type text NOT NULL CHECK (media_type IN ('PHOTO', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    file_path text NOT NULL,
    score_out_of_ten integer NOT NULL DEFAULT 0,
    score_level integer NOT NULL DEFAULT 0,
    recorded_date date NOT NULL DEFAULT CURRENT_DATE,
    evaluated_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.18 Institutions Table (SuperAdmin Multi-School Ecosystem)
CREATE TABLE IF NOT EXISTS public.institutions (
    id text PRIMARY KEY,
    name text NOT NULL,
    urdu_name text NOT NULL DEFAULT '',
    institution_type text NOT NULL,
    city text NOT NULL DEFAULT '',
    admin_contact_name text NOT NULL,
    admin_email text NOT NULL,
    student_count integer NOT NULL DEFAULT 0,
    teacher_count integer NOT NULL DEFAULT 0,
    plan text NOT NULL,
    status text NOT NULL DEFAULT 'Approved',
    registered_date date NOT NULL DEFAULT CURRENT_DATE,
    is_integration_connected boolean NOT NULL DEFAULT false,
    active_sync_method text NOT NULL DEFAULT 'CLOUD_PORTAL',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.19 Sync History Logs Table
CREATE TABLE IF NOT EXISTS public.sync_history_logs (
    id text PRIMARY KEY,
    timestamp text NOT NULL,
    method text NOT NULL,
    records_synced integer NOT NULL DEFAULT 0,
    duplicates_found integer NOT NULL DEFAULT 0,
    status text NOT NULL,
    initiated_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3.20 School Settings Table (Attendance Threshold & Weekly Off Days)
CREATE TABLE IF NOT EXISTS public.school_settings (
    id text PRIMARY KEY DEFAULT 'default',
    low_attendance_threshold integer NOT NULL DEFAULT 75,
    weekly_off_days integer[] NOT NULL DEFAULT ARRAY[1],
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.school_settings (id, low_attendance_threshold, weekly_off_days)
VALUES ('default', 75, ARRAY[1])
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 4. ENABLE & FORCE ROW LEVEL SECURITY ON ALL TABLES
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles FORCE ROW LEVEL SECURITY;

ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students FORCE ROW LEVEL SECURITY;

ALTER TABLE public.student_teachers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_teachers FORCE ROW LEVEL SECURITY;

ALTER TABLE public.skill_assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.skill_assessments FORCE ROW LEVEL SECURITY;

ALTER TABLE public.skill_assessment_private_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.skill_assessment_private_notes FORCE ROW LEVEL SECURITY;

ALTER TABLE public.attendance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance_records FORCE ROW LEVEL SECURITY;

ALTER TABLE public.attendance_change_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance_change_logs FORCE ROW LEVEL SECURITY;

ALTER TABLE public.class_activity_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.class_activity_media FORCE ROW LEVEL SECURITY;

ALTER TABLE public.student_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_assignments FORCE ROW LEVEL SECURITY;

ALTER TABLE public.school_holidays ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_holidays FORCE ROW LEVEL SECURITY;

ALTER TABLE public.attendance_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance_alerts FORCE ROW LEVEL SECURITY;

ALTER TABLE public.school_calendar_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_calendar_events FORCE ROW LEVEL SECURITY;

ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects FORCE ROW LEVEL SECURITY;

ALTER TABLE public.exam_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exam_schedules FORCE ROW LEVEL SECURITY;

ALTER TABLE public.student_exam_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_exam_results FORCE ROW LEVEL SECURITY;

ALTER TABLE public.parent_announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.parent_announcements FORCE ROW LEVEL SECURITY;

ALTER TABLE public.announcement_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.announcement_comments FORCE ROW LEVEL SECURITY;

ALTER TABLE public.skill_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.skill_evidence FORCE ROW LEVEL SECURITY;

ALTER TABLE public.institutions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.institutions FORCE ROW LEVEL SECURITY;

ALTER TABLE public.sync_history_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sync_history_logs FORCE ROW LEVEL SECURITY;

ALTER TABLE public.school_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_settings FORCE ROW LEVEL SECURITY;

-- ==============================================================================
-- 5. SECURITY DEFINER HELPER FUNCTIONS & LOGIN EMAIL RESOLVER
-- ==============================================================================

-- Resolves a School Login ID (e.g. A-001, T-001, S-001, P-001) or Permanent Student ID
-- to its email address for Supabase Password Auth without exposing profile tables to anon.
CREATE OR REPLACE FUNCTION public.resolve_login_email(p_identifier text)
RETURNS text
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT email
    FROM public.profiles
    WHERE approval_status <> 'Archived'
      AND (
          lower(login_id) = lower(trim(p_identifier))
          OR lower(permanent_student_id) = lower(trim(p_identifier))
          OR lower(email) = lower(trim(p_identifier))
      )
    LIMIT 1;
$$;

REVOKE ALL ON FUNCTION public.resolve_login_email(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.resolve_login_email(text) TO anon, authenticated;

-- Check if current user is an Administrator or SuperAdmin
CREATE OR REPLACE FUNCTION public.is_administrator()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role IN ('Administrator', 'SuperAdmin')
    );
$$;

-- Check if current user is a Teacher (or Admin)
CREATE OR REPLACE FUNCTION public.is_teacher_or_admin()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role IN ('Teacher', 'Administrator', 'SuperAdmin')
    );
$$;

-- Check if current user is a Teacher assigned to the target student
CREATE OR REPLACE FUNCTION public.is_assigned_teacher(target_student_id text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.student_teachers
        WHERE student_permanent_id = target_student_id
          AND teacher_id = auth.uid()
    );
$$;

-- Check if current user is a Parent of the target student
CREATE OR REPLACE FUNCTION public.is_parent_of_student(target_student_id text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.students
        WHERE permanent_student_id = target_student_id
          AND parent_id = auth.uid()
    );
$$;

-- Check if current user is the target student themselves
CREATE OR REPLACE FUNCTION public.is_student_self(target_student_id text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid()
          AND role = 'Student'
          AND permanent_student_id = target_student_id
    );
$$;

-- Unified student authorization helper
CREATE OR REPLACE FUNCTION public.can_access_student(target_student_id text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT public.is_administrator()
        OR public.is_assigned_teacher(target_student_id)
        OR public.is_parent_of_student(target_student_id)
        OR public.is_student_self(target_student_id);
$$;

-- ==============================================================================
-- 6. ROW LEVEL SECURITY POLICIES BY TABLE
-- ==============================================================================

-- 6.1 PROFILES POLICIES
DROP POLICY IF EXISTS "profiles_select_policy" ON public.profiles;
CREATE POLICY "profiles_select_policy" ON public.profiles
FOR SELECT USING (
    id = auth.uid()
    OR public.is_administrator()
    OR EXISTS (
        SELECT 1 FROM public.students
        WHERE auth_user_id = profiles.id AND parent_id = auth.uid()
    )
    OR EXISTS (
        SELECT 1 FROM public.student_teachers st
        JOIN public.students s ON s.permanent_student_id = st.student_permanent_id
        WHERE s.auth_user_id = profiles.id AND st.teacher_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "profiles_insert_policy" ON public.profiles;
CREATE POLICY "profiles_insert_policy" ON public.profiles
FOR INSERT WITH CHECK (
    public.is_administrator()
);

DROP POLICY IF EXISTS "profiles_update_policy" ON public.profiles;
CREATE POLICY "profiles_update_policy" ON public.profiles
FOR UPDATE USING (
    id = auth.uid() OR public.is_administrator()
) WITH CHECK (
    public.is_administrator()
    OR (
        id = auth.uid()
        AND role = (SELECT p.role FROM public.profiles p WHERE p.id = auth.uid())
        AND approval_status = (SELECT p.approval_status FROM public.profiles p WHERE p.id = auth.uid())
    )
);

DROP POLICY IF EXISTS "profiles_delete_policy" ON public.profiles;
CREATE POLICY "profiles_delete_policy" ON public.profiles
FOR DELETE USING (
    public.is_administrator()
);

-- 6.2 STUDENTS POLICIES
DROP POLICY IF EXISTS "students_select_policy" ON public.students;
CREATE POLICY "students_select_policy" ON public.students
FOR SELECT USING (
    public.is_administrator()
    OR parent_id = auth.uid()
    OR public.is_assigned_teacher(permanent_student_id)
    OR public.is_student_self(permanent_student_id)
);

DROP POLICY IF EXISTS "students_insert_policy" ON public.students;
CREATE POLICY "students_insert_policy" ON public.students
FOR INSERT WITH CHECK (
    public.is_administrator()
);

DROP POLICY IF EXISTS "students_update_policy" ON public.students;
CREATE POLICY "students_update_policy" ON public.students
FOR UPDATE USING (
    public.is_administrator()
    OR parent_id = auth.uid()
    OR public.is_assigned_teacher(permanent_student_id)
) WITH CHECK (
    public.is_administrator()
    OR parent_id = auth.uid()
    OR public.is_assigned_teacher(permanent_student_id)
);

DROP POLICY IF EXISTS "students_delete_policy" ON public.students;
CREATE POLICY "students_delete_policy" ON public.students
FOR DELETE USING (
    public.is_administrator()
);

-- 6.3 STUDENT_TEACHERS POLICIES
DROP POLICY IF EXISTS "student_teachers_select_policy" ON public.student_teachers;
CREATE POLICY "student_teachers_select_policy" ON public.student_teachers
FOR SELECT USING (
    public.is_administrator()
    OR teacher_id = auth.uid()
    OR public.is_parent_of_student(student_permanent_id)
    OR public.is_student_self(student_permanent_id)
);

DROP POLICY IF EXISTS "student_teachers_admin_manage" ON public.student_teachers;
CREATE POLICY "student_teachers_admin_manage" ON public.student_teachers
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

-- 6.4 SKILL_ASSESSMENTS POLICIES
DROP POLICY IF EXISTS "skill_assessments_select_policy" ON public.skill_assessments;
CREATE POLICY "skill_assessments_select_policy" ON public.skill_assessments
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "skill_assessments_insert_policy" ON public.skill_assessments;
CREATE POLICY "skill_assessments_insert_policy" ON public.skill_assessments
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "skill_assessments_update_policy" ON public.skill_assessments;
CREATE POLICY "skill_assessments_update_policy" ON public.skill_assessments
FOR UPDATE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "skill_assessments_delete_policy" ON public.skill_assessments;
CREATE POLICY "skill_assessments_delete_policy" ON public.skill_assessments
FOR DELETE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

-- 6.5 SKILL_ASSESSMENT_PRIVATE_NOTES POLICIES (Teacher & Admin ONLY)
DROP POLICY IF EXISTS "private_notes_select_policy" ON public.skill_assessment_private_notes;
CREATE POLICY "private_notes_select_policy" ON public.skill_assessment_private_notes
FOR SELECT USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "private_notes_insert_policy" ON public.skill_assessment_private_notes;
CREATE POLICY "private_notes_insert_policy" ON public.skill_assessment_private_notes
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "private_notes_update_policy" ON public.skill_assessment_private_notes;
CREATE POLICY "private_notes_update_policy" ON public.skill_assessment_private_notes
FOR UPDATE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "private_notes_delete_policy" ON public.skill_assessment_private_notes;
CREATE POLICY "private_notes_delete_policy" ON public.skill_assessment_private_notes
FOR DELETE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

-- 6.6 ATTENDANCE_RECORDS POLICIES
DROP POLICY IF EXISTS "attendance_records_select_policy" ON public.attendance_records;
CREATE POLICY "attendance_records_select_policy" ON public.attendance_records
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "attendance_records_insert_policy" ON public.attendance_records;
CREATE POLICY "attendance_records_insert_policy" ON public.attendance_records
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "attendance_records_update_policy" ON public.attendance_records;
CREATE POLICY "attendance_records_update_policy" ON public.attendance_records
FOR UPDATE USING (
    public.is_administrator()
    OR (public.is_assigned_teacher(student_permanent_id) AND date = CURRENT_DATE)
);

-- 6.7 ATTENDANCE_CHANGE_LOGS POLICIES
DROP POLICY IF EXISTS "attendance_logs_select_policy" ON public.attendance_change_logs;
CREATE POLICY "attendance_logs_select_policy" ON public.attendance_change_logs
FOR SELECT USING (
    public.is_administrator()
    OR EXISTS (
        SELECT 1 FROM public.attendance_records ar
        WHERE ar.id = attendance_change_logs.attendance_record_id
          AND public.can_access_student(ar.student_permanent_id)
    )
);

DROP POLICY IF EXISTS "attendance_logs_insert_policy" ON public.attendance_change_logs;
CREATE POLICY "attendance_logs_insert_policy" ON public.attendance_change_logs
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR EXISTS (
        SELECT 1 FROM public.attendance_records ar
        WHERE ar.id = attendance_change_logs.attendance_record_id
          AND public.is_assigned_teacher(ar.student_permanent_id)
    )
);

-- 6.8 CLASS_ACTIVITY_MEDIA POLICIES
DROP POLICY IF EXISTS "class_media_select_policy" ON public.class_activity_media;
CREATE POLICY "class_media_select_policy" ON public.class_activity_media
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "class_media_insert_policy" ON public.class_activity_media;
CREATE POLICY "class_media_insert_policy" ON public.class_activity_media
FOR INSERT WITH CHECK (
    (
        public.is_administrator()
        OR (public.is_assigned_teacher(student_permanent_id) AND teacher_id = auth.uid())
    )
    AND EXISTS (
        SELECT 1 FROM public.students
        WHERE permanent_student_id = class_activity_media.student_permanent_id
          AND parent_media_consent_given = true
    )
);

DROP POLICY IF EXISTS "class_media_update_policy" ON public.class_activity_media;
CREATE POLICY "class_media_update_policy" ON public.class_activity_media
FOR UPDATE USING (
    public.is_administrator()
    OR (teacher_id = auth.uid() AND public.is_assigned_teacher(student_permanent_id))
);

DROP POLICY IF EXISTS "class_media_delete_policy" ON public.class_activity_media;
CREATE POLICY "class_media_delete_policy" ON public.class_activity_media
FOR DELETE USING (
    public.is_administrator()
    OR (teacher_id = auth.uid() AND public.is_assigned_teacher(student_permanent_id))
);

-- 6.9 STUDENT_ASSIGNMENTS POLICIES
DROP POLICY IF EXISTS "assignments_select_policy" ON public.student_assignments;
CREATE POLICY "assignments_select_policy" ON public.student_assignments
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "assignments_insert_policy" ON public.student_assignments;
CREATE POLICY "assignments_insert_policy" ON public.student_assignments
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "assignments_update_policy" ON public.student_assignments;
CREATE POLICY "assignments_update_policy" ON public.student_assignments
FOR UPDATE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
    OR public.is_parent_of_student(student_permanent_id)
    OR public.is_student_self(student_permanent_id)
);

DROP POLICY IF EXISTS "assignments_delete_policy" ON public.student_assignments;
CREATE POLICY "assignments_delete_policy" ON public.student_assignments
FOR DELETE USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

-- 6.10 SCHOOL_HOLIDAYS POLICIES
DROP POLICY IF EXISTS "holidays_select_policy" ON public.school_holidays;
CREATE POLICY "holidays_select_policy" ON public.school_holidays
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "holidays_manage_policy" ON public.school_holidays;
CREATE POLICY "holidays_manage_policy" ON public.school_holidays
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

-- 6.11 ATTENDANCE_ALERTS POLICIES
DROP POLICY IF EXISTS "alerts_select_policy" ON public.attendance_alerts;
CREATE POLICY "alerts_select_policy" ON public.attendance_alerts
FOR SELECT USING (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
    OR (alert_type = 'ABSENT_OR_LATE' AND public.is_parent_of_student(student_permanent_id))
    OR (alert_type = 'ABSENT_OR_LATE' AND public.is_student_self(student_permanent_id))
);

DROP POLICY IF EXISTS "alerts_insert_policy" ON public.attendance_alerts;
CREATE POLICY "alerts_insert_policy" ON public.attendance_alerts
FOR INSERT WITH CHECK (
    public.is_administrator()
    OR public.is_assigned_teacher(student_permanent_id)
);

DROP POLICY IF EXISTS "alerts_manage_policy" ON public.attendance_alerts;
CREATE POLICY "alerts_manage_policy" ON public.attendance_alerts
FOR UPDATE USING (public.is_administrator());

DROP POLICY IF EXISTS "alerts_delete_policy" ON public.attendance_alerts;
CREATE POLICY "alerts_delete_policy" ON public.attendance_alerts
FOR DELETE USING (public.is_administrator());

-- 6.12 SCHOOL_CALENDAR_EVENTS POLICIES
DROP POLICY IF EXISTS "calendar_events_select_policy" ON public.school_calendar_events;
CREATE POLICY "calendar_events_select_policy" ON public.school_calendar_events
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "calendar_events_manage_policy" ON public.school_calendar_events;
CREATE POLICY "calendar_events_manage_policy" ON public.school_calendar_events
FOR ALL USING (public.is_teacher_or_admin()) WITH CHECK (public.is_teacher_or_admin());

-- 6.13 SUBJECTS POLICIES
DROP POLICY IF EXISTS "subjects_select_policy" ON public.subjects;
CREATE POLICY "subjects_select_policy" ON public.subjects
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "subjects_manage_policy" ON public.subjects;
CREATE POLICY "subjects_manage_policy" ON public.subjects
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

-- 6.14 EXAM_SCHEDULES POLICIES
DROP POLICY IF EXISTS "exam_schedules_select_policy" ON public.exam_schedules;
CREATE POLICY "exam_schedules_select_policy" ON public.exam_schedules
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "exam_schedules_manage_policy" ON public.exam_schedules;
CREATE POLICY "exam_schedules_manage_policy" ON public.exam_schedules
FOR ALL USING (public.is_teacher_or_admin()) WITH CHECK (public.is_teacher_or_admin());

-- 6.15 STUDENT_EXAM_RESULTS POLICIES
DROP POLICY IF EXISTS "exam_results_select_policy" ON public.student_exam_results;
CREATE POLICY "exam_results_select_policy" ON public.student_exam_results
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "exam_results_manage_policy" ON public.student_exam_results;
CREATE POLICY "exam_results_manage_policy" ON public.student_exam_results
FOR ALL USING (
    public.is_administrator() OR public.is_assigned_teacher(student_permanent_id)
) WITH CHECK (
    public.is_administrator() OR public.is_assigned_teacher(student_permanent_id)
);

-- 6.16 PARENT_ANNOUNCEMENTS & COMMENTS POLICIES
DROP POLICY IF EXISTS "announcements_select_policy" ON public.parent_announcements;
CREATE POLICY "announcements_select_policy" ON public.parent_announcements
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "announcements_manage_policy" ON public.parent_announcements;
CREATE POLICY "announcements_manage_policy" ON public.parent_announcements
FOR ALL USING (public.is_teacher_or_admin()) WITH CHECK (public.is_teacher_or_admin());

DROP POLICY IF EXISTS "announcement_comments_select_policy" ON public.announcement_comments;
CREATE POLICY "announcement_comments_select_policy" ON public.announcement_comments
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "announcement_comments_insert_policy" ON public.announcement_comments;
CREATE POLICY "announcement_comments_insert_policy" ON public.announcement_comments
FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- 6.17 SKILL_EVIDENCE POLICIES
DROP POLICY IF EXISTS "skill_evidence_select_policy" ON public.skill_evidence;
CREATE POLICY "skill_evidence_select_policy" ON public.skill_evidence
FOR SELECT USING (public.can_access_student(student_permanent_id));

DROP POLICY IF EXISTS "skill_evidence_manage_policy" ON public.skill_evidence;
CREATE POLICY "skill_evidence_manage_policy" ON public.skill_evidence
FOR ALL USING (
    public.is_administrator() OR public.is_assigned_teacher(student_permanent_id)
) WITH CHECK (
    public.is_administrator() OR public.is_assigned_teacher(student_permanent_id)
);

-- 6.18 INSTITUTIONS & SYNC_HISTORY_LOGS & SCHOOL_SETTINGS POLICIES
DROP POLICY IF EXISTS "institutions_select_policy" ON public.institutions;
CREATE POLICY "institutions_select_policy" ON public.institutions
FOR SELECT USING (public.is_administrator());

DROP POLICY IF EXISTS "institutions_manage_policy" ON public.institutions;
CREATE POLICY "institutions_manage_policy" ON public.institutions
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

DROP POLICY IF EXISTS "sync_logs_admin_policy" ON public.sync_history_logs;
CREATE POLICY "sync_logs_admin_policy" ON public.sync_history_logs
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

DROP POLICY IF EXISTS "school_settings_select_policy" ON public.school_settings;
CREATE POLICY "school_settings_select_policy" ON public.school_settings
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "school_settings_manage_policy" ON public.school_settings;
CREATE POLICY "school_settings_manage_policy" ON public.school_settings
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

-- ==============================================================================
-- 7. STORAGE BUCKETS & STORAGE OBJECTS RLS POLICIES
-- ==============================================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
    ('student-media', 'student-media', false, 52428800, ARRAY['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'audio/mpeg', 'audio/mp4', 'audio/wav']),
    ('assignments', 'assignments', false, 52428800, ARRAY['image/jpeg', 'image/png', 'application/pdf', 'video/mp4', 'audio/mpeg', 'audio/mp4', 'audio/wav'])
ON CONFLICT (id) DO UPDATE SET public = false;

ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE storage.objects FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "storage_student_media_select" ON storage.objects;
CREATE POLICY "storage_student_media_select" ON storage.objects
FOR SELECT USING (
    bucket_id IN ('student-media', 'assignments')
    AND (
        public.is_administrator()
        OR public.is_assigned_teacher((storage.foldername(name))[1])
        OR public.is_parent_of_student((storage.foldername(name))[1])
        OR public.is_student_self((storage.foldername(name))[1])
    )
);

DROP POLICY IF EXISTS "storage_student_media_insert" ON storage.objects;
CREATE POLICY "storage_student_media_insert" ON storage.objects
FOR INSERT WITH CHECK (
    (
        bucket_id = 'student-media'
        AND (
            public.is_administrator()
            OR public.is_assigned_teacher((storage.foldername(name))[1])
            OR public.is_parent_of_student((storage.foldername(name))[1])
        )
        AND EXISTS (
            SELECT 1 FROM public.students
            WHERE permanent_student_id = (storage.foldername(name))[1]
              AND parent_media_consent_given = true
        )
    )
    OR
    (
        bucket_id = 'assignments'
        AND (
            public.is_administrator()
            OR public.is_student_self((storage.foldername(name))[1])
            OR public.is_parent_of_student((storage.foldername(name))[1])
            OR public.is_assigned_teacher((storage.foldername(name))[1])
        )
    )
);

DROP POLICY IF EXISTS "storage_student_media_delete" ON storage.objects;
CREATE POLICY "storage_student_media_delete" ON storage.objects
FOR DELETE USING (
    bucket_id IN ('student-media', 'assignments')
    AND (
        public.is_administrator()
        OR auth.uid() = owner
    )
);
