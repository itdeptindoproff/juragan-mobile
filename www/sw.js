/**
 * Service Worker untuk Juragan Home (portal launcher).
 *
 * Minimal — hanya untuk memenuhi syarat installability PWA. Kita TIDAK cache
 * asset karena strategi update Frappe pakai filename-hashing (file lama auto
 * invalid). Biarkan browser handle semua request seperti biasa.
 */

const SW_VERSION = 'juragan-home-sw-v1'

self.addEventListener('install', (event) => {
	// Skip waiting supaya SW langsung aktif tanpa reload manual
	event.waitUntil(self.skipWaiting())
})

self.addEventListener('activate', (event) => {
	event.waitUntil(self.clients.claim())
})

// Fetch listener no-op — sebagian browser cek adanya listener untuk
// installability. Network passthrough (default behaviour).
self.addEventListener('fetch', () => {})
