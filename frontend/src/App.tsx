import { useState } from 'react'
import './App.css'
import { SearchIcon } from './components/Icons'
import { TrabajoForm } from './features/trabajos/TrabajoForm'
import { TrabajoLookup } from './features/trabajos/TrabajoLookup'
import { TrabajoSummary } from './features/trabajos/TrabajoSummary'
import type { Trabajo } from './types/trabajo'

type Vista = 'crear' | 'consultar'

function App() {
  const [vista, setVista] = useState<Vista>('crear')
  const [trabajo, setTrabajo] = useState<Trabajo | null>(null)
  const [consultaInicial, setConsultaInicial] = useState('')

  const mostrarConsulta = (id?: string) => {
    setConsultaInicial(id ?? '')
    setVista('consultar')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="app-shell">
      <main id="inicio">
        <section className="hero-section" aria-labelledby="page-title">
          <div className="hero-copy">
            <h1 id="page-title">Hogar de los Alpes</h1>
            <p>
              Registra una solicitud de servicio para el hogar o consulta el estado de un
              trabajo existente utilizando su identificador.
            </p>
          </div>
        </section>

        <section className="workspace" aria-label="Gestión de trabajos">
          <div className="workspace-main">
            <nav className="view-tabs" aria-label="Acciones disponibles">
              <button
                className={vista === 'crear' ? 'active' : ''}
                type="button"
                onClick={() => setVista('crear')}
                aria-current={vista === 'crear' ? 'page' : undefined}
              >
                <span>01</span> Crear solicitud
              </button>
              <button
                className={vista === 'consultar' ? 'active' : ''}
                type="button"
                onClick={() => mostrarConsulta()}
                aria-current={vista === 'consultar' ? 'page' : undefined}
              >
                <SearchIcon /> Consultar trabajo
              </button>
            </nav>

            <div className="panel-content">
              {vista === 'crear' ? (
                trabajo ? (
                  <TrabajoSummary
                    trabajo={trabajo}
                    onCreateAnother={() => setTrabajo(null)}
                    onLookup={() => mostrarConsulta(trabajo.id)}
                  />
                ) : (
                  <TrabajoForm onCreated={setTrabajo} />
                )
              ) : (
                <TrabajoLookup key={consultaInicial} initialId={consultaInicial} />
              )}
            </div>
          </div>

        </section>
      </main>
    </div>
  )
}

export default App
