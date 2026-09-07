import type { CrearTrabajoRequest, Trabajo } from '../types/trabajo'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    })
  } catch {
    throw new ApiError('No pudimos conectarnos con el servicio. Verifica que trabajos-service esté disponible.', 0)
  }

  if (!response.ok) {
    const details = await response.text()
    const fallback = response.status === 404
      ? 'No encontramos un trabajo con ese identificador.'
      : 'La solicitud no pudo completarse. Revisa los datos e inténtalo de nuevo.'

    throw new ApiError(details || fallback, response.status)
  }

  return response.json() as Promise<T>
}

export function crearTrabajo(payload: CrearTrabajoRequest) {
  return request<Trabajo>('/trabajos', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function consultarTrabajo(id: string) {
  return request<Trabajo>(`/trabajos/${encodeURIComponent(id)}`)
}
