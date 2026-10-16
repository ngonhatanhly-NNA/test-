"use client";
import { useParams } from "next/navigation";
import VersionPreview from "@/components/VersionPreview";

export default function VersionPage() {
  const { id } = useParams<{ id: string }>();
  return <VersionPreview versionId={id} />;
}
