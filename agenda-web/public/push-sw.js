/* global self */

self.addEventListener('push', function (event) {
  var data = event.data ? event.data.json() : { title: 'Instituto da Dor', body: 'Voce tem um novo lembrete.' }
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/pwa-192.svg',
    badge: '/pwa-192.svg',
    data: { url: data.url || '/' },
    tag: 'appointment-reminder'
  }))
})

self.addEventListener('notificationclick', function (event) {
  event.notification.close()
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (clients) {
    var target = event.notification.data && event.notification.data.url ? event.notification.data.url : '/'
    var existing = clients.find(function (client) { return 'focus' in client })
    if (existing) return existing.focus()
    return self.clients.openWindow(target)
  }))
})
