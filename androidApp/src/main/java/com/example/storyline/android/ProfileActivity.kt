package com.example.storyline.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userEmail = user.email ?: "No email"
        val loggedInUserId = user.uid

        firestore.collection("users").document(user.uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(
                    this,
                    "Failed to listen for user data changes: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userName = snapshot.getString("name") ?: "Anonymous"
                val profilePictureUrl = snapshot.getString("profilePictureUrl")
                val followers = snapshot.get("followers") as? List<String> ?: emptyList()
                val following = snapshot.get("following") as? List<String> ?: emptyList()
                setContent {
                    navController = rememberNavController()
                    Theme {
                        NavHost(navController = navController, startDestination = "profile") {
                            composable("profile") {
                                ProfileScreen(
                                    loggedInUserId = loggedInUserId,
                                    name = userName,
                                    email = userEmail,
                                    profilePictureUrl = profilePictureUrl,
                                    followersCount = followers.size,
                                    followingCount = following.size,
                                    onProfilePictureClick = { pickImage() },
                                    onFollowersClick = { navController.navigate("followers") },
                                    onFollowingClick = { navController.navigate("following") },
                                    onReadingListClick = { navController.navigate("reading_list/$loggedInUserId") },
                                    onLogoutClick = { logout() },
                                    navController = navController
                                )
                            }
                            composable("followers") {
                                FollowersScreen(navController, followers, loggedInUserId)
                            }
                            composable("following") {
                                FollowingScreen(navController, following, loggedInUserId)
                            }
                            composable(
                                "userProfile/{userId}/{loggedInUserId}",
                                arguments = listOf(navArgument("userId") {
                                    type = NavType.StringType
                                }, navArgument("loggedInUserId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId")
                                    ?: return@composable
                                val loggedInUserId =
                                    backStackEntry.arguments?.getString("loggedInUserId")
                                        ?: return@composable
                                UserProfileScreen(navController, userId, loggedInUserId)
                            }
                            composable(
                                "reading_list/{loggedInUserId}",
                                arguments = listOf(navArgument("loggedInUserId") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val loggedInUserId =
                                    backStackEntry.arguments?.getString("loggedInUserId")
                                        ?: return@composable
                                ReadingListScreen(
                                    navController = navController,
                                    userId = loggedInUserId
                                )
                            }
                            composable(
                                "reading_list_story_detail/{storyId}/{userId}",
                                arguments = listOf(navArgument("storyId") {
                                    type = NavType.StringType
                                },
                                    navArgument("userId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val storyId = backStackEntry.arguments?.getString("storyId")
                                backStackEntry.arguments?.getString("userId")
                                ReadingListStoryDetailScreen(navController, storyId)
                            }
                            composable(
                                "published_stories/{userId}",
                                arguments = listOf(navArgument("userId") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val userId =
                                    backStackEntry.arguments?.getString("userId")
                                        ?: return@composable
                                PublishedStoriesScreen(
                                    navController = navController,
                                    userId = userId
                                )
                            }
                            composable(
                                "published_story_detail/{storyId}/{userId}",
                                arguments = listOf(navArgument("storyId") {
                                    type = NavType.StringType
                                },
                                    navArgument("userId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val storyId = backStackEntry.arguments?.getString("storyId")
                                val userId = backStackEntry.arguments?.getString("userId")
                                PublishedStoryDetailScreen(navController, storyId)
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
            } else {
                Toast.makeText(this, "Current user data: null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (::navController.isInitialized && navController.currentDestination?.route == "profile") {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            if (::navController.isInitialized) {
                navController.popBackStack()
            } else {
                super.onBackPressed()
            }
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImage(it) }
        }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImage(uri: Uri) {
        val user = auth.currentUser ?: return
        val storageRef = storage.reference.child("profile_pictures/${user.uid}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener { _ ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateProfilePictureInFireStore(downloadUri)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun updateProfilePictureInFireStore(uri: Uri) {
        val user = auth.currentUser ?: return
        val userRef = firestore.collection("users").document(user.uid)
        userRef.update("profilePictureUrl", uri.toString())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to update profile picture: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    loggedInUserId: String,
    name: String,
    email: String,
    profilePictureUrl: String?,
    followersCount: Int,
    followingCount: Int,
    onProfilePictureClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onReadingListClick: () -> Unit,
    onLogoutClick: () -> Unit,
    navController: NavHostController
) {
    // State variables for followers and following count
    var currentFollowersCount by remember { mutableIntStateOf(followersCount) }
    var currentFollowingCount by remember { mutableIntStateOf(followingCount) }

    // Context and Firestore instance
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Real-time listener for user data
    LaunchedEffect(loggedInUserId) {
        firestore.collection("users").document(loggedInUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(
                        context,
                        "Failed to listen for user data changes: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    currentFollowersCount = (snapshot.get("followers") as? List<*>)?.size ?: 0
                    currentFollowingCount = (snapshot.get("following") as? List<*>)?.size ?: 0
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "profile",
                navController = navController,
                context = LocalContext.current
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (profilePictureUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(profilePictureUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .clickable {
                            onProfilePictureClick()
                        }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.applogo),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .clickable {
                            onProfilePictureClick()
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = email, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.140f)
                    .background(Color(0xFFBBDFBA)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 50.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        TextButton(
                            onClick = onFollowersClick,
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$currentFollowersCount Follower",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column {
                        TextButton(
                            onClick = onFollowingClick,
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$currentFollowingCount Following",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.navigate("published_stories/$loggedInUserId") },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.20f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8BBF8C),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text(
                    text = "Published Stories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onReadingListClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8BBF8C),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text(
                    text = "Reading List",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF2DAAFF),
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Transparent
                )
            ) {
                Text(
                    text = "Logout",
                    color = Color.Red,
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String, navController: NavHostController, context: Context) {
    NavigationBar(
        modifier = Modifier
            .fillMaxHeight(0.07f),
        containerColor = Color(0xFF8BBF8C),
        contentColor = Color.Black
    ) {
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = "Home") },
            selected = currentRoute == "home",
            onClick = {
                val intent = Intent(context, HomeActivity::class.java)
                context.startActivity(intent)
            }
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
            onClick = { }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    navController: NavHostController,
    followers: List<String>,
    loggedInUserId: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Followers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            LazyColumn {
                items(followers) { followerId ->
                    var followerName by remember { mutableStateOf("Loading...") }
                    var followerProfilePictureUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(followerId) {
                        FirebaseFirestore.getInstance().collection("users").document(followerId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    followerName = document.getString("name") ?: "No name"
                                    followerProfilePictureUrl =
                                        document.getString("profilePictureUrl")
                                }
                            }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                navController.navigate("userProfile/$followerId/$loggedInUserId")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        followerProfilePictureUrl?.let { url ->
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                        } ?: Image(
                            painter = painterResource(id = R.drawable.applogo),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = followerName)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    navController: NavHostController,
    following: List<String>,
    loggedInUserId: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Following") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            LazyColumn {
                items(following) { followingId ->
                    var followingName by remember { mutableStateOf("Loading...") }
                    var followingProfilePictureUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(followingId) {
                        FirebaseFirestore.getInstance().collection("users").document(followingId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    followingName = document.getString("name") ?: "No name"
                                    followingProfilePictureUrl =
                                        document.getString("profilePictureUrl")
                                }
                            }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                navController.navigate("userProfile/$followingId/$loggedInUserId")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        followingProfilePictureUrl?.let { url ->
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                        } ?: Image(
                            painter = painterResource(id = R.drawable.applogo),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = followingName)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavHostController, userId: String, loggedInUserId: String) {
    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firestore = FirebaseFirestore.getInstance()

    fun fetchUserData() {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userName = document.getString("name") ?: "No name"
                    userEmail = document.getString("email") ?: "No email"
                    profilePictureUrl = document.getString("profilePictureUrl")
                    followersCount = (document.get("followers") as? List<*>)?.size ?: 0
                    followingCount = (document.get("following") as? List<*>)?.size ?: 0

                    if (loggedInUserId.isNotEmpty()) {
                        val followingList = document.get("followers") as? List<*>
                        isFollowing = followingList?.contains(loggedInUserId) ?: false
                    }
                }
            }
            .addOnFailureListener { exception ->
                scope.launch {}
                Toast.makeText(
                    context,
                    "Failed to fetch user data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    LaunchedEffect(userId) {
        fetchUserData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            profilePictureUrl?.let { url ->
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                )
            } ?: Image(
                painter = painterResource(id = R.drawable.applogo),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = userEmail, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isFollowing = !isFollowing
                    updateFollowStatus(userId, loggedInUserId, isFollowing) {
                        fetchUserData()
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = if (isFollowing) "Unfollow" else "Follow")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.140f)
                    .background(Color(0xFFBBDFBA)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 50.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        TextButton(
                            onClick = { },
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$followersCount Follower",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column {
                        TextButton(
                            onClick = { },
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$followingCount Following",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun updateFollowStatus(
    userId: String,
    loggedInUserId: String,
    isFollowing: Boolean,
    onComplete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val userRef = firestore.collection("users").document(userId)
    val loggedInUserRef = firestore.collection("users").document(loggedInUserId)

    firestore.runBatch { batch ->
        if (isFollowing) {
            batch.update(loggedInUserRef, "following", FieldValue.arrayUnion(userId))
            batch.update(userRef, "followers", FieldValue.arrayUnion(loggedInUserId))
        } else {
            batch.update(loggedInUserRef, "following", FieldValue.arrayRemove(userId))
            batch.update(userRef, "followers", FieldValue.arrayRemove(loggedInUserId))
        }
    }.addOnSuccessListener {
        onComplete()
    }.addOnFailureListener { exception ->
        Log.e("Firestore", "Error updating follow status: $exception")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishedStoriesScreen(navController: NavHostController, userId: String) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var publishedStories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "published")
            .get()
            .addOnSuccessListener { storyDocuments ->
                val stories = storyDocuments.mapNotNull { doc ->
                    try {
                        Story(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            imageUrl = doc.getString("coverImageUrl") ?: "",
                            authorId = doc.getString("userId") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                                ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error parsing story document", e)
                        null
                    }
                }
                publishedStories = stories
                isLoading = false
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting stories: ", exception)
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Published Stories") },
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
        } else if (publishedStories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No published stories", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                items(publishedStories) { story ->
                    StoryItem(story = story, onClick = {
                        navController.navigate("published_story_detail/${story.id}/$userId")
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishedStoryDetailScreen(navController: NavHostController, storyId: String?) {
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
                title = { Text("Story Detail") },
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
                        Text(
                            text = "See The Content",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(navController: NavHostController, userId: String) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var readingList by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val storyIds = document.get("readingList") as? List<String> ?: emptyList()
                if (storyIds.isNotEmpty()) {
                    firestore.collection("stories").whereIn(FieldPath.documentId(), storyIds).get()
                        .addOnSuccessListener { storyDocuments ->
                            val stories = storyDocuments.mapNotNull { doc ->
                                try {
                                    Story(
                                        id = doc.id,
                                        title = doc.getString("title") ?: "",
                                        imageUrl = doc.getString("coverImageUrl") ?: "",
                                        authorId = doc.getString("userId") ?: "",
                                        createdAt = doc.getTimestamp("createdAt")
                                            ?: com.google.firebase.Timestamp.now()
                                    )
                                } catch (e: Exception) {
                                    Log.e("Firestore", "Error parsing story document", e)
                                    null
                                }
                            }
                            readingList = stories
                            isLoading = false
                        }
                        .addOnFailureListener { exception ->
                            Log.w("Firestore", "Error getting stories: ", exception)
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting reading list: ", exception)
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading List") },
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
        } else if (readingList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stories in your reading list",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                items(readingList) { story ->
                    StoryItem(story = story, onClick = {
                        navController.navigate("reading_list_story_detail/${story.id}/$userId")
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListStoryDetailScreen(
    navController: NavHostController,
    storyId: String?
) {
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
                title = { Text("Story Detail") },
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
                        Text(
                            text = "Continue Reading",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

