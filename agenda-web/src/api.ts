export type Appointment = {
  id: string
  patientName: string
  startAt: string
  endAt: string
  googleEventId?: string | null
  status: 'SCHEDULED' | 'CANCELLED'
}

export type AppointmentInput = Pick<Appointment, 'patientName' | 'startAt' | 'endAt'>

const apiUrl = import.meta.env.VITE_API_URL ?? '/api'
const apiOrigin = new URL(apiUrl, window.location.origin).origin

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers)
  if (options?.body) headers.set('Content-Type', 'application/json')
  const response = await fetch(`${apiUrl}${path}`, {
    credentials: 'include',
    headers,
    ...options
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Nao foi possivel concluir a operacao.' }))
    throw new Error(error.message ?? 'Nao foi possivel concluir a operacao.')
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export function getAppointments(start: Date, end: Date) {
  const params = new URLSearchParams({ start: start.toISOString(), end: end.toISOString() })
  return request<Appointment[]>(`/appointments?${params}`)
}

export function createAppointment(input: AppointmentInput) {
  return request<Appointment>('/appointments', { method: 'POST', body: JSON.stringify(input) })
}

export function cancelAppointment(id: string) {
  return request<void>(`/appointments/${id}`, { method: 'DELETE' })
}

export function getCurrentUser() {
  return request<{ id: string; name: string; email: string }>('/auth/me')
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}

export function getVapidPublicKey() {
  return request<{ publicKey: string }>('/notifications/vapid-public-key')
}

export function savePushSubscription(subscription: { endpoint: string; keys: { p256dh?: string; auth?: string } }) {
  return request<void>('/notifications/subscriptions', {
    method: 'POST',
    body: JSON.stringify({ endpoint: subscription.endpoint, p256dh: subscription.keys.p256dh, auth: subscription.keys.auth })
  })
}

export function googleLoginUrl() {
  return `${apiOrigin}/oauth2/authorization/google`
}
