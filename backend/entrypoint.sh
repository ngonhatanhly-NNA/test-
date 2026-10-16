#!/usr/bin/env bash
set -e

echo "Waiting for PostgreSQL to be ready..."
python - <<END
import time, os, psycopg2
dsn = os.environ.get("DATABASE_URL", "").replace("+psycopg2", "")
for _ in range(30):
    try:
        conn = psycopg2.connect(dsn)
        conn.close()
        break
    except Exception:
        time.sleep(1)
else:
    raise RuntimeError("Database connection timeout")
END

echo "Running Alembic migrations..."
alembic upgrade head

echo "Starting FastAPI server..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4