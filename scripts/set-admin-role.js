/**
 * Script: set-admin-role.js
 * Description: Sets the custom user claim `role="admin"` on a Firebase Authentication user.
 * 
 * Usage:
 *   1. Download your service account key from Firebase Console (Project Settings > Service accounts).
 *   2. Set GOOGLE_APPLICATION_CREDENTIALS="path/to/serviceAccountKey.json" (or provide serviceAccount.json in scripts/).
 *   3. Run:
 *        node scripts/set-admin-role.js <uid>
 * 
 * Note: Must be run once manually for the first admin account.
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  try {
    admin.initializeApp({
      credential: admin.credential.applicationDefault()
    });
  } catch (error) {
    console.error('Error initializing Firebase Admin SDK:', error.message);
    console.error('Please ensure GOOGLE_APPLICATION_CREDENTIALS is set or serviceAccountKey.json is configured.');
    process.exit(1);
  }
}

const targetUid = process.argv[2];

if (!targetUid) {
  console.error('Usage: node scripts/set-admin-role.js <uid>');
  process.exit(1);
}

async function setAdminRole(uid) {
  try {
    // Set custom user claims on the user
    await admin.auth().setCustomUserClaims(uid, { role: 'admin' });
    console.log(`Successfully assigned role="admin" to user: ${uid}`);

    // Update Firestore UserProfile document role field as well
    const userDocRef = admin.firestore().collection('users').document(uid);
    await userDocRef.set({ role: 'admin' }, { merge: true });
    console.log(`Updated Firestore users/${uid} document with role="admin"`);

    console.log('Done! Note: The user may need to sign out and sign back in for claims to take effect.');
    process.exit(0);
  } catch (error) {
    console.error('Failed to set admin role:', error);
    process.exit(1);
  }
}

setAdminRole(targetUid);
