# DevJournal Firebase Configuration & Deployment

## Deploying Firestore Security Rules

To deploy the Firestore security rules to your Firebase project:

1. Install the Firebase CLI globally (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. Log in to Firebase:
   ```bash
   firebase login
   ```

3. Select your DevJournal Firebase project:
   ```bash
   firebase use --add
   ```
   *(Select the DevJournal Firebase project from the interactive list)*

4. Deploy the Firestore security rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

---

## Admin Role Custom Claim

The Firestore security rules enforce `request.auth.token.role == "admin"` for creating and managing articles.

In Firebase Authentication, custom claims cannot be set directly from client apps (Android/Web). You must use the Firebase Admin SDK to set custom claims on the admin user account:

1. Download a Service Account private key JSON from Firebase Console:
   - **Project Settings** > **Service accounts** > **Generate new private key**.
   - Save the key file (e.g., `serviceAccountKey.json`).
   - Set the environment variable: `GOOGLE_APPLICATION_CREDENTIALS="path/to/serviceAccountKey.json"`.

2. Run the admin assignment script with the admin user's UID:
   ```bash
   node scripts/set-admin-role.js <uid>
   ```

3. Once set, the user must refresh their token (or sign out and sign back in) to receive the updated `role: "admin"` claim.
