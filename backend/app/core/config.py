from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Smart Exam Generator"
    environment: str = "development"  # development | production
    database_url: str = "postgresql+psycopg://exam:exam@localhost:5432/exam"
    secret_key: str = "change-me"
    storage_path: str = "./storage"
    max_upload_size: int = 20 * 1024 * 1024
    ocr_enabled: bool = False
    ai_enabled: bool = False
    llm_api_key: str = ""
    cors_origins: str = "http://localhost:3000"

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    @property
    def is_production(self) -> bool:
        return self.environment == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()
