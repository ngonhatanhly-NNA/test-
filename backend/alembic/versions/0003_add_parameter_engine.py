"""add parameter engine columns

Revision ID: 0003_add_parameter_engine
Revises: 0002_import_and_generation
Create Date: 2026-10-16 14:00:00.000000
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0003_add_parameter_engine"
down_revision = "0002_import_and_generation"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Lưu cấu hình biến số và công thức cho câu hỏi gốc
    op.add_column(
        "questions",
        sa.Column("parameter_config", sa.JSON(), nullable=True),
    )
    # Cờ cho phép đổi số liệu khi bấm Generate
    op.add_column(
        "generation_configs",
        sa.Column(
            "vary_parameters",
            sa.Boolean(),
            nullable=False,
            server_default=sa.text("false"),
        ),
    )
    # Lưu nội dung câu hỏi và bộ số đã sinh cho từng mã đề
    op.add_column(
        "question_versions",
        sa.Column("rendered_content", sa.Text(), nullable=True),
    )
    op.add_column(
        "question_versions",
        sa.Column("parameter_values", sa.JSON(), nullable=True),
    )
    op.add_column(
        "question_versions",
        sa.Column("rendered_choices", sa.JSON(), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("question_versions", "rendered_choices")
    op.drop_column("question_versions", "parameter_values")
    op.drop_column("question_versions", "rendered_content")
    op.drop_column("generation_configs", "vary_parameters")
    op.drop_column("questions", "parameter_config")