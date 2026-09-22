# Portal web — Hogar de los Alpes

Interfaz React + TypeScript para crear y consultar trabajos a través del
`bff-service`, el único punto de entrada público. El portal mantiene separados el comando de creación y la consulta,
en línea con CQS, y explica al usuario el flujo asincrónico sin afirmar que los procesos
posteriores ya terminaron.

## Requisitos

- Node.js 20.19 o superior.
- `bff-service` disponible en `http://localhost:8090` (Docker Compose) o en `http://localhost:8080` (ejecución directa).

## Ejecución local

```bash
npm install
npm run dev
```

Abrir `http://localhost:5173`. En desarrollo, Vite redirige las peticiones de `/api`
a `http://localhost:8090`, por lo que no es necesario habilitar CORS en Spring Boot.

## Comandos

```bash
npm run dev       # servidor de desarrollo
npm run build     # validación TypeScript y build de producción
npm run lint      # análisis estático
npm test          # pruebas de componentes e integración HTTP simulada
npm run preview   # previsualiza el build de producción
```

Para apuntar a otra instancia, copie `.env.example` como `.env` y ajuste
`VITE_API_BASE_URL`.

## Funcionalidad

- Formulario de creación con validación de UUID y campos requeridos.
- `partnerId` condicional para siniestros y suscripciones.
- Estados de carga y errores de conexión o negocio.
- Confirmación con ID y resumen del trabajo creado.
- Consulta por UUID y presentación del estado persistido.
- Diseño responsivo y navegación accesible por teclado.
