const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');
if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function clearOldRooms() {
    console.log("Tìm các phòng không có SĐT để xóa...");
    const snapshot = await db.collectionGroup('rooms').get();
    let count = 0;

    // Lấy tất cả UIDs và settings
    const uniqueUids = [...new Set(snapshot.docs.map(d => d.ref.path.split('/')[1]))];
    const settingsMap = {};
    for (const uid of uniqueUids) {
        const sDoc = await db.collection('users').doc(uid).collection('settings').doc('config').get();
        if (sDoc.exists) settingsMap[uid] = sDoc.data();
    }

    const batch = db.batch();
    for (const doc of snapshot.docs) {
        const uid = doc.ref.path.split('/')[1];
        const s = settingsMap[uid] || {};
        if (!s.landlordPhone) {
            console.log(`Xóa phòng ma: ${doc.data().roomName} (UID: ${uid})`);
            batch.delete(doc.ref);
            count++;
        }
    }

    if (count > 0) {
        await batch.commit();
        console.log(`Đã xóa ${count} phòng cũ rác thành công.`);
    } else {
        console.log("Không tìm thấy phòng nào.");
    }
}
clearOldRooms().catch(console.error);
