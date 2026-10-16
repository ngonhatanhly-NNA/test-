"use client";
import { useParams } from "next/navigation";
import QuestionReview from "@/components/QuestionReview";

export default function ExamPage() {
  const { id } = useParams<{ id: string }>();
  return <QuestionReview examId={id} />;
}
