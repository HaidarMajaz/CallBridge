const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: process.env.PORT || 8080 });

const rooms = {}; // roomCode -> { master: ws, slaves: [ws] }

wss.on('connection', ws => {
    ws.room = null;
    ws.role = null;

    ws.on('message', data => {
        try {
            const msg = JSON.parse(data);
            switch (msg.type) {
                case 'join':
                    ws.room = msg.room;
                    ws.role = msg.role;
                    if (!rooms[msg.room]) rooms[msg.room] = { master: null, slaves: [] };
                    if (msg.role === 'master') {
                        rooms[msg.room].master = ws;
                    } else {
                        rooms[msg.room].slaves.push(ws);
                        // Notify master a slave joined
                        const master = rooms[msg.room].master;
                        if (master && master.readyState === WebSocket.OPEN) {
                            master.send(JSON.stringify({ type: 'slave_joined' }));
                        }
                    }
                    break;
                case 'offer':
                case 'answer':
                case 'ice':
                    relay(ws, msg);
                    break;
            }
        } catch (e) {}
    });

    ws.on('close', () => {
        if (!ws.room || !rooms[ws.room]) return;
        if (ws.role === 'master') rooms[ws.room].master = null;
        else rooms[ws.room].slaves = rooms[ws.room].slaves.filter(s => s !== ws);
    });
});

function relay(sender, msg) {
    const room = rooms[sender.room];
    if (!room) return;
    if (sender.role === 'master') {
        room.slaves.forEach(s => { if (s.readyState === WebSocket.OPEN) s.send(JSON.stringify(msg)); });
    } else {
        const master = room.master;
        if (master && master.readyState === WebSocket.OPEN) master.send(JSON.stringify({ ...msg, from: 'slave' }));
    }
}

console.log('Signaling server running on port', process.env.PORT || 8080);
