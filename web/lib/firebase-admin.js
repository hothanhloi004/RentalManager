import admin from 'firebase-admin';

if (!admin.apps.length) {
    try {
        let credential;
        // Prefer env variable (used by production/admin pages)
        if (process.env.FIREBASE_SERVICE_ACCOUNT_KEY) {
            const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_KEY);
            credential = admin.credential.cert(serviceAccount);
        } else {
            // Fallback to local file for development
            const serviceAccount = require('../service-account.json');
            credential = admin.credential.cert(serviceAccount);
        }

        admin.initializeApp({
            credential,
            storageBucket: 'rentalmanager-4803a.firebasestorage.app',
        });
    } catch (error) {
        console.error('Firebase admin initialization error', error.stack);
    }
}

export default admin;
