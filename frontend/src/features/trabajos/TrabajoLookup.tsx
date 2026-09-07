import { useState, type FormEvent } from 'react'
import { AlertIcon, SearchIcon } from '../../components/Icons'
import { ApiError, consultarTrabajo } from '../../services/trabajosApi'
import type { Trabajo } from '../../types/trabajo'
import { TrabajoDetails } from './TrabajoSummary'

interface TrabajoLookupProps {
  initialId?: string
}

export function TrabajoLookup({ initialId = '' }: TrabajoLookupProps) {
  const [id, setId] = useState(initialId)
  const [trabajo, setTrabajo] = useState<Trabajo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setTrabajo(null)

    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id.trim())) {
      setError('Ingresa un identificador UUID válido.')
      return
    }

    setLoading(true)
    try {
      setTrabajo(await consultarTrabajo(id.trim()))
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'No fue posible realizar la consulta.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lookup-panel">
      <div className="panel-heading">
        <span className="section-number">02</span>
        <div><h2>Consulta un trabajo</h2><p>Usa el identificador entregado al crear la solicitud.</p></div>
      </div>

      <form className="lookup-form" onSubmit={submit} noValidate>
        <label htmlFor="lookup-id">Identificador del trabajo</label>
        <div className="search-control">
          <SearchIcon />
          <input id="lookup-id" value={id} onChange={(event) => setId(event.target.value)} placeholder="Ej. a63f8965-6b70-4e2b-bf01-0636401287a2" />
          <button type="submit" disabled={loading}>{loading ? 'Buscando…' : 'Consultar'}</button>
        </div>
      </form>

      {error && <div className="alert alert-error" role="alert"><AlertIcon /><span>{error}</span></div>}
      {trabajo && <div className="lookup-result"><TrabajoDetails trabajo={trabajo} /></div>}

      {!trabajo && !error && (
        <div className="empty-state">
          <span><SearchIcon /></span>
          <h3>Tu solicitud aparecerá aquí</h3>
          <p>Podrás revisar su información, origen, prioridad y estado actual.</p>
        </div>
      )}
    </div>
  )
}
