-- Migration: 20261115000001_full_tables_and_auth_rpc.sql
-- Adds resolve_login_email RPC for ID+Password login, profiles delete policy,
-- and complete module tables (subjects, exam_schedules, student_exam_results,
-- parent_announcements, announcement_comments, skill_evidence, institutions,
-- sync_history_logs, school_settings) with strict Row Level Security.

ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_color_hex bigint NOT NULL DEFAULT 1784687;
ALTER TABLE public.class_activity_media ADD COLUMN IF NOT EXISTS date date NOT NULL DEFAULT CURRENT_DATE;

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

CREATE TABLE IF NOT EXISTS public.school_settings (
    id text PRIMARY KEY DEFAULT 'default',
    low_attendance_threshold integer NOT NULL DEFAULT 75,
    weekly_off_days integer[] NOT NULL DEFAULT ARRAY[1],
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.school_settings (id, low_attendance_threshold, weekly_off_days)
VALUES ('default', 75, ARRAY[1])
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "profiles_delete_policy" ON public.profiles;
CREATE POLICY "profiles_delete_policy" ON public.profiles
FOR DELETE USING (
    public.is_administrator()
);

-- Enable & Force RLS on all newly created tables
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

-- Policies for subjects
DROP POLICY IF EXISTS "subjects_select_policy" ON public.subjects;
CREATE POLICY "subjects_select_policy" ON public.subjects
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "subjects_manage_policy" ON public.subjects;
CREATE POLICY "subjects_manage_policy" ON public.subjects
FOR ALL USING (public.is_administrator()) WITH CHECK (public.is_administrator());

-- Policies for exam_schedules
DROP POLICY IF EXISTS "exam_schedules_select_policy" ON public.exam_schedules;
CREATE POLICY "exam_schedules_select_policy" ON public.exam_schedules
FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "exam_schedules_manage_policy" ON public.exam_schedules;
CREATE POLICY "exam_schedules_manage_policy" ON public.exam_schedules
FOR ALL USING (public.is_teacher_or_admin()) WITH CHECK (public.is_teacher_or_admin());

-- Policies for student_exam_results
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

-- Policies for parent_announcements & announcement_comments
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

-- Policies for skill_evidence
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

-- Policies for institutions, sync_history_logs, and school_settings
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
