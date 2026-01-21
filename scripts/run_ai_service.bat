@echo off
echo Starting Python AI Service...
set GEMINI_API_KEY=AIzaSyAfGj64ca1oSZsHkbP7FSl-5R06yyxQ0zM
set PYTHONPATH=%CD%
.\venv\Scripts\uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pause
