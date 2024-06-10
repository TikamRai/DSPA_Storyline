package com.example.storyline.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SearchedUserProfile : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setContent {
            val navController = rememberNavController()

            val loggedInUserId = auth.currentUser?.uid ?: ""
            val userId = intent.getStringExtra("userId")

            SearchedUserProfileContent(context = this, loggedInUserId = loggedInUserId, userId = userId ?: "", navController = navController, firestore = firestore)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchedUserProfileContent(
    context: Context,
    loggedInUserId: String,
    userId: String,
    navController: NavHostController,
    firestore: FirebaseFirestore
) {
    var userName by remember { mutableStateOf("Loading...") }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userName = document.getString("name") ?: "No name"
                    profilePictureUrl = document.getString("profilePictureUrl")
                    followersCount = (document.get("followers") as? List<*>)?.size ?: 0
                    followingCount = (document.get("following") as? List<*>)?.size ?: 0

                    if (loggedInUserId.isNotEmpty()) {
                        val followingList = document.get("followers") as? List<String>
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
                    updateFollowStatus(userId, loggedInUserId, isFollowing)
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

private fun updateFollowStatus(userId: String, loggedInUserId: String, isFollowing: Boolean) {
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
    }.addOnFailureListener { exception ->
        Log.e("Firestore", "Error updating follow status: $exception")
    }
}
