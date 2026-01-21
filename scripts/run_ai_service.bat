@echo off
echo Starting Python AI Service...
set PYTHONPATH=%CD%
.\venv\Scripts\uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pause
