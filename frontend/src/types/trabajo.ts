export const urgencias = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'] as const
export const origenes = ['MARKETPLACE', 'SINIESTRO', 'SUSCRIPCION'] as const
export const monedas = ['COP', 'MXN', 'BRL', 'ARS'] as const

export type Urgencia = (typeof urgencias)[number]
export type OrigenTrabajo = (typeof origenes)[number]
export type Moneda = (typeof monedas)[number]

export interface CrearTrabajoRequest {
  clienteId: string
  categoriaServicio: string
  urgencia: Urgencia
  ciudad: string
  origen: OrigenTrabajo
  partnerId: string | null
  moneda: Moneda
}

export interface Trabajo extends CrearTrabajoRequest {
  id: string
  estado: string
  fechaCreacion: string
}
