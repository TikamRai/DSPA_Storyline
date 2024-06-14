package com.example.storyline.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.filter

class HomeActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            navController = rememberNavController()
            Theme {
                NavHost(navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("story_detail/{storyId}") { backStackEntry ->
                        StoryDetailScreen(
                            navController,
                            backStackEntry.arguments?.getString("storyId")
                        )
                    }
                    composable("story_parts/{storyId}") { backStackEntry ->
                        StoryPartsScreen(
                            navController,
                            backStackEntry.arguments?.getString("storyId")
                        )
                    }
                    composable("read_story_part/{partId}") { backStackEntry ->
                        ReadStoryPartScreen(
                            navController,
                            backStackEntry.arguments?.getString("partId")
                        )
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::navController.isInitialized && navController.currentDestination?.route == "home") {
            finishAffinity()
        } else {
            if (::navController.isInitialized) {
                navController.popBackStack()
            } else {
                super.onBackPressed()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var lastVisibleStory by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isFetchingMore by remember { mutableStateOf(false) }
    val loadedStoryIds by remember { mutableStateOf<MutableSet<String>>(mutableSetOf()) }
    var followedUserIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    fun fetchStories() {
        if (followedUserIds.isEmpty()) {
            return
        }

        val query = firestore.collection("stories")
            .whereEqualTo("status", "published")
            .whereIn("userId", followedUserIds)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(10)

        if (lastVisibleStory != null) {
            query.startAfter(lastVisibleStory)
        }

        query.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    isFetchingMore = false
                    return@addOnSuccessListener
                }
                lastVisibleStory = documents.documents.lastOrNull()
                val storyList = documents.mapNotNull { document ->
                    try {
                        val userId = document.getString("userId") ?: return@mapNotNull null
                        if (loadedStoryIds.contains(document.id)) {
                            return@mapNotNull null
                        }
                        loadedStoryIds.add(document.id)
                        Story(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            imageUrl = document.getString("coverImageUrl") ?: "",
                            authorId = userId,
                            createdAt = document.getTimestamp("createdAt")
                                ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error parsing story document", e)
                        null
                    }
                }
                stories = stories + storyList
                isLoading = false
                isFetchingMore = false
                Log.d("Firestore", "Published stories loaded: $storyList")
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting published stories: ", exception)
                isLoading = false
                isFetchingMore = false
            }
    }

    fun fetchFollowedUserIds(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val following = document.get("following") as? List<String> ?: emptyList()
                followedUserIds = following
                if (following.isNotEmpty()) {
                    fetchStories()
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting followed user IDs: ", exception)
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        fetchFollowedUserIds(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        },
        bottomBar = {
            BottomNavigationBar(currentRoute = "home")
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (followedUserIds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No stories to read.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Follow someone to read their story.",
                            fontSize = 20.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.padding(16.dp),
                    state = rememberLazyListState().apply {
                        this.onBottomReached {
                            if (!isFetchingMore) {
                                isFetchingMore = true
                                fetchStories()
                            }
                        }
                    }
                ) {
                    items(stories) { story ->
                        StoryItem(story = story, onClick = {
                            navController.navigate("story_detail/${story.id}")
                        })
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (isFetchingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LazyListState.onBottomReached(
    loadMore: () -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= layoutInfo.totalItemsCount - 5 // Adjust this threshold as needed
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .filter { it }
            .collect {
                loadMore()
            }
    }
}

@Composable
fun StoryItem(story: Story, onClick: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    var authorName by remember { mutableStateOf("Unknown") }

    LaunchedEffect(story.authorId) {
        firestore.collection("users").document(story.authorId).get()
            .addOnSuccessListener { userDoc ->
                authorName = userDoc.getString("name") ?: "Unknown"
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error retrieving user details", e)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = story.imageUrl),
            contentDescription = story.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = story.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = authorName,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(navController: NavHostController, storyId: String?) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var storyDetail by remember { mutableStateOf<StoryDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(storyId) {
        if (storyId != null) {
            firestore.collection("stories").document(storyId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userId = document.getString("userId") ?: return@addOnSuccessListener
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val authorName = userDoc.getString("name") ?: "Unknown"
                                storyDetail = StoryDetail(
                                    id = document.id,
                                    title = document.getString("title") ?: "",
                                    imageUrl = document.getString("coverImageUrl") ?: "",
                                    author = authorName,
                                    createdAt = document.getTimestamp("createdAt")
                                        ?: com.google.firebase.Timestamp.now(),
                                    category = document.getString("category") ?: "",
                                    description = document.getString("description") ?: ""
                                )
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error retrieving story details", e)
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Story Time") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            storyDetail?.let { story ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = story.imageUrl),
                        contentDescription = story.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = story.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        text = story.author,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        text = story.category,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        text = "Description",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(4.dp)
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = story.description,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            navController.navigate("story_parts/${story.id}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BBF8C))
                    ) {
                        Text(text = "Start Reading", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryPartsScreen(navController: NavHostController, storyId: String?) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var storyParts by remember { mutableStateOf<List<StoryParts>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(storyId) {
        if (storyId != null) {
            firestore.collection("story_parts")
                .whereEqualTo("storyId", storyId)
                .orderBy("writtenAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val partsList = documents.mapNotNull { document ->
                        try {
                            StoryParts(
                                id = document.id,
                                title = document.getString("title") ?: "",
                                content = document.getString("content") ?: "",
                                writtenAt = document.getTimestamp("writtenAt")
                                    ?: com.google.firebase.Timestamp.now()
                            )
                        } catch (e: Exception) {
                            Log.e("Firestore", "Error parsing story part document", e)
                            null
                        }
                    }
                    storyParts = partsList
                    isLoading = false
                    Log.d("Firestore", "Story parts loaded: $partsList")
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting story parts: ", exception)
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Story Parts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.padding(16.dp)
            ) {
                items(storyParts) { parts ->
                    StoryPartItem(parts = parts, onClick = {
                        navController.navigate("read_story_part/${parts.id}")
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StoryPartItem(parts: StoryParts, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = parts.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = parts.content.take(100) + "...",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadStoryPartScreen(navController: NavHostController, partId: String?) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var storyPart by remember { mutableStateOf<StoryPart?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(partId) {
        if (partId != null) {
            firestore.collection("story_parts").document(partId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        storyPart = StoryPart(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            content = document.getString("content") ?: "",
                            writtenAt = document.getTimestamp("writtenAt")
                                ?: com.google.firebase.Timestamp.now()
                        )
                        isLoading = false
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error retrieving story part details", e)
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    storyPart?.let {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = it.title,
                                modifier = Modifier.align(Alignment.TopStart),
                                color = Color.Black,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            storyPart?.let { part ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = part.content,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String) {
    val context = LocalContext.current

    NavigationBar(
        modifier = Modifier
            .fillMaxHeight(0.07f),
        containerColor = Color(0xFF8BBF8C),
        contentColor = Color.Black
    ) {
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = "Home") },
            selected = currentRoute == "home",
            onClick = { }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search"
                )
            },
            selected = currentRoute == "search",
            onClick = {
                val intent = Intent(context, SearchActivity::class.java)
                context.startActivity(intent)
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_create),
                    contentDescription = "Create"
                )
            },
            selected = currentRoute == "create",
            onClick = {
                val intent = Intent(context, CreateStoryActivity::class.java)
                context.startActivity(intent)
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_profile),
                    contentDescription = "Profile"
                )
            },
            selected = currentRoute == "profile",
            onClick = {
                val intent = Intent(context, ProfileActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
}

data class Story(
    val id: String,
    val title: String,
    val authorId: String,
    val imageUrl: String,
    val createdAt: com.google.firebase.Timestamp
)

data class StoryDetail(
    val id: String,
    val title: String,
    val author: String,
    val imageUrl: String,
    val createdAt: com.google.firebase.Timestamp,
    val category: String,
    val description: String
)

data class StoryParts(
    val id: String,
    val title: String,
    val content: String,
    val writtenAt: com.google.firebase.Timestamp
)

data class StoryPart(
    val id: String,
    val title: String,
    val content: String,
    val writtenAt: com.google.firebase.Timestamp
)
