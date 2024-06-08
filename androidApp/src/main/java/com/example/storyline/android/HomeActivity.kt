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
import androidx.compose.foundation.lazy.items
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
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme {
                HomeScreen()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("stories")
            .whereEqualTo("status", "published")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val storyList = documents.mapNotNull { document ->
                    try {
                        val userId = document.getString("userId") ?: return@mapNotNull null
                        val authorName = document.getString("author") ?: "Unknown"
                        Story(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            imageUrl = document.getString("coverImageUrl") ?: "",
                            author = authorName,
                            createdAt = document.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error parsing story document", e)
                        null
                    }
                }
                stories = storyList
                isLoading = false
                Log.d("Firestore", "Published stories loaded: $storyList")
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting published stories: ", exception)
                isLoading = false
            }
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
                modifier = Modifier.padding(16.dp)
            ) {
                items(stories) { story ->
                    StoryItem(story = story, onClick = {
                        //val intent = Intent(context, StoryDetailActivity::class.java)
                        //intent.putExtra("storyId", story.id)
                        //context.startActivity(intent)
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StoryItem(story: Story, onClick: () -> Unit) {
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
            modifier = Modifier
            .padding(start = 4.dp)
        )
        Text(
            text = story.author,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier
                .padding(start = 4.dp)
        )
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
    val author: String,
    val imageUrl: String,
    val createdAt: com.google.firebase.Timestamp
)