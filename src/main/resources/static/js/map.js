// Renders the station map using Leaflet + CartoDB Dark Matter tiles
// (dark theme companion to OpenStreetMap, free, no API key).
(function () {
    const mapEl = document.getElementById('stationMap');
    if (!mapEl) return;

    const map = L.map('stationMap');
    function refreshMapSize() {
        map.invalidateSize();
    }
    window.addEventListener('resize', refreshMapSize);
    if (window.ResizeObserver) {
        new ResizeObserver(refreshMapSize).observe(mapEl);
    }
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
        subdomains: 'abcd',
        maxZoom: 19
    }).addTo(map);

    // Custom electric-blue marker icon
    const brandIcon = L.divIcon({
        className: 'ev-marker',
        html: '<div style="width:26px;height:26px;border-radius:50%;background:#00AEEF;border:3px solid #0F172A;box-shadow:0 0 0 2px #00AEEF, 0 4px 12px rgba(0,174,239,0.6);display:grid;place-items:center;color:#0F172A;font-weight:900;font-size:13px;font-family:\'Plus Jakarta Sans\',sans-serif;">⚡</div>',
        iconSize: [26, 26],
        iconAnchor: [13, 13]
    });

    const params = new URLSearchParams(window.location.search);
    const apiUrl = '/api/v1/stations?' + params.toString();

    fetch(apiUrl, { headers: { 'Accept': 'application/json' } })
        .then(r => r.json())
        .then(stations => {
            if (!stations.length) {
                map.setView([40.6401, 22.9444], 12); // Thessaloniki fallback
                return;
            }
            const group = L.featureGroup();
            stations.forEach(s => {
                const m = L.marker([s.latitude, s.longitude], { icon: brandIcon }).bindPopup(
                    `<strong>${escapeHtml(s.name)}</strong><br/>` +
                    `${escapeHtml(s.address)}<br/>` +
                    `<a href="/stations/${s.id}">View details →</a>`
                );
                group.addLayer(m);
            });
            group.addTo(map);
            map.fitBounds(group.getBounds().pad(0.2));
            setTimeout(refreshMapSize, 0);
        })
        .catch(err => {
            console.error('Failed to load stations', err);
            map.setView([40.6401, 22.9444], 12);
        });

    function escapeHtml(s) {
        return String(s ?? '').replace(/[&<>"']/g, c => ({
            '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
        })[c]);
    }
})();
