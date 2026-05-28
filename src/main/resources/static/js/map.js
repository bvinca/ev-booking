// Renders the station map using Leaflet + OpenStreetMap tiles.
// Pulls station data from the REST API so the same backend powers both
// the server-rendered list and the dynamic map.
(function () {
    const mapEl = document.getElementById('stationMap');
    if (!mapEl) return;

    const map = L.map('stationMap');
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(map);

    const params = new URLSearchParams(window.location.search);
    const apiUrl = '/api/v1/stations?' + params.toString();

    fetch(apiUrl, { headers: { 'Accept': 'application/json' } })
        .then(r => r.json())
        .then(stations => {
            if (!stations.length) {
                map.setView([52.5, -1.5], 6); // UK fallback
                return;
            }
            const group = L.featureGroup();
            stations.forEach(s => {
                const m = L.marker([s.latitude, s.longitude]).bindPopup(
                    `<strong>${escapeHtml(s.name)}</strong><br/>` +
                    `${escapeHtml(s.address)}<br/>` +
                    `<a href="/stations/${s.id}">View details</a>`
                );
                group.addLayer(m);
            });
            group.addTo(map);
            map.fitBounds(group.getBounds().pad(0.2));
        })
        .catch(err => {
            console.error('Failed to load stations', err);
            map.setView([52.5, -1.5], 6);
        });

    function escapeHtml(s) {
        return String(s ?? '').replace(/[&<>"']/g, c => ({
            '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
        })[c]);
    }
})();
