# DevJournal Android

DevJournal is a native Android developer journal and blog client built with Kotlin and Jetpack Compose. It shares its Firebase backend with a companion web admin panel to provide a complete content management and consumption experience.

## Features

* Email and Google authentication
* Real time feed with tag filtering
* Markdown post detail with syntax highlighted code blocks
* Like and comment system
* Post creation and editing for authorized authors
* User profiles with follower and following counts
* Follow and unfollow between users
* Settings screen with account and notification preferences
* Push notifications via Firebase Cloud Messaging triggered through a Cloudflare Worker
* Account suspension enforcement

## Tech Stack

* Kotlin
* Jetpack Compose
* Material 3
* MVVM plus Clean Architecture
* Hilt for dependency injection
* Kotlin Coroutines and Flow
* Firebase Authentication
* Cloud Firestore
* Cloudinary for media storage
* Coil for image loading
* OkHttp for networking

## Architecture

```text
app/src/main/java/com/devjournal/
  data/
    model/
    repository/
    remote/
  domain/
    model/
    repository/
    usecase/
  presentation/
    auth/
    feed/
    postdetail/
    profile/
    settings/
```

The application follows Clean Architecture principles to separate the user interface from the business logic and data sources. This separation of concerns ensures that the codebase is scalable, testable, and maintainable. By isolating the domain layer, the core logic remains independent of the database or UI framework, allowing for easier updates and robust testing.

## Screenshots

1. ![Login](screenshots/login.png)
2. ![Feed](screenshots/feed.png)
3. ![Post Detail](screenshots/post_detail.png)
4. ![Editor](screenshots/editor.png)
5. ![Profile](screenshots/profile.png)

## Setup and Installation

1. Clone the repository.
2. Add your google services json file from the Firebase console.
3. Open the project in Android Studio or Antigravity.
4. Set the Cloudinary cloud name and Cloudflare Worker URL in the relevant configuration files.
5. Run the admin role script to designate the first admin.
6. Run the seed script for sample data.
7. Build and run the application.

## Firestore Schema

```json
{
  "users": {
    "uid": {
      "name": "string",
      "email": "string",
      "bio": "string",
      "avatarUrl": "string",
      "isAdmin": "boolean",
      "suspended": "boolean",
      "followersCount": "number",
      "followingCount": "number"
    }
  },
  "posts": {
    "postId": {
      "title": "string",
      "content": "string",
      "authorId": "string",
      "createdAt": "timestamp",
      "tags": ["string"],
      "published": "boolean"
    }
  },
  "comments": {
    "commentId": {
      "postId": "string",
      "authorId": "string",
      "content": "string",
      "createdAt": "timestamp"
    }
  }
}
```

## Related Project

This application is accompanied by the devjournal web admin panel repository. Both projects share the same Firebase project, allowing administrators to manage content from the web while users consume and interact with it on their Android devices.

## License

MIT License
