const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');
if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function check() {
    const s = await db.collectionGroup('rooms').get();
    s.docs.forEach(d => {
        const uid = d.ref.path.split('/')[1];
        const r = d.data();
        if (r.roomName === 'Phòng 10' || r.roomName === 'Phong 11' || r.roomName === 'Phong 12') {
            console.log(`roomName: ${r.roomName}, roomId: ${r.roomId}, UID: ${uid}`);
        }
    });
}
check();
