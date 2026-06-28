#!/bin/bash
set -e

cd /app/web
/app/venv/bin/gunicorn -w 2 -b 0.0.0.0:8080 app:app &

cd /app
exec java -jar /app/app.jar
