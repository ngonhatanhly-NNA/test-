# backend/app/services/parameter_service.py
import ast
import math
import operator
import random
from typing import Any, Dict, List, Tuple

SAFE_OPERATORS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.Pow: operator.pow,
    ast.Mod: operator.mod,
    ast.USub: operator.neg,
}

SAFE_FUNCTIONS = {
    "sqrt": math.sqrt,
    "sin": math.sin,
    "cos": math.cos,
    "tan": math.tan,
    "abs": abs,
    "round": round,
    "pi": math.pi,
}


class SafeExpressionEvaluator(ast.NodeVisitor):
    def __init__(self, variables: Dict[str, float]):
        self.variables = variables

    def visit_Expression(self, node: ast.Expression) -> float:
        return float(self.visit(node.body))

    def visit_Constant(self, node: ast.Constant) -> float:
        if isinstance(node.value, (int, float)):
            return float(node.value)
        raise ValueError(f"Hằng số không hợp lệ: {node.value}")

    def visit_Name(self, node: ast.Name) -> Any:
        if node.id in self.variables:
            return self.variables[node.id]
        if node.id in SAFE_FUNCTIONS:
            return SAFE_FUNCTIONS[node.id]
        raise ValueError(f"Biến chưa được khai báo: {node.id}")

    def visit_BinOp(self, node: ast.BinOp) -> float:
        left = self.visit(node.left)
        right = self.visit(node.right)
        op_func = SAFE_OPERATORS.get(type(node.op))
        if not op_func:
            raise ValueError(f"Toán tử không hỗ trợ: {type(node.op)}")
        return float(op_func(left, right))

    def visit_UnaryOp(self, node: ast.UnaryOp) -> float:
        operand = self.visit(node.operand)
        op_func = SAFE_OPERATORS.get(type(node.op))
        if not op_func:
            raise ValueError(f"Toán tử 1 ngôi không hỗ trợ: {type(node.op)}")
        return float(op_func(operand))

    def visit_Call(self, node: ast.Call) -> float:
        func = self.visit(node.func)
        args = [self.visit(arg) for arg in node.args]
        return float(func(*args))


def evaluate_math_formula(formula: str, variables: Dict[str, float]) -> float:
    parsed = ast.parse(formula.strip(), mode="eval")
    return SafeExpressionEvaluator(variables).visit(parsed)


def format_numeric_answer(value: float, decimals: int = 2, unit: str = "") -> str:
    rounded = round(value, decimals)
    if abs(rounded - round(rounded)) < 1e-9:
        num_str = str(int(round(rounded)))
    else:
        num_str = f"{rounded:.{decimals}f}".rstrip("0").rstrip(".")
    return f"{num_str} {unit}".strip()


class ParameterService:
    @staticmethod
    def sample_variables(
        variables_spec: Dict[str, Dict[str, Any]], rng: random.Random
    ) -> Dict[str, float]:
        sampled: Dict[str, float] = {}
        for name, spec in variables_spec.items():
            min_val = float(spec.get("min", 1))
            max_val = float(spec.get("max", 10))
            step = float(spec.get("step", 1))
            if step <= 0:
                step = 1.0
            total_steps = max(0, int(round((max_val - min_val) / step)))
            chosen = min_val + rng.randint(0, total_steps) * step
            sampled[name] = (
                int(round(chosen)) if abs(chosen - round(chosen)) < 1e-9 else round(chosen, 4)
            )
        return sampled

    @classmethod
    def apply_parameters(
        cls,
        question_content: str,
        choices: List[Dict[str, Any]],
        parameter_config: Dict[str, Any] | None,
        rng: random.Random,
    ) -> Tuple[str, List[Dict[str, Any]], Dict[str, float]]:
        """
        Sinh bộ số mới, cập nhật nội dung câu hỏi và text của từng đáp án.
        Giữ nguyên choice['id'] và choice['is_correct'] để hệ thống trộn đáp án
        và AnswerKey hoạt động chính xác tuyệt đối.
        """
        if not parameter_config or not parameter_config.get("enabled"):
            return question_content, [dict(c) for c in choices], {}

        template = parameter_config.get("template") or question_content
        variables_spec = parameter_config.get("variables") or {}
        correct_formula = parameter_config.get("correct_formula", "").strip()
        distractor_formulas = parameter_config.get("distractor_formulas") or []
        decimals = int(parameter_config.get("decimals", 2))
        unit = str(parameter_config.get("unit", "")).strip()

        if not correct_formula or not variables_spec:
            return question_content, [dict(c) for c in choices], {}

        for _ in range(30):
            sampled = cls.sample_variables(variables_spec, rng)
            try:
                correct_val = evaluate_math_formula(correct_formula, sampled)
                distractor_vals = [
                    evaluate_math_formula(f, sampled) for f in distractor_formulas if f.strip()
                ]
            except (ZeroDivisionError, ValueError):
                continue

            correct_text = format_numeric_answer(correct_val, decimals, unit)
            distractor_texts = [
                format_numeric_answer(v, decimals, unit) for v in distractor_vals
            ]

            # Nếu người dùng không nhập đủ công thức nhiễu, tự tạo nhiễu lệch chuẩn
            offset = 1
            while len(distractor_texts) < max(0, len(choices) - 1):
                fallback_val = correct_val + (offset * (1 if offset % 2 == 1 else -1))
                candidate = format_numeric_answer(fallback_val, decimals, unit)
                if candidate != correct_text and candidate not in distractor_texts:
                    distractor_texts.append(candidate)
                offset += 1

            all_texts = [correct_text] + distractor_texts[: len(choices) - 1]
            if len(set(all_texts)) == len(all_texts):
                break
        else:
            return question_content, [dict(c) for c in choices], {}

        try:
            rendered_question = template.format(**sampled)
        except KeyError:
            rendered_question = question_content

        updated_choices: List[Dict[str, Any]] = []
        d_idx = 0
        for ch in choices:
            new_ch = dict(ch)
            if new_ch.get("is_correct"):
                new_ch["content"] = correct_text
            else:
                if d_idx < len(distractor_texts):
                    new_ch["content"] = distractor_texts[d_idx]
                    d_idx += 1
            updated_choices.append(new_ch)

        return rendered_question, updated_choices, sampled