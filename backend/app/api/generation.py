from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.schemas.generation import (
    GenerationConfigIn, GenerationConfigOut, GenerationResult, VersionDetail, VersionPreview, VersionSummary,
)
from app.services import exam_generator, version_service
from fastapi.responses import Response
from app.services.pdf_generator import PDFGeneratorService
from app.services.version_service import VersionService

router = APIRouter(tags=["generation"])


@router.post("/api/exams/{exam_id}/generate", response_model=GenerationResult, status_code=status.HTTP_201_CREATED)
def generate(exam_id: str, config: GenerationConfigIn, db: Session = Depends(get_db)):
    outcome = exam_generator.generate_exam_versions(db, exam_id, config)
    return GenerationResult(
        config=GenerationConfigOut.model_validate(outcome.config),
        versions=[VersionSummary.model_validate(v) for v in outcome.versions],
        warnings=outcome.warnings,
    )


@router.get("/api/exams/{exam_id}/versions", response_model=list[VersionSummary])
def list_versions(exam_id: str, db: Session = Depends(get_db)):
    return version_service.list_versions(db, exam_id)


@router.get("/api/versions/{version_id}", response_model=VersionDetail)
def get_version(version_id: str, db: Session = Depends(get_db)):
    return version_service.version_detail(db, version_service.get_version(db, version_id))


@router.get("/api/versions/{version_id}/preview", response_model=VersionPreview)
def preview_version(version_id: str, db: Session = Depends(get_db)):
    return version_service.render_version(db, version_service.get_version(db, version_id))

@router.get("/versions/{version_id}/export-pdf")
def export_exam_version_pdf(
    version_id: int,
    include_answers: bool = False,
    db: Session = Depends(get_db),
):
    version_data = VersionService.get_version_detail(db, version_id)
    if include_answers:
        pdf_bytes = PDFGeneratorService.build_answer_key_pdf(
            exam_title=version_data["exam_title"],
            version_code=version_data["version_code"],
            answer_mappings=version_data["answer_key"],
        )
        filename = f"dap_an_{version_data['version_code']}.pdf"
    else:
        pdf_bytes = PDFGeneratorService.build_exam_version_pdf(
            exam_title=version_data["exam_title"],
            version_code=version_data["version_code"],
            questions=version_data["questions"],
        )
        filename = f"de_thi_{version_data['version_code']}.pdf"

    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )