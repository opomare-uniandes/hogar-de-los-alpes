import { useState } from 'react'
import { ArrowRightIcon, CopyIcon, SuccessIcon } from '../../components/Icons'
import type { Trabajo } from '../../types/trabajo'

interface TrabajoSummaryProps {
  trabajo: Trabajo
  onCreateAnother: () => void
  onLookup: () => void
}

const labels: Record<string, string> = {
  MARKETPLACE: 'Marketplace', SINIESTRO: 'Siniestro', SUSCRIPCION: 'Suscripción',
  BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta', CRITICA: 'Crítica', CREADO: 'Creado',
}

const titleCase = (value: string) => value.replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
const display = (value: string) => labels[value] ?? titleCase(value)

export function TrabajoDetails({ trabajo }: { trabajo: Trabajo }) {
  return (
    <div className="details-card">
      <div className="details-header">
        <div><span>Estado actual</span><strong>{display(trabajo.estado)}</strong></div>
        <span className="status-badge"><i /> {display(trabajo.estado)}</span>
      </div>
      <dl className="details-grid">
        <div><dt>Servicio</dt><dd>{display(trabajo.categoriaServicio)}</dd></div>
        <div><dt>Urgencia</dt><dd>{display(trabajo.urgencia)}</dd></div>
        <div><dt>Ciudad</dt><dd>{trabajo.ciudad}</dd></div>
        <div><dt>Origen</dt><dd>{display(trabajo.origen)}</dd></div>
        <div><dt>Moneda</dt><dd>{trabajo.moneda}</dd></div>
        <div><dt>Creado</dt><dd>{new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(trabajo.fechaCreacion))}</dd></div>
      </dl>
    </div>
  )
}

export function TrabajoSummary({ trabajo, onCreateAnother, onLookup }: TrabajoSummaryProps) {
  const [copied, setCopied] = useState(false)

  const copyId = async () => {
    await navigator.clipboard.writeText(trabajo.id)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1800)
  }

  return (
    <div className="success-panel">
      <div className="success-heading">
        <SuccessIcon />
        <span>Solicitud registrada</span>
        <h2>Tu trabajo ya está en marcha.</h2>
        <p>Guarda este identificador. Lo necesitarás para consultar la solicitud.</p>
      </div>

      <div className="id-card">
        <span>Identificador del trabajo</span>
        <div><code>{trabajo.id}</code><button type="button" onClick={copyId} aria-label="Copiar identificador"><CopyIcon /> {copied ? 'Copiado' : 'Copiar'}</button></div>
      </div>

      <TrabajoDetails trabajo={trabajo} />

      <div className="success-actions">
        <button type="button" className="secondary-button" onClick={onCreateAnother}>Crear otra solicitud</button>
        <button type="button" className="primary-button" onClick={onLookup}>Consultar trabajo <ArrowRightIcon /></button>
      </div>
    </div>
  )
}
