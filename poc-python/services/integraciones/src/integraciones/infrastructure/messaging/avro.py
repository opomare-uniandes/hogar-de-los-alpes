import json
from pathlib import Path

from pulsar.schema import AvroSchema


class CatalogoAvro:
    """Carga una sola vez los esquemas que el cliente registra en Pulsar."""

    def __init__(self, contracts_dir: Path) -> None:
        self._contracts_dir = contracts_dir
        self._cache: dict[str, AvroSchema] = {}

    def obtener(self, relative_path: str) -> AvroSchema:
        if relative_path not in self._cache:
            path = self._contracts_dir / relative_path
            with path.open(encoding="utf-8") as schema_file:
                definition = json.load(schema_file)
            self._cache[relative_path] = AvroSchema(
                record_cls=None,
                schema_definition=definition,
            )
        return self._cache[relative_path]

