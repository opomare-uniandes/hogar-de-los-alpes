import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const trabajoCreado = {
  id: 'a63f8965-6b70-4e2b-bf01-0636401287a2',
  clienteId: 'b3f5b1b0-1111-4a2a-9c1a-000000000001',
  categoriaServicio: 'plomeria',
  urgencia: 'MEDIA',
  ciudad: 'Bogotá',
  origen: 'MARKETPLACE',
  partnerId: null,
  moneda: 'COP',
  estado: 'CREADO',
  fechaCreacion: '2026-09-07T16:00:00Z',
}

describe('Portal de Hogar de los Alpes', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('presenta las acciones principales', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /hogar de los alpes/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear solicitud/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /consultar trabajo/i })).toBeInTheDocument()
  })

  it('solicita un aliado cuando el origen no es marketplace', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.selectOptions(screen.getByLabelText(/origen de la solicitud/i), 'SINIESTRO')

    expect(screen.getByLabelText(/identificador del aliado/i)).toBeInTheDocument()
  })

  it('muestra el identificador después de crear un trabajo', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => trabajoCreado,
    }))
    render(<App />)

    await user.selectOptions(screen.getByLabelText(/^servicio$/i), 'plomeria')
    await user.click(screen.getByRole('button', { name: /^crear trabajo/i }))

    expect(await screen.findByRole('heading', { name: /tu trabajo ya está en marcha/i })).toBeInTheDocument()
    expect(screen.getByText(trabajoCreado.id)).toBeInTheDocument()
  })
})
