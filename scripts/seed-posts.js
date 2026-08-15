const { initializeApp, applicationDefault, getApps } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

// Initialize Firebase Admin SDK
if (!getApps().length) {
  try {
    initializeApp({
      credential: applicationDefault()
    });
  } catch (error) {
    console.error('Error initializing Firebase Admin SDK:', error.message);
    console.error('Please ensure GOOGLE_APPLICATION_CREDENTIALS is set or serviceAccountKey.json is configured.');
    process.exit(1);
  }
}

const targetUid = process.argv[2];

if (!targetUid) {
  console.error('Usage: node scripts/seed-posts.js <uid>');
  process.exit(1);
}

const db = getFirestore();

const samplePosts = [
  {
    title: "Understanding Jetpack Compose State",
    slug: "understanding-jetpack-compose-state",
    excerpt: "A deep dive into managing state in Jetpack Compose to build reactive UIs effortlessly.",
    content: `Managing state in Jetpack Compose is fundamentally different from the old View system.

Instead of manually updating UI widgets, you simply define the state, and Compose takes care of re-rendering the UI when that state changes. 

Here is a quick example of using \`remember\` and \`mutableStateOf\`:

\`\`\`kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
\`\`\`

By wrapping our value in \`mutableStateOf\`, Compose knows to observe this variable and trigger a recomposition whenever it updates.`,
    tags: ["Compose", "Android", "Kotlin"],
    readTimeMinutes: 4,
    coverImageUrl: "https://images.unsplash.com/photo-1618477388954-7852f32655ec?auto=format&fit=crop&w=1200&q=80"
  },
  {
    title: "Clean Architecture in Android",
    slug: "clean-architecture-in-android",
    excerpt: "How to decouple your business logic from the Android framework using Clean Architecture.",
    content: `Clean Architecture is a software design philosophy that separates the elements of a design into ring levels.

The main rule of Clean Architecture is the **Dependency Rule**, which dictates that source code dependencies can only point inwards. 

### Layers
1. **Domain**: Contains business rules and models.
2. **Data**: Implements repositories and handles network/DB.
3. **Presentation**: The UI and ViewModels.

\`\`\`kotlin
// Domain Layer Example
interface PostRepository {
    suspend fun getPosts(): Result<List<Post>>
}
\`\`\`

By adhering to this, your core business logic remains independent of any specific framework like Android.`,
    tags: ["Architecture", "Android", "Tips"],
    readTimeMinutes: 5,
    coverImageUrl: "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80"
  },
  {
    title: "Mastering Kotlin Coroutines",
    slug: "mastering-kotlin-coroutines",
    excerpt: "Learn how to write asynchronous, non-blocking code gracefully using Kotlin Coroutines.",
    content: `Coroutines simplify asynchronous programming by replacing callbacks with sequential code.

They are lightweight threads that can be suspended and resumed without blocking the underlying OS thread.

\`\`\`kotlin
viewModelScope.launch {
    try {
        val data = repository.fetchData() // Suspends here
        _uiState.value = UiState.Success(data) // Resumes here
    } catch(e: Exception) {
        _uiState.value = UiState.Error(e.message)
    }
}
\`\`\`

Always remember to use the appropriate \`CoroutineDispatcher\` for your tasks (e.g., \`Dispatchers.IO\` for network requests).`,
    tags: ["Kotlin", "Android", "Tips"],
    readTimeMinutes: 6,
    coverImageUrl: "https://images.unsplash.com/photo-1542831371-29b0f74f9713?auto=format&fit=crop&w=1200&q=80"
  },
  {
    title: "5 Tips for Better UI Performance",
    slug: "5-tips-for-better-ui-performance",
    excerpt: "Quick and actionable tips to ensure your Android app runs smoothly at 60fps.",
    content: `Achieving buttery-smooth UI performance is critical for user retention. Here are some top tips:

1. **Avoid unnecessary allocations** inside loops or \`onDraw\`.
2. **Use RecyclerView** (or LazyColumn in Compose) for lists.
3. **Profile your app** using the Android Studio Profiler.

If using Compose, be mindful of recompositions:

\`\`\`kotlin
// Use key to prevent unnecessary recompositions in lists
LazyColumn {
    items(items = myList, key = { it.id }) { item ->
        MyListItem(item)
    }
}
\`\`\`

Keeping these in mind will make your app feel blazing fast!`,
    tags: ["Android", "Tips", "Compose"],
    readTimeMinutes: 3,
    coverImageUrl: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80"
  },
  {
    title: "Migrating from Java to Kotlin",
    slug: "migrating-from-java-to-kotlin",
    excerpt: "A practical guide to smoothly transitioning your existing Java codebase to Kotlin.",
    content: `Kotlin is the preferred language for Android development, offering null safety, conciseness, and coroutines.

When migrating, you don't have to rewrite everything at once because Kotlin is 100% interoperable with Java.

### Key differences:
- **Null safety**: \`String?\` vs \`String\`
- **Data classes**: Replaces boilerplate POJOs.

\`\`\`kotlin
// Java POJO
public class User {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

// Kotlin Data Class
data class User(var name: String)
\`\`\`

Start by writing all new features in Kotlin, and slowly convert existing Java files using the built-in Android Studio converter.`,
    tags: ["Kotlin", "Tips", "Architecture"],
    readTimeMinutes: 5,
    coverImageUrl: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=1200&q=80"
  }
];

async function seedPosts() {
  console.log('Seeding posts for admin UID:', targetUid);
  
  const batch = db.batch();
  const postsRef = db.collection('posts');

  samplePosts.forEach((postData) => {
    const docRef = postsRef.doc();
    const serverTime = FieldValue.serverTimestamp();
    
    batch.set(docRef, {
      ...postData,
      published: true,
      authorId: targetUid,
      createdAt: serverTime,
      updatedAt: serverTime,
      likeCount: 0,
      commentCount: 0
    });
  });

  try {
    await batch.commit();
    console.log(`Successfully seeded ${samplePosts.length} posts!`);
    process.exit(0);
  } catch (error) {
    console.error('Error seeding posts:', error);
    process.exit(1);
  }
}

seedPosts();
