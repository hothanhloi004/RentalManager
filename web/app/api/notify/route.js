import { NextResponse } from 'next/server';
import admin from '../../../lib/firebase-admin';

export async function POST(req) {
    try {
        const body = await req.json();
        const { uid, title, message } = body;

        if (!uid || !title || !message) {
            return NextResponse.json({ error: 'Missing required fields' }, { status: 400 });
        }

        const userDoc = await admin.firestore().collection('users').doc(uid).get();
        if (!userDoc.exists) {
            return NextResponse.json({ error: 'User not found' }, { status: 404 });
        }

        const userData = userDoc.data();
        const fcmToken = userData.fcmToken;

        if (!fcmToken) {
            return NextResponse.json({ error: 'User has no FCM Token registered' }, { status: 404 });
        }

        const payload = {
            notification: {
                title: title,
                body: message,
            },
            token: fcmToken
        };

        const response = await admin.messaging().send(payload);

        return NextResponse.json({ success: true, messageId: response });
    } catch (error) {
        console.error('Error sending push notification:', error);
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
