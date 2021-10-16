self.addEventListener("install", async () => {
    await self.skipWaiting();
});

self.addEventListener("activate", async () => {
    await clearCaches();
    await deleteDatabase();
    await self.registration.unregister();
    await reloadClients();
});

async function clearCaches() {
    const cacheKeys = await caches.keys();
    const mendixCacheKeys = cacheKeys.filter(key => key.startsWith("mendix-"));
    await Promise.all(mendixCacheKeys.map(key => caches.delete(key)));
}

async function deleteDatabase() {
    return new Promise((resolve, reject) => {
        const req = indexedDB.deleteDatabase("workbox-expiration");
        req.onsuccess = resolve;
        req.onerror = reject;
        req.onblocked = reject;
    });
}

async function reloadClients() {
    const clients = await self.clients.matchAll({ type: "window" });
    await Promise.all(clients.map(client => client.navigate(client.url)));
}
