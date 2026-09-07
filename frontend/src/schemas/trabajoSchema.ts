import { z } from 'zod'

export const trabajoSchema = z.object({
  clienteId: z.string().trim().uuid('Ingresa un identificador de cliente válido.'),
  categoriaServicio: z.string().trim().min(2, 'Selecciona una categoría.'),
  urgencia: z.enum(['BAJA', 'MEDIA', 'ALTA', 'CRITICA']),
  ciudad: z.string().trim().min(2, 'Indica la ciudad donde se prestará el servicio.'),
  origen: z.enum(['MARKETPLACE', 'SINIESTRO', 'SUSCRIPCION']),
  partnerId: z.union([
    z.string().trim().uuid('Ingresa un identificador de aliado válido.'),
    z.literal(''),
  ]),
  moneda: z.enum(['COP', 'MXN', 'BRL', 'ARS']),
}).superRefine((data, context) => {
  if (data.origen !== 'MARKETPLACE' && !data.partnerId) {
    context.addIssue({
      code: 'custom',
      path: ['partnerId'],
      message: 'El aliado es obligatorio para siniestros y suscripciones.',
    })
  }
})

export type TrabajoFormValues = z.infer<typeof trabajoSchema>
