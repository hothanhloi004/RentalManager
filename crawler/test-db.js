const { initializeApp } = require('firebase/app');
const { getFirestore, collectionGroup, query, getDocs } = require('firebase/firestore');

const firebaseConfig = {
    apiKey: "AIzaSyAiI9-QdPGTdINvL7mGdTBoyH95MqQaNUk",
    authDomain: "rentalmanager-4803a.firebaseapp.com",
    projectId: "rentalmanager-4803a",
    storageBucket: "rentalmanager-4803a.firebasestorage.app",
    messagingSenderId: "854957572297",
    appId: "1:854957572297:android:76aa9a1b4b9eb89f144d18"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function testQuery() {
    try {
        console.log("Testing collectionGroup('tenants')...");
        // Try getting just one to see if rules allow read
        const q = query(collectionGroup(db, 'tenants'));
        const snapshot = await getDocs(q);
        console.log(`Success! Found ${snapshot.size} tenants globally.`);
    } catch (e) {
        console.error("Error:", e.message);
    }
}

testQuery();
