FROM python:3.12-slim

WORKDIR /app

COPY contracts /app/contracts
COPY services/integraciones /app/services/integraciones

RUN pip install --no-cache-dir \
    "/app/services/integraciones[persistence,messaging]"

ENV PYTHONUNBUFFERED=1

CMD ["python", "-m", "integraciones"]
