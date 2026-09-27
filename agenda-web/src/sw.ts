/// <reference lib="webworker" />

import { precacheAndRoute } from 'workbox-precaching'

declare const self: ServiceWorkerGlobalScope & { __WB_MANIFEST: Array<unknown> }

precacheAndRoute(self.__WB_MANIFEST)

self.addEventListener('push', event => {
  const data = event.data?.json() ?? { title: 'Instituto da Dor', body: 'Voce tem um novo lembrete.' }
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/pwa-192.svg',
    badge: '/pwa-192.svg',
    data: { url: data.url ?? '/' },
    tag: 'appointment-reminder'
  }))
})

self.addEventListener('notificationclick', event => {
  event.notification.close()
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
    const target = event.notification.data?.url ?? '/'
    const existing = clients.find(client => 'focus' in client)
    if (existing) return existing.focus()
    return self.clients.openWindow(target)
  }))
})
