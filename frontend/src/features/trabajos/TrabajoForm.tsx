/* oxlint-disable react/incompatible-library -- React Hook Form expone funciones estables por su propia API. */
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { AlertIcon, ArrowRightIcon } from '../../components/Icons'
import { trabajoSchema, type TrabajoFormValues } from '../../schemas/trabajoSchema'
import { ApiError, crearTrabajo } from '../../services/trabajosApi'
import type { CrearTrabajoRequest, Trabajo } from '../../types/trabajo'

interface TrabajoFormProps {
  onCreated: (trabajo: Trabajo) => void
}

const categorias = [
  { value: 'plomeria', label: 'Plomería' },
  { value: 'electricidad', label: 'Electricidad' },
  { value: 'cerrajeria', label: 'Cerrajería' },
  { value: 'limpieza', label: 'Limpieza' },
  { value: 'mantenimiento', label: 'Mantenimiento general' },
  { value: 'jardineria', label: 'Jardinería' },
]

const nuevaIdentidad = () => crypto.randomUUID()

export function TrabajoForm({ onCreated }: TrabajoFormProps) {
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<TrabajoFormValues>({
    resolver: zodResolver(trabajoSchema),
    defaultValues: {
      clienteId: nuevaIdentidad(),
      categoriaServicio: '',
      urgencia: 'MEDIA',
      ciudad: 'Bogotá',
      origen: 'MARKETPLACE',
      partnerId: '',
      moneda: 'COP',
    },
  })

  const origen = watch('origen')

  const submit = handleSubmit(async (values) => {
    setServerError(null)
    const payload: CrearTrabajoRequest = {
      ...values,
      partnerId: values.origen === 'MARKETPLACE' ? null : values.partnerId || null,
    }

    try {
      onCreated(await crearTrabajo(payload))
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Ocurrió un error inesperado.')
    }
  })

  return (
    <form className="job-form" onSubmit={submit} noValidate>
      <div className="panel-heading">
        <span className="section-number">01</span>
        <div>
          <h2>Cuéntanos qué necesitas</h2>
          <p>Completa los datos esenciales para registrar el trabajo.</p>
        </div>
      </div>

      {serverError && (
        <div className="alert alert-error" role="alert"><AlertIcon /><span>{serverError}</span></div>
      )}

      <div className="form-grid">
        <div className="field field-full">
          <div className="label-row">
            <label htmlFor="clienteId">Identificador del cliente</label>
            <button type="button" className="text-button" onClick={() => setValue('clienteId', nuevaIdentidad(), { shouldValidate: true })}>Generar otro</button>
          </div>
          <input id="clienteId" className={errors.clienteId ? 'invalid' : ''} {...register('clienteId')} aria-describedby={errors.clienteId ? 'clienteId-error' : undefined} />
          {errors.clienteId && <span className="field-error" id="clienteId-error">{errors.clienteId.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="categoriaServicio">Servicio</label>
          <select id="categoriaServicio" className={errors.categoriaServicio ? 'invalid' : ''} {...register('categoriaServicio')}>
            <option value="">Selecciona una opción</option>
            {categorias.map((categoria) => <option key={categoria.value} value={categoria.value}>{categoria.label}</option>)}
          </select>
          {errors.categoriaServicio && <span className="field-error">{errors.categoriaServicio.message}</span>}
        </div>

        <div className="field">
          <label htmlFor="ciudad">Ciudad</label>
          <input id="ciudad" list="ciudades" placeholder="Ej. Bogotá" className={errors.ciudad ? 'invalid' : ''} {...register('ciudad')} />
          <datalist id="ciudades"><option value="Bogotá" /><option value="Medellín" /><option value="Cali" /><option value="Barranquilla" /></datalist>
          {errors.ciudad && <span className="field-error">{errors.ciudad.message}</span>}
        </div>

        <fieldset className="field field-full urgency-field">
          <legend>Nivel de urgencia</legend>
          <div className="choice-group">
            {(['BAJA', 'MEDIA', 'ALTA', 'CRITICA'] as const).map((urgencia) => (
              <label className="choice-pill" key={urgencia}>
                <input type="radio" value={urgencia} {...register('urgencia')} />
                <span>{urgencia === 'CRITICA' ? 'Crítica' : urgencia.charAt(0) + urgencia.slice(1).toLowerCase()}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="field">
          <label htmlFor="origen">Origen de la solicitud</label>
          <select id="origen" {...register('origen')}>
            <option value="MARKETPLACE">Marketplace</option>
            <option value="SINIESTRO">Siniestro de un aliado</option>
            <option value="SUSCRIPCION">Plan o suscripción</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="moneda">Moneda</label>
          <select id="moneda" {...register('moneda')}>
            <option value="COP">COP · Peso colombiano</option>
            <option value="MXN">MXN · Peso mexicano</option>
            <option value="BRL">BRL · Real brasileño</option>
            <option value="ARS">ARS · Peso argentino</option>
          </select>
        </div>

        {origen !== 'MARKETPLACE' && (
          <div className="field field-full conditional-field">
            <div className="label-row">
              <label htmlFor="partnerId">Identificador del aliado</label>
              <button type="button" className="text-button" onClick={() => setValue('partnerId', nuevaIdentidad(), { shouldValidate: true })}>Generar ID de prueba</button>
            </div>
            <input id="partnerId" placeholder="UUID del asegurador o aliado" className={errors.partnerId ? 'invalid' : ''} {...register('partnerId')} />
            <small>Necesario para dirigir correctamente el trabajo al aliado que lo originó.</small>
            {errors.partnerId && <span className="field-error">{errors.partnerId.message}</span>}
          </div>
        )}
      </div>

      <div className="form-actions">
        <p><span>*</span> Al continuar, la solicitud será registrada y comunicada a los procesos relacionados.</p>
        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? <><span className="spinner" /> Registrando…</> : <>Crear trabajo <ArrowRightIcon /></>}
        </button>
      </div>
    </form>
  )
}
