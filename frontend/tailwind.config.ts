import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        paper: "#f7f8fa",
        ink: "#1f2937",
        muted: "#6b7280",
        line: "#e3e7ee",
        brand: { DEFAULT: "#2f6f8f", soft: "#e8f1f6" },
      },
    },
  },
  plugins: [],
};
export default config;
