import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Appointment, cancelAppointment, createAppointment, getAppointments, getCurrentUser, getVapidPublicKey, googleLoginUrl, logout, savePushSubscription } from './api'

const monthNames = ['Janeiro', 'Fevereiro', 'Marco', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro']
const weekdays = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB']
const brandName = 'Instituto da Dor'

type User = { id: string; name: string; email: string }
type SessionState = 'checking' | 'authenticated' | 'unauthenticated'

function dateKey(date: Date) { return date.toISOString().slice(0, 10) }
function formatTime(value: string) { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
function duration(start: string, end: string) {
  const minutes = Math.max(0, (new Date(`1970-01-01T${end}`).getTime() - new Date(`1970-01-01T${start}`).getTime()) / 60000)
  return `${Math.floor(minutes / 60)}h${minutes % 60 ? String(minutes % 60).padStart(2, '0') : ''}`
}

function base64ToBytes(value: string) {
  const padding = '='.repeat((4 - value.length % 4) % 4)
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(base64), character => character.charCodeAt(0))
}

function waitForServiceWorker() {
  return Promise.race([
    navigator.serviceWorker.ready,
    new Promise<ServiceWorkerRegistration>((_, reject) => setTimeout(() => reject(new Error('O Service Worker ainda nao esta pronto. Recarregue o PWA e tente novamente.')), 10000))
  ])
}

function Brand({ compact = false }: { compact?: boolean }) {
  return <div className={`brand ${compact ? 'brand-compact' : ''}`}>
    <img src="/logo.svg" alt="" className="brand-logo" />
    <div><strong>{brandName}</strong>{!compact && <small>Agenda profissional</small>}</div>
  </div>
}

function LoginPage() {
  return <main className="login-page">
    <div className="login-art" aria-hidden="true"><div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" /><div className="art-dots" /><div className="art-glow" /></div>
    <section className="login-panel">
      <Brand compact />
      <div className="login-copy"><p className="eyebrow">AGENDA PROFISSIONAL</p><h1>Cuidado que começa com organização.</h1><p>Tenha seus atendimentos sempre à mão, com segurança e sincronização com o Google Calendar.</p></div>
      <a className="google-button" href={googleLoginUrl()}><span className="google-icon">G</span><span>Entrar com Google</span><span className="button-arrow">→</span></a>
      <p className="login-note">Acesso exclusivo para profissionais autorizados do {brandName}.</p>
      <div className="login-footer"><span className="secure-dot" /> Login protegido pelo Google <span>•</span> Seus dados ficam seguros</div>
    </section>
  </main>
}

function LoadingPage() {
  return <main className="login-page loading-page"><img src="/logo.svg" alt="" className="loading-logo" /><span className="loader light-loader" /><p>Preparando sua agenda...</p></main>
}

function AuthGate() {
  const [state, setState] = useState<SessionState>('checking')
  const [user, setUser] = useState<User | null>(null)

  useEffect(() => {
    getCurrentUser().then(currentUser => { setUser(currentUser); setState('authenticated') }).catch(() => setState('unauthenticated'))
  }, [])

  if (state === 'checking') return <LoadingPage />
  if (!user) return <LoginPage />
  return <Agenda user={user} />
}

function Agenda({ user }: { user: User }) {
  const [month, setMonth] = useState(new Date(2026, 8, 1))
  const [selectedDate, setSelectedDate] = useState(new Date(2026, 8, 28))
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [appointmentToCancel, setAppointmentToCancel] = useState<Appointment | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const [showProfileMenu, setShowProfileMenu] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [notificationsEnabled, setNotificationsEnabled] = useState(false)
  const [notificationsLoading, setNotificationsLoading] = useState(false)
  const [notificationMessage, setNotificationMessage] = useState('')
  const [form, setForm] = useState({ patientName: '', date: '2026-09-28', start: '09:00', end: '10:00' })

  const calendarDays = useMemo(() => {
    const first = new Date(month.getFullYear(), month.getMonth(), 1)
    const start = new Date(first)
    start.setDate(first.getDate() - first.getDay())
    return Array.from({ length: 42 }, (_, index) => { const date = new Date(start); date.setDate(start.getDate() + index); return date })
  }, [month])
  const appointmentsByDay = useMemo(() => appointments.reduce<Record<string, Appointment[]>>((result, item) => { const key = dateKey(new Date(item.startAt)); (result[key] ??= []).push(item); return result }, {}), [appointments])
  const selectedAppointments = appointmentsByDay[dateKey(selectedDate)] ?? []

  useEffect(() => {
    let cancelled = false
    const start = new Date(month.getFullYear(), month.getMonth(), 1)
    const end = new Date(month.getFullYear(), month.getMonth() + 1, 1)
    setLoading(true)
    getAppointments(start, end).then(items => { if (!cancelled) { setAppointments(items); setError('') } }).catch(requestError => { if (!cancelled) setError(requestError instanceof Error ? requestError.message : 'Nao foi possivel carregar sua agenda.') }).finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [month])

  function openForm() { setForm(value => ({ ...value, date: dateKey(selectedDate) })); setError(''); setShowForm(true) }
  function shiftMonth(value: number) { setMonth(current => new Date(current.getFullYear(), current.getMonth() + value, 1)) }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!form.patientName.trim() || form.end <= form.start) { setError('Informe o paciente e um horario final posterior ao inicial.'); return }
    const startAt = new Date(`${form.date}T${form.start}:00`).toISOString()
    const endAt = new Date(`${form.date}T${form.end}:00`).toISOString()
    try {
      const appointment = await createAppointment({ patientName: form.patientName.trim(), startAt, endAt })
      setAppointments(current => [...current, appointment]); setSelectedDate(new Date(`${form.date}T12:00:00`)); setShowForm(false); setError('')
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : 'Nao foi possivel agendar.') }
  }
  async function confirmCancellation() {
    if (!appointmentToCancel) return
    setCancelling(true)
    try {
      await cancelAppointment(appointmentToCancel.id)
      setAppointments(current => current.filter(item => item.id !== appointmentToCancel.id))
      setAppointmentToCancel(null)
      setError('')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Nao foi possivel cancelar o atendimento.')
    } finally {
      setCancelling(false)
    }
  }
  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
      window.location.reload()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Nao foi possivel sair da conta.')
      setLoggingOut(false)
    }
  }
  async function activateNotifications() {
    setNotificationsLoading(true)
    setNotificationMessage('')
    try {
      if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) throw new Error('Seu dispositivo nao oferece notificacoes Push para este PWA.')
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') throw new Error('Permissao de notificacoes nao concedida.')
      const { publicKey } = await getVapidPublicKey()
      if (!publicKey) throw new Error('As notificacoes ainda nao foram configuradas no servidor.')
      const serviceWorkerPath = import.meta.env.PROD ? '/sw.js' : '/push-sw.js'
      await navigator.serviceWorker.register(serviceWorkerPath, { scope: '/', type: import.meta.env.PROD ? 'module' : 'classic' })
      const registration = await waitForServiceWorker()
      const subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: base64ToBytes(publicKey) })
      await savePushSubscription(subscription.toJSON() as { endpoint: string; keys: { p256dh?: string; auth?: string } })
      setNotificationsEnabled(true)
      setNotificationMessage('Notificacoes ativadas neste dispositivo.')
    } catch (requestError) {
      setNotificationMessage(requestError instanceof Error ? requestError.message : 'Nao foi possivel ativar as notificacoes.')
    } finally {
      setNotificationsLoading(false)
    }
  }

  return <main className="app-shell">
    <header className="topbar"><Brand /><div className="top-actions"><span className="connection online"><i /> Conectado</span><div className="profile-wrap"><button className="profile" aria-label={`Abrir perfil de ${user.name}`} aria-expanded={showProfileMenu} onClick={() => setShowProfileMenu(value => !value)}>{user.name.slice(0, 2).toUpperCase()}</button>{showProfileMenu && <div className="profile-menu"><div className="profile-info"><strong>{user.name}</strong><span>{user.email}</span></div><button className={`notification-menu-button ${notificationsEnabled ? 'enabled' : ''}`} onClick={activateNotifications} disabled={notificationsLoading}><span className="notification-menu-icon">{notificationsEnabled ? '✓' : '◌'}</span>{notificationsEnabled ? 'Notificações ativas' : notificationsLoading ? 'Ativando...' : 'Ativar notificações'}</button><button className="logout-button" onClick={handleLogout} disabled={loggingOut}>{loggingOut ? 'Saindo...' : 'Sair da conta'}</button></div>}</div></div></header>
    <section className="welcome"><div><p className="eyebrow">{new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(selectedDate).toUpperCase()}</p><h1>Bom dia, {user.name.split(' ')[0]}.</h1><p className="muted">Organize seus atendimentos com tranquilidade.</p></div><button className="primary" onClick={openForm}>+ Novo atendimento</button></section>
    {notificationMessage && <div className="notification-feedback">{notificationMessage}</div>}
    {error && !showForm && <div className="page-error">{error}</div>}
    <section className="content-grid"><div className="calendar-card"><div className="calendar-heading"><div><p className="eyebrow">SUA AGENDA</p><h2>{monthNames[month.getMonth()]} <span>{month.getFullYear()}</span></h2></div><div className="month-actions"><button onClick={() => shiftMonth(-1)} aria-label="Mes anterior">‹</button><button onClick={() => shiftMonth(1)} aria-label="Mes seguinte">›</button></div></div><div className="weekdays">{weekdays.map(day => <span key={day}>{day}</span>)}</div><div className="days">{calendarDays.map(day => { const key = dateKey(day); const inMonth = day.getMonth() === month.getMonth(); return <button key={key} className={`${inMonth ? '' : 'muted-day '}${key === dateKey(selectedDate) ? 'selected ' : ''}${key === dateKey(new Date()) ? 'today' : ''}`} onClick={() => inMonth && setSelectedDate(day)}><span>{day.getDate()}</span>{appointmentsByDay[key] && <i />}</button> })}</div><div className="calendar-footer"><span><i className="legend-dot" /> Atendimento agendado</span><button className="text-button" onClick={() => setSelectedDate(new Date())}>Hoje</button></div></div>
      <aside className="day-card"><div className="day-heading"><div><p className="eyebrow">ATENDIMENTOS DO DIA</p><h2>{selectedDate.getDate()} <span>{monthNames[selectedDate.getMonth()].slice(0, 3).toUpperCase()}</span></h2></div><button className="round-add" onClick={openForm}>+</button></div>{loading ? <div className="empty"><span className="loader" /><p>Carregando agenda...</p></div> : selectedAppointments.length ? <div className="appointment-list">{selectedAppointments.map(item => <article className="appointment" key={item.id}><div className="time">{formatTime(item.startAt)} - {formatTime(item.endAt)}</div><div><strong>{item.patientName}</strong><span>Atendimento</span></div><button className="more" onClick={() => setAppointmentToCancel(item)} aria-label={`Cancelar atendimento de ${item.patientName}`}>•••</button></article>)}</div> : <div className="empty"><span>✦</span><p>Nenhum atendimento<br />para este dia.</p><button className="text-button" onClick={openForm}>Agendar agora</button></div>}</aside>
    </section>
    <footer className="footer"><span>{brandName}</span><span>Sincronizado com sua conta Google</span></footer>
    {showForm && <div className="modal-backdrop" onClick={() => setShowForm(false)}><form className="modal" onClick={event => event.stopPropagation()} onSubmit={submit}><div className="modal-header"><div><p className="eyebrow">NOVO REGISTRO</p><h2>Novo atendimento</h2></div><button type="button" className="close" onClick={() => setShowForm(false)}>×</button></div><label>Nome do paciente<input required value={form.patientName} onChange={event => setForm({ ...form, patientName: event.target.value })} placeholder="Ex.: Joao da Silva" /></label><label>Data<input required type="date" value={form.date} onChange={event => setForm({ ...form, date: event.target.value })} /></label><div className="form-row"><label>Inicio<input required type="time" value={form.start} onChange={event => setForm({ ...form, start: event.target.value })} /></label><label>Termino<input required type="time" value={form.end} onChange={event => setForm({ ...form, end: event.target.value })} /></label></div>{error && <p className="form-error">{error}</p>}<div className="duration">Duracao calculada <strong>{form.end > form.start ? duration(form.start, form.end) : 'Periodo invalido'}</strong></div><button className="primary full" type="submit">Agendar atendimento</button></form></div>}
    {appointmentToCancel && <div className="modal-backdrop" onClick={() => !cancelling && setAppointmentToCancel(null)}><div className="modal confirmation-modal" onClick={event => event.stopPropagation()}><div className="confirmation-icon">!</div><p className="eyebrow">CANCELAR ATENDIMENTO</p><h2>Deseja cancelar este horario?</h2><p className="confirmation-copy"><strong>{appointmentToCancel.patientName}</strong><br />{formatTime(appointmentToCancel.startAt)} - {formatTime(appointmentToCancel.endAt)}</p><div className="confirmation-actions"><button className="secondary" disabled={cancelling} onClick={() => setAppointmentToCancel(null)}>Manter agendamento</button><button className="danger" disabled={cancelling} onClick={confirmCancellation}>{cancelling ? 'Cancelando...' : 'Cancelar atendimento'}</button></div></div></div>}
  </main>
}

export default function App() { return <AuthGate /> }
