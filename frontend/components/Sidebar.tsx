const items = [
  { label: "Trang chủ", active: true },
  { label: "Đề thi" },
  { label: "Ngân hàng câu hỏi" },
  { label: "Tạo mã đề" },
  { label: "Lịch sử" },
  { label: "Cài đặt" },
];

export default function Sidebar() {
  return (
    <nav aria-label="Menu chính" className="w-56 shrink-0 border-r border-line bg-white p-4">
      <p className="mb-6 px-2 text-lg font-semibold text-brand">Smart Exam</p>
      <ul className="space-y-1">
        {items.map((i) => (
          <li key={i.label}>
            <span
              aria-current={i.active ? "page" : undefined}
              className={`block rounded px-2 py-2 text-sm ${
                i.active ? "bg-brand-soft font-medium text-brand" : "cursor-not-allowed text-muted"
              }`}
            >
              {i.label}
            </span>
          </li>
        ))}
      </ul>
    </nav>
  );
}
