package com.example.storyline.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.filter

class HomeActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Theme {
                NavHost(navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("story_detail/{storyId}") { backStackEntry ->
                        StoryDetailScreen(navController, backStackEntry.arguments?.getString("storyId"))
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
    var loadedStoryIds by remember { mutableStateOf<MutableSet<String>>(mutableSetOf()) }

    fun fetchStories() {
        val query = firestore.collection("stories")
            .whereEqualTo("status", "published")
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
                            createdAt = document.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now()
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

    LaunchedEffect(Unit) {
        fetchStories()
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
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
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
                                    createdAt = document.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now(),
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
                        text = "Category: ${story.category}",
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
                        onClick = { /* Handle Read button click */ },
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
