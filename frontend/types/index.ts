export type ExamStatus =
  | "UPLOADED" | "PARSING" | "DRAFT_REVIEW" | "APPROVED"
  | "GENERATING" | "GENERATED" | "EXPORTED";
export interface VariableSpec {
  min: number;
  max: number;
  step: number;
}

export interface ParameterConfig {
  enabled: boolean;
  template: string;
  variables: Record<string, VariableSpec>;
  correct_formula: string;
  distractor_formulas: string[];
  decimals: number;
  unit: string;
}
export interface Exam {
  id: string;
  name: string;
  description: string | null;
  subject: string | null;
  status: ExamStatus;
  question_count: number;
  version_count: number;
  created_at: string;
  updated_at: string;
}

export interface Choice {
  id: string;
  question_id: string;
  position: number;
  original_label: string;
  content: string;
  is_correct: boolean;
  pinned: boolean;
}

export interface Question {
  id: string;
  exam_id: string;
  original_number: number | null;
  position: number;
  content: string;
  shuffleable: boolean;
  parameterized: boolean;
  has_image: boolean;
  correct_choice_id: string | null;
  image_ids: string[];
  import_warnings: string[];
  choices: Choice[];
  parameter_config?: ParameterConfig | null;
}

export interface ChoiceInput {
  id?: string;
  content: string;
  is_correct: boolean;
  pinned: boolean;
}

export interface QuestionInput {
  content: string;
  shuffleable: boolean;
  parameterized: boolean;
  choices: ChoiceInput[];
}

export interface UploadResult {
  exam: Exam;
  source: "text" | "ocr";
  question_count: number;
  warnings: { question_id: string | null; number: number | null; message: string }[];
}

export interface GenerationConfigInput {
  number_of_versions: number;
  shuffle_questions: boolean;
  shuffle_choices: boolean;
  change_parameters: boolean;
  difficulty_mode: "same";
  seed: number | null;
}

export interface VersionSummary {
  id: string;
  exam_id: string;
  version_code: string;
  seed: number;
  question_count: number;
  status: string;
  created_at: string;
}

export interface GenerationResult {
  config: { seed: number };
  versions: VersionSummary[];
  warnings: string[];
}

export interface Issue {
  severity: "error" | "warning";
  code: string;
  message: string;
  question_id: string | null;
  position: number | null;
}

export interface VersionDetail extends VersionSummary {
  answer_key: { number: number; question_id: string; correct_label: string }[];
  issues: Issue[];
  is_valid: boolean;
}

export interface RenderedQuestion {
  number: number;
  question_id: string;
  content: string;
  image_ids: string[];
  choices: { label: string; choice_id: string; content: string }[];
  correct_label: string;
}

export interface VersionPreview {
  version: VersionSummary;
  exam_name: string;
  questions: RenderedQuestion[];
}

export interface ApiErrorBody {
  error: { code: string; message: string; details: Record<string, unknown> };
}
