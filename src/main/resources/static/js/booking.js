// Booking page: loads availability for a connector/day and lets the user
// select a contiguous range of free slots, then submits the form-encoded
// booking POST. The hidden inputs are filled with the start and end of the
// selected range as ISO LocalDateTime values (UTC) — the server interprets
// these as UTC instants.
(function () {
    const slotsEl = document.getElementById('slots');
    if (!slotsEl) return;
    const bookForm = document.getElementById('bookForm');
    const startInput = document.getElementById('startTime');
    const endInput   = document.getElementById('endTime');
    const startLabel = document.getElementById('startLabel');
    const endLabel   = document.getElementById('endLabel');
    const clearBtn   = document.getElementById('clearBtn');

    let slots = [];
    let selectedIndices = []; // contiguous

    fetch(`/api/v1/connectors/${connectorId}/availability?date=${encodeURIComponent(date)}`, {
        headers: { 'Accept': 'application/json' }
    })
    .then(r => r.json())
    .then(data => {
        slots = data;
        render();
    })
    .catch(err => {
        slotsEl.innerHTML = '<div class="text-danger">Could not load availability.</div>';
        console.error(err);
    });

    function render() {
        slotsEl.innerHTML = '';
        slots.forEach((s, i) => {
            const div = document.createElement('div');
            div.className = 'slot ' + (s.past ? 'past' : (s.free ? 'free' : 'booked'));
            div.dataset.index = i;
            div.textContent = formatTime(s.start);
            if (s.free && !s.past) div.addEventListener('click', () => toggle(i));
            if (selectedIndices.includes(i)) div.classList.add('selected');
            slotsEl.appendChild(div);
        });
        updateForm();
    }

    function toggle(i) {
        if (selectedIndices.length === 0) {
            selectedIndices = [i];
        } else if (selectedIndices.includes(i)) {
            // Deselect — only allow shortening from either end to keep contiguity
            const min = Math.min(...selectedIndices);
            const max = Math.max(...selectedIndices);
            if (i === min) selectedIndices = selectedIndices.filter(x => x !== i);
            else if (i === max) selectedIndices = selectedIndices.filter(x => x !== i);
            else selectedIndices = [i];
        } else {
            const min = Math.min(...selectedIndices);
            const max = Math.max(...selectedIndices);
            if (i === max + 1 || i === min - 1) {
                // Extend, but only across free slots
                const lo = Math.min(min, i);
                const hi = Math.max(max, i);
                for (let k = lo; k <= hi; k++) {
                    if (!slots[k].free || slots[k].past) { selectedIndices = [i]; render(); return; }
                }
                selectedIndices = [];
                for (let k = lo; k <= hi; k++) selectedIndices.push(k);
            } else {
                selectedIndices = [i];
            }
        }
        render();
    }

    function updateForm() {
        if (selectedIndices.length === 0) {
            bookForm.classList.add('d-none');
            return;
        }
        const min = Math.min(...selectedIndices);
        const max = Math.max(...selectedIndices);
        const startIso = toLocalDateTimeUtc(slots[min].start);
        const endIso   = toLocalDateTimeUtc(slots[max].end);
        startInput.value = startIso;
        endInput.value   = endIso;
        startLabel.textContent = formatDateTime(slots[min].start);
        endLabel.textContent   = formatDateTime(slots[max].end);
        bookForm.classList.remove('d-none');
    }

    clearBtn.addEventListener('click', () => { selectedIndices = []; render(); });

    function formatTime(iso)     { const d = new Date(iso); return pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes()); }
    function formatDateTime(iso) { return new Date(iso).toUTCString(); }
    function toLocalDateTimeUtc(iso) {
        // Convert "2026-06-01T10:30:00Z" → "2026-06-01T10:30" (LocalDateTime in UTC)
        const d = new Date(iso);
        return d.getUTCFullYear() + '-' + pad(d.getUTCMonth()+1) + '-' + pad(d.getUTCDate())
             + 'T' + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes());
    }
    function pad(n) { return String(n).padStart(2, '0'); }
})();
