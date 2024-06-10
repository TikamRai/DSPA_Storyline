package com.example.storyline.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import androidx.navigation.compose.*
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.*
import kotlinx.coroutines.*

class SearchedUserProfile : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        setContent {
            val navController = rememberNavController()

            val userId = intent.getStringExtra("userId")
            val sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
            val loggedInUserId = sharedPreferences.getString("loggedInUserId", null)

            userId?.let {
                SearchedUserProfileContent(context = this, searchedUserId = it, loggedInUserId = loggedInUserId, navController = navController, firestore = firestore)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchedUserProfileContent(
    context: Context,
    searchedUserId: String,
    loggedInUserId: String?,
    navController: NavHostController,
    firestore: FirebaseFirestore
) {
    var userName by remember { mutableStateOf("Loading...") }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchedUserId) {
        firestore.collection("users").document(searchedUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userName = document.getString("name") ?: "No name"
                    profilePictureUrl = document.getString("profilePictureUrl")

                    val followersList = document.get("followersList") as? Map<String, Boolean>
                    followersCount = followersList?.size ?: 0

                    val followingList = document.get("followingList") as? Map<String, Boolean>
                    followingCount = followingList?.size ?: 0

                    isFollowing = loggedInUserId != null && followingList?.containsKey(loggedInUserId) == true
                } else {

                    userName = "No name"
                    profilePictureUrl = null
                    followersCount = 0
                    followingCount = 0
                    isFollowing = false
                }
            }
            .addOnFailureListener { exception ->
                scope.launch {
                    Toast.makeText(
                        context,
                        "Failed to fetch user data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, SearchActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(74.dp))

            profilePictureUrl?.let { url ->
                Image(
                    painter = rememberImagePainter(url),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .clickable { /* Handle click if needed */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = userName, fontSize = 24.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isFollowing = !isFollowing
                    val currentUserRef = firestore.collection("users").document(searchedUserId)
                    val loggedInUserRef = firestore.collection("users").document(loggedInUserId ?: "")

                    if (isFollowing) {
                        loggedInUserRef.update("followingList.$searchedUserId", true)
                            .addOnSuccessListener {
                                followersCount++
                                Toast.makeText(
                                    context,
                                    "Now following",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to update following list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        currentUserRef.update("followersList.$loggedInUserId", true)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Follower added",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to update followers list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        loggedInUserRef.update("followingList.$searchedUserId", null)
                            .addOnSuccessListener {
                                followersCount--
                                Toast.makeText(
                                    context,
                                    "Unfollowed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to update following list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        currentUserRef.update("followersList.$loggedInUserId", null)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Follower removed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to update followers list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                    Column() {
                        TextButton(
                            onClick = { navController.navigate("followers") },
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                text = "$followersCount Follower",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column() {
                        TextButton(
                            onClick = { navController.navigate("following") },
                            modifier = Modifier
                                .padding(2.dp)
                                .align(Alignment.CenterHorizontally),
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
