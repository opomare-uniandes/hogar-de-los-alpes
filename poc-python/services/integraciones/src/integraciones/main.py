import logging


def main() -> None:
    """Punto de entrada temporal mientras se conectan Pulsar y PostgreSQL."""

    logging.basicConfig(level=logging.INFO)
    logging.getLogger(__name__).info(
        "integraciones-service listo; adaptadores de Pulsar y PostgreSQL pendientes"
    )


if __name__ == "__main__":
    main()

