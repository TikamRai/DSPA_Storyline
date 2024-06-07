package com.example.storyline.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userEmail = user.email ?: "No email"

        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val userName = document.getString("name") ?: "Anonymous"
                    val profilePictureUrl = document.getString("profilePictureUrl")
                    val followers = document.get("followers") as? List<String> ?: emptyList()
                    val following = document.get("following") as? List<String> ?: emptyList()
                    setContent {
                        val navController = rememberNavController()
                        Theme {
                            NavHost(navController = navController, startDestination = "profile") {
                                composable("profile") {
                                    ProfileScreen(
                                        name = userName,
                                        email = userEmail,
                                        profilePictureUrl = profilePictureUrl,
                                        followersCount = followers.size,
                                        followingCount = following.size,
                                        onProfilePictureClick = { pickImage() },
                                        onFollowersClick = { navController.navigate("followers") },
                                        onFollowingClick = { navController.navigate("following") },
                                        onLogoutClick = { logout() },
                                        navController = navController
                                    )
                                }
                                composable("followers") {
                                    FollowersScreen(navController, followers)
                                }
                                composable("following") {
                                    FollowingScreen(navController, following)
                                }
                                composable("userProfile/{userId}") { backStackEntry ->
                                    val userId = backStackEntry.arguments?.getString("userId")
                                        ?: return@composable
                                    UserProfileScreen(navController, userId)
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to fetch user data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    email: String,
    profilePictureUrl: String?,
    followersCount: Int,
    followingCount: Int,
    onProfilePictureClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onLogoutClick: () -> Unit,
    navController: NavHostController
) {
    var name by remember { mutableStateOf(name) }
    var email by remember { mutableStateOf(email) }
    var profilePictureUrl by remember { mutableStateOf(profilePictureUrl) }
    var followers by remember { mutableStateOf(followersCount) }
    var following by remember { mutableStateOf(followingCount) }

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

            ElevatedButton(
                onClick = { /* Handle edit profile */ },
                modifier = Modifier
                    .fillMaxWidth(0.50f),
                colors = ButtonColors(
                    containerColor = Color(0xFFE6F0E1),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text(text = "Edit Profile")
            }
            Spacer(modifier = Modifier.height(24.dp))

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
                    Column() {
                        TextButton(
                            onClick = onFollowersClick,
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$followers Follower",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column() {
                        TextButton(
                            onClick = onFollowingClick,
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2DAAFF),
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "$following Following",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* Handle published stories */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.20f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonColors(
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
                onClick = { /* Handle reading list */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonColors(
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
                colors = ButtonColors(
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
fun FollowersScreen(navController: NavHostController, followers: List<String>) {
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
            followers.forEach { follower ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { navController.navigate("userProfile/$follower") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = follower,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { /* Handle unfollow logic here */ }) {
                        Text("Unfollow")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(navController: NavHostController, following: List<String>) {
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
            following.forEach { follow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { navController.navigate("userProfile/$follow") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = follow,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { /* Handle unfollow logic here */ }) {
                        Text("Unfollow")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavHostController, userId: String) {
    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userName = document.getString("name") ?: "No name"
                    userEmail = document.getString("email") ?: "No email"
                    profilePictureUrl = document.getString("profilePictureUrl")
                    followersCount = (document.get("followers") as? List<*>)?.size ?: 0
                    followingCount = (document.get("following") as? List<*>)?.size ?: 0
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
                        .size(150.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = userEmail, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "$followersCount Followers", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$followingCount Following", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
