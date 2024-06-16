package com.example.storyline.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateStoryActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var navController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setContent {
            val navController = rememberNavController()
            Theme {
                NavHost(navController, startDestination = "create_screen") {
                    composable("create_screen") {
                        CreateScreen(navController, auth, firestore)
                    }
                    composable("creation_screen") {
                        CreationScreen(navController, auth, firestore, storage)
                    }
                    composable(
                        "story_editor_screen/{storyId}",
                        arguments = listOf(navArgument("storyId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val storyId = backStackEntry.arguments?.getString("storyId")
                        StoryEditorScreen(navController, storyId, firestore)
                    }
                    composable("published_list_screen") {
                        PublishedListScreen(navController, auth, firestore)
                    }
                    composable("edit_story_screen/{storyId}/{isDraft}") { backStackEntry ->
                        val storyId = backStackEntry.arguments?.getString("storyId")
                        val isDraft =
                            backStackEntry.arguments?.getString("isDraft")?.toBoolean() ?: false
                        EditStoryScreen(navController, storyId, firestore, isDraft, auth)
                    }
                    composable(
                        "story_part_editor_screen/{draftPartId}",
                        arguments = listOf(navArgument("draftPartId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val draftPartId = backStackEntry.arguments?.getString("draftPartId")
                        StoryPartEditorScreen(navController, draftPartId, firestore)
                    }
                }
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (::navController.isInitialized && navController.currentDestination?.route == "create_screen") {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    navController: NavHostController,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    val currentUser = auth.currentUser?.uid
    val drafts = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d("Firestore", "Current user ID: $currentUser")
            firestore.collection("stories")
                .whereEqualTo("userId", currentUser)
                .whereEqualTo("status", "draft")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("Firestore", "Documents retrieved: ${documents.size()}")
                    if (documents.isEmpty) {
                        Log.d("Firestore", "No drafts found for the user")
                    } else {
                        val draftList = documents.map { document ->
                            document.data.apply { put("id", document.id) }
                        }
                        drafts.value = draftList
                        Log.d("Firestore", "Drafts loaded successfully: $draftList")
                    }
                    isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting drafts: ", exception)
                    isLoading.value = false
                }
        } else {
            Log.w("Firestore", "No current user logged in")
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Story") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        },
        bottomBar = {
            BottomNavigationBar(currentRoute = "create", context = LocalContext.current)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = { navController.navigate("creation_screen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD3D3D3),
                    contentColor = Color.Black
                )
            ) {
                Text(text = "Write a New Story", fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = { navController.navigate("published_list_screen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD3D3D3),
                    contentColor = Color.Black
                )
            ) {
                Text(text = "Edit Published Stories", fontSize = 18.sp)
            }

            Text(
                text = "Drafts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
            )

            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                if (drafts.value.isEmpty()) {
                    Text(
                        text = "No drafts available",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.99f)
                    ) {
                        items(drafts.value) { draft ->
                            OutlinedButton(
                                onClick = { navController.navigate("edit_story_screen/${draft["id"]}/true") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD3D3D3),
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(text = draft["title"] as String, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String, context: Context) {
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
            onClick = { }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationScreen(
    navController: NavHostController,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) {
    val currentUser = auth.currentUser

    var storyCoverUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> storyCoverUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Story") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.LightGray)
                        .fillMaxWidth(1f)
                        .height(120.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    storyCoverUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(uri),
                            contentDescription = "Story Cover Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } ?: run {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 50.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add Story Cover")
                            }
                        }
                    }
                }

                var title by remember { mutableStateOf(TextFieldValue()) }
                var description by remember { mutableStateOf(TextFieldValue()) }
                var tags by remember { mutableStateOf(TextFieldValue()) }
                var category by remember { mutableStateOf(TextFieldValue()) }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Story Title",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                )

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .align(Alignment.CenterHorizontally),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(
                    text = "Description",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .align(Alignment.CenterHorizontally)
                        .height(150.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(
                    text = "Tags",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                )

                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .align(Alignment.CenterHorizontally),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(
                    text = "Story Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                )

                TextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .align(Alignment.CenterHorizontally),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Button to upload story and navigate to editor screen
                OutlinedButton(
                    onClick = {
                        storyCoverUri?.let { uri ->
                            val storageRef =
                                storage.reference.child("story_covers/${currentUser?.uid}_${System.currentTimeMillis()}.jpg")
                            val uploadTask = storageRef.putFile(uri)
                            uploadTask.addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val story = hashMapOf(
                                        "title" to title.text,
                                        "description" to description.text,
                                        "tags" to tags.text,
                                        "category" to category.text,
                                        "coverImageUrl" to downloadUri.toString(),
                                        "userId" to (currentUser?.uid ?: ""),
                                        "status" to "draft",
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "modifiedAt" to com.google.firebase.Timestamp.now()
                                    )
                                    firestore.collection("stories")
                                        .add(story)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d(
                                                "Firestore",
                                                "Story created with ID: ${documentReference.id}"
                                            )
                                            navController.navigate("story_editor_screen/${documentReference.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("Firestore", "Error creating the story", e)
                                        }
                                }
                            }.addOnFailureListener { e ->
                                Log.w("FirebaseStorage", "Error uploading image", e)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Text(
                        text = "Start Writing",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    navController: NavHostController,
    storyId: String?,
    firestore: FirebaseFirestore
) {
    var storyPartTitle by remember { mutableStateOf(TextFieldValue()) }
    var storyContent by remember { mutableStateOf(TextFieldValue()) }
    val storyPartWordLimit = 5000

    val wordCount = storyContent.text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    val remainingWords = storyPartWordLimit - wordCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize()
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = storyPartTitle,
                    onValueChange = { storyPartTitle = it },
                    placeholder = {
                        Text(
                            text = "Title your Story Part",
                            color = Color.LightGray
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White
                    )
                )

                HorizontalDivider(thickness = 1.dp, color = Color.Gray)

                Text(
                    text = "Word count: $wordCount/$storyPartWordLimit",
                    modifier = Modifier.align(Alignment.End),
                    color = if (remainingWords < 0) Color.Red else Color.Gray
                )
                Text(
                    text = "Words remaining: $remainingWords",
                    modifier = Modifier.align(Alignment.End),
                    color = if (remainingWords < 0) Color.Red else Color.Gray
                )

                TextField(
                    value = storyContent,
                    onValueChange = { it ->
                        if (it.text.split("\\s+".toRegex())
                                .filter { it.isNotEmpty() }.size <= storyPartWordLimit
                        ) {
                            storyContent = it
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Tap here to start writing",
                            color = Color.LightGray
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val storyPart = hashMapOf(
                                "title" to storyPartTitle.text,
                                "content" to storyContent.text,
                                "storyId" to storyId,
                                "writtenAt" to com.google.firebase.Timestamp.now()
                            )
                            firestore.collection("story_parts")
                                .add(storyPart)
                                .addOnSuccessListener { documentReference ->
                                    Log.d(
                                        "Firestore",
                                        "Story part saved with ID: ${documentReference.id}"
                                    )
                                    storyId?.let {
                                        val modifiedAt = com.google.firebase.Timestamp.now()
                                        firestore.collection("stories").document(it)
                                            .update("modifiedAt", modifiedAt)
                                    }
                                    navController.navigate("create_screen") {
                                        popUpTo("create_screen") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error saving story part", e)
                                }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (storyId != null) {
                                val storyPart = hashMapOf(
                                    "title" to storyPartTitle.text,
                                    "content" to storyContent.text,
                                    "storyId" to storyId,
                                    "writtenAt" to com.google.firebase.Timestamp.now()
                                )
                                firestore.collection("story_parts")
                                    .add(storyPart)
                                    .addOnSuccessListener { documentReference ->
                                        Log.d(
                                            "Firestore",
                                            "Story part published with ID: ${documentReference.id}"
                                        )
                                        val publishedAt = com.google.firebase.Timestamp.now()
                                        val modifiedAt = com.google.firebase.Timestamp.now()
                                        firestore.collection("stories").document(storyId)
                                            .update(
                                                mapOf(
                                                    "status" to "published",
                                                    "publishedAt" to publishedAt,
                                                    "modifiedAt" to modifiedAt
                                                )
                                            )
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Story published")
                                                navController.navigate("create_screen") {
                                                    popUpTo("create_screen") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("Firestore", "Error publishing story", e)
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error publishing story part", e)
                                    }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Publish",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStoryScreen(
    navController: NavHostController,
    storyId: String?,
    firestore: FirebaseFirestore,
    isDraft: Boolean,
    auth: FirebaseAuth
) {
    val currentUser = auth.currentUser?.uid
    var storyCoverUri by remember { mutableStateOf<Uri?>(null) }
    var oldCoverImageUrl by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var tags by remember { mutableStateOf(TextFieldValue()) }
    var category by remember { mutableStateOf(TextFieldValue()) }
    var createdAt by remember { mutableStateOf<com.google.firebase.Timestamp?>(null) }
    var draftParts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> storyCoverUri = uri }
    )

    LaunchedEffect(storyId) {
        storyId?.let {
            firestore.collection("stories").document(it).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        title = TextFieldValue(document.getString("title") ?: "")
                        description = TextFieldValue(document.getString("description") ?: "")
                        tags = TextFieldValue(document.getString("tags") ?: "")
                        category = TextFieldValue(document.getString("category") ?: "")
                        oldCoverImageUrl = document.getString("coverImageUrl")
                        storyCoverUri = oldCoverImageUrl?.let { Uri.parse(it) }
                        createdAt = document.getTimestamp("createdAt")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error retrieving story details", e)
                }

            firestore.collection("story_parts")
                .whereEqualTo("storyId", it)
                .orderBy("writtenAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val partsList =
                        documents.map { document -> document.data.apply { put("id", document.id) } }
                    draftParts = partsList
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error retrieving story parts", e)
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDraft) "Edit Draft" else "Edit Story") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.LightGray)
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    storyCoverUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Text("Change Story Cover")
                }

                Text(text = "Story Title", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(text = "Story Description", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(text = "Tags", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(text = "Story Category", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(
                    text = "Draft Parts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight(0.54f)
                ) {
                    items(draftParts.size) { index ->
                        val draftPart = draftParts[index]
                        OutlinedButton(
                            onClick = { navController.navigate("story_part_editor_screen/${draftPart["id"]}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(top = 2.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD3D3D3),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = draftPart["title"] as String, fontSize = 16.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val newPart = hashMapOf(
                            "title" to "",
                            "content" to "",
                            "storyId" to storyId,
                            "writtenAt" to com.google.firebase.Timestamp.now()
                        )
                        firestore.collection("story_parts")
                            .add(newPart)
                            .addOnSuccessListener { documentReference ->
                                firestore.collection("stories").document(storyId ?: "")
                                    .update("modifiedAt", com.google.firebase.Timestamp.now())
                                    .addOnSuccessListener {
                                        navController.navigate("story_part_editor_screen/${documentReference.id}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding new part", e)
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = "+ Add New Part", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val finalCoverImageUrl = storyCoverUri?.toString() ?: oldCoverImageUrl
                            val story = hashMapOf(
                                "title" to title.text,
                                "description" to description.text,
                                "tags" to tags.text,
                                "category" to category.text,
                                "coverImageUrl" to finalCoverImageUrl,
                                "status" to "published",
                                "publishedAt" to com.google.firebase.Timestamp.now(),
                                "modifiedAt" to com.google.firebase.Timestamp.now(),
                                "createdAt" to createdAt,  // Use the original createdAt
                                "userId" to (currentUser ?: "")  // Ensure userId is added
                            )
                            firestore.collection("stories").document(storyId ?: "")
                                .set(story)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Story published with ID: $storyId")
                                    navController.navigate("create_screen") {
                                        popUpTo("create_screen") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error publishing story", e)
                                }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Publish",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            storyId?.let { id ->
                                firestore.collection("stories").document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        firestore.collection("story_parts")
                                            .whereEqualTo("storyId", id)
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                for (document in documents) {
                                                    firestore.collection("story_parts")
                                                        .document(document.id)
                                                        .delete()
                                                }
                                                navController.navigate("create_screen") {
                                                    popUpTo("create_screen") { inclusive = true }
                                                }
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error deleting story", e)
                                    }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Delete",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryPartEditorScreen(
    navController: NavHostController,
    draftPartId: String?,
    firestore: FirebaseFirestore
) {
    var draftPartTitle by remember { mutableStateOf(TextFieldValue()) }
    var draftPartContent by remember { mutableStateOf(TextFieldValue()) }
    var isTitleAvailable by remember { mutableStateOf(false) }
    var isContentAvailable by remember { mutableStateOf(false) }
    var storyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draftPartId) {
        draftPartId?.let {
            firestore.collection("story_parts").document(it).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        val title = document.getString("title") ?: ""
                        val content = document.getString("content") ?: ""
                        draftPartTitle = TextFieldValue(title)
                        draftPartContent = TextFieldValue(content)
                        isTitleAvailable = title.isNotEmpty()
                        isContentAvailable = content.isNotEmpty()
                        storyId = document.getString("storyId")
                    }
                }
        }
    }

    val partWordLimit = 5000
    val wordsCount = draftPartContent.text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    val remainingWords = partWordLimit - wordsCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize()
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = draftPartTitle,
                    onValueChange = { draftPartTitle = it },
                    placeholder = { if (!isTitleAvailable) Text("Title your Story Part") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White
                    )
                )

                HorizontalDivider(thickness = 1.dp, color = Color.Gray)

                Text(
                    text = "Word count: $wordsCount/$partWordLimit",
                    modifier = Modifier.align(Alignment.End),
                    color = if (wordsCount > partWordLimit) Color.Red else Color.Gray
                )

                Text(
                    text = "Remaining words: $remainingWords",
                    modifier = Modifier.align(Alignment.End),
                    color = if (remainingWords < 0) Color.Red else Color.Gray
                )

                TextField(
                    value = draftPartContent,
                    onValueChange = {
                        val newWordsCount =
                            it.text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                        if (newWordsCount <= partWordLimit) {
                            draftPartContent = it
                        }
                    },
                    placeholder = { if (!isContentAvailable) Text("Tap here to start writing") },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val draftPart = hashMapOf(
                                "title" to draftPartTitle.text,
                                "content" to draftPartContent.text,
                                "storyId" to storyId,
                                "writtenAt" to com.google.firebase.Timestamp.now()
                            )
                            firestore.collection("story_parts").document(draftPartId ?: "")
                                .set(draftPart)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Draft part saved with ID: $draftPartId")
                                    storyId?.let { id ->
                                        firestore.collection("stories").document(id)
                                            .update(
                                                "modifiedAt",
                                                com.google.firebase.Timestamp.now()
                                            )
                                    }
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error saving draft part", e)
                                }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            draftPartId?.let { id ->
                                firestore.collection("story_parts").document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d(
                                            "Firestore",
                                            "Draft part deleted with ID: $draftPartId"
                                        )
                                        storyId?.let { id ->
                                            firestore.collection("stories").document(id)
                                                .update(
                                                    "modifiedAt",
                                                    com.google.firebase.Timestamp.now()
                                                )
                                        }
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error deleting draft part", e)
                                    }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Delete",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishedListScreen(
    navController: NavHostController,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    val currentUser = auth.currentUser?.uid
    val publishedStories = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("stories")
                .whereEqualTo("userId", currentUser)
                .whereEqualTo("status", "published")
                .get()
                .addOnSuccessListener { documents ->
                    val storyList = documents.map { document ->
                        document.data.apply { put("id", document.id) }
                    }
                    publishedStories.value = storyList
                    isLoading.value = false
                    Log.d("Firestore", "Published stories loaded: $storyList")
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting published stories: ", exception)
                    isLoading.value = false
                }
        } else {
            Log.w("Firestore", "No current user logged in")
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Published Stories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else {
                    if (publishedStories.value.isEmpty()) {
                        Text(
                            text = "No published stories available",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(publishedStories.value) { story ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable { navController.navigate("edit_story_screen/${story["id"]}/false") }
                                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = rememberAsyncImagePainter(story["coverImageUrl"] as String),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(120.dp)
                                                .background(
                                                    Color.LightGray,
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = story["title"] as String,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}


