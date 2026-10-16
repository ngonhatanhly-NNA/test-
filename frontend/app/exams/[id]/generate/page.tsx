"use client";
import { useParams } from "next/navigation";
import GenerateForm from "@/components/GenerateForm";

export default function GeneratePage() {
  const { id } = useParams<{ id: string }>();
  return <GenerateForm examId={id} />;
}
