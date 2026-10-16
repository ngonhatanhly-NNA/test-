/* eslint-disable @next/next/no-img-element */
import { API_BASE } from "@/lib/api";

export default function QuestionImages({ ids }: { ids: string[] }) {
  if (ids.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-3">
      {ids.map((id) => (
        <img key={id} src={`${API_BASE}/api/images/${id}`} alt="Hình minh họa của câu hỏi"
             className="max-h-56 rounded border border-line bg-white" />
      ))}
    </div>
  );
}
