from abc import ABC, abstractmethod
from pathlib import Path

from app.core.config import get_settings


class StorageService(ABC):
    @abstractmethod
    def save_file(self, data: bytes, key: str) -> str: ...

    @abstractmethod
    def get_file(self, key: str) -> bytes: ...

    @abstractmethod
    def delete_file(self, key: str) -> None: ...


class LocalStorageService(StorageService):
    """Local filesystem storage. An S3-compatible implementation can replace it later."""

    def __init__(self, root: Path) -> None:
        self.root = root.resolve()
        self.root.mkdir(parents=True, exist_ok=True)

    def _path(self, key: str) -> Path:
        path = (self.root / key).resolve()
        if self.root not in path.parents:
            raise ValueError("Invalid storage key")
        return path

    def save_file(self, data: bytes, key: str) -> str:
        path = self._path(key)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return key

    def get_file(self, key: str) -> bytes:
        return self._path(key).read_bytes()

    def delete_file(self, key: str) -> None:
        self._path(key).unlink(missing_ok=True)


_storage: StorageService | None = None


def get_storage() -> StorageService:
    global _storage
    if _storage is None:
        _storage = LocalStorageService(Path(get_settings().storage_path))
    return _storage
