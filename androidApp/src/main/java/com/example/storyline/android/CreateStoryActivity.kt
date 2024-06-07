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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.items
import coil.compose.rememberImagePainter

class CreateStoryActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
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
                        CreateScreen(navController, auth, firestore, storage)
                    }
                    composable("creation_screen") {
                        CreationScreen(navController, auth, firestore, storage)
                    }
                    composable(
                        "story_editor_screen/{storyId}",
                        arguments = listOf(navArgument("storyId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val storyId = backStackEntry.arguments?.getString("storyId")
                        TextEditorScreen(navController, storyId, firestore)
                    }
                    composable(
                        "edit_draft_screen/{draftId}",
                        arguments = listOf(navArgument("draftId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val draftId = backStackEntry.arguments?.getString("draftId")
                        EditDraftScreen(navController, draftId, firestore, storage)
                    }
                    composable(
                        "draft_part_editor_screen/{draftPartId}",
                        arguments = listOf(navArgument("draftPartId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val draftPartId = backStackEntry.arguments?.getString("draftPartId")
                        DraftPartEditorScreen(navController, draftPartId, firestore)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    navController: NavHostController,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) {
    val currentUser = auth.currentUser
    val drafts = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            firestore.collection("stories")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("status", "draft")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val draftList = documents.map { document ->
                        document.data.apply { put("id", document.id) }
                    }
                    drafts.value = draftList
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting drafts: ", exception)
                }
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
                onClick = { /* Navigate to Edit Published Stories page */ },
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

            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.99f)
            ) {
                items(drafts.value) { draft ->
                    OutlinedButton(
                        onClick = { navController.navigate("draft_editor_screen/${draft["id"]}") },
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

            if (drafts.value.isEmpty()) {
                Text(
                    text = "No drafts available",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
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
                                        "status" to "draft"
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
fun TextEditorScreen(
    navController: NavHostController,
    storyId: String?,
    firestore: FirebaseFirestore
) {
    var storyPartTitle by remember { mutableStateOf(TextFieldValue()) }
    var storyContent by remember { mutableStateOf(TextFieldValue()) }

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
                    .fillMaxSize(),
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

                TextField(
                    value = storyContent,
                    onValueChange = { storyContent = it },
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
                                "storyId" to storyId
                            )
                            firestore.collection("story_parts")
                                .add(storyPart)
                                .addOnSuccessListener { documentReference ->
                                    Log.d(
                                        "Firestore",
                                        "Draft saved with ID: ${documentReference.id}"
                                    )
                                    navController.navigate("create_screen") {
                                        popUpTo("create_screen") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error saving draft", e)
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
                            val storyPart = hashMapOf(
                                "title" to storyPartTitle.text,
                                "content" to storyContent.text,
                                "storyId" to storyId,
                            )
                            val story = hashMapOf(
                                "status" to "published"
                            )
                            firestore.collection("story_parts")
                                .add(storyPart)
                                .addOnSuccessListener { documentReference ->
                                    Log.d(
                                        "Firestore",
                                        "Story part published with ID: ${documentReference.id}"
                                    )
                                    navController.navigate("create_screen") {
                                        popUpTo("create_screen") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error publishing story part", e)
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
fun EditDraftScreen(
    navController: NavHostController,
    draftId: String?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) {
    var storyCoverUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var tags by remember { mutableStateOf(TextFieldValue()) }
    var category by remember { mutableStateOf(TextFieldValue()) }
    var draftParts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> storyCoverUri = uri }
    )

    LaunchedEffect(draftId) {
        draftId?.let {
            firestore.collection("stories").document(it).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        title = TextFieldValue(document.getString("title") ?: "")
                        description = TextFieldValue(document.getString("description") ?: "")
                        tags = TextFieldValue(document.getString("tags") ?: "")
                        category = TextFieldValue(document.getString("category") ?: "")
                        storyCoverUri = Uri.parse(document.getString("coverImageUrl"))
                    }
                }

            firestore.collection("story_parts")
                .whereEqualTo("storyId", it)
                .get()
                .addOnSuccessListener { documents ->
                    val partsList = documents.map { document -> document.data }
                    draftParts = partsList
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Story") },
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
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    storyCoverUri?.let {
                        Image(
                            painter = rememberImagePainter(it),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Text("Change Story Cover")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Story Title", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

                Text(text = "Story Description", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Text(text = "Tags", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

                Text(text = "Story Category", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Draft Parts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyColumn(modifier = Modifier.fillMaxHeight(0.99f)) {
                    items(draftParts.size) { index ->
                        val draftPart = draftParts[index]
                        OutlinedButton(
                            onClick = { navController.navigate("draft_editor_screen/${draftPart["id"]}") },
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
                            Text(text = draftPart["title"] as String, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val newPart = hashMapOf(
                            "title" to "",
                            "content" to "",
                            "storyId" to draftId
                        )
                        firestore.collection("story_parts")
                            .add(newPart)
                            .addOnSuccessListener { documentReference ->
                                navController.navigate("draft_editor_screen/${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding new part", e)
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .padding(6.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = "+ Add New Part", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        storyCoverUri?.let { uri ->
                            val storageRef =
                                storage.reference.child("story_covers/${draftId}_${System.currentTimeMillis()}.jpg")
                            val uploadTask = storageRef.putFile(uri)
                            uploadTask.addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val story = hashMapOf(
                                        "title" to title.text,
                                        "description" to description.text,
                                        "tags" to tags.text,
                                        "category" to category.text,
                                        "coverUrl" to downloadUri.toString(),
                                        "status" to "published"
                                    )
                                    firestore.collection("stories").document(draftId ?: "")
                                        .set(story)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "Story published with ID: $draftId")
                                            navController.navigate("create_screen") {
                                                popUpTo("create_screen") { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("Firestore", "Error publishing story", e)
                                        }
                                }
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
                        text = "Publish",
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
fun DraftPartEditorScreen(
    navController: NavHostController,
    draftPartId: String?,
    firestore: FirebaseFirestore
) {
    var draftPartTitle by remember { mutableStateOf(TextFieldValue()) }
    var draftPartContent by remember { mutableStateOf(TextFieldValue()) }
    var isTitleAvailable by remember { mutableStateOf(false) }
    var isContentAvailable by remember { mutableStateOf(false) }

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
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Draft Part") },
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
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Divider(color = Color.Gray, thickness = 1.dp)

                TextField(
                    value = draftPartContent,
                    onValueChange = { draftPartContent = it },
                    placeholder = { if (!isContentAvailable) Text("Tap here to start writing") },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray,
                        focusedContainerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                        focusedIndicatorColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val draftPart = hashMapOf(
                            "title" to draftPartTitle.text,
                            "content" to draftPartContent.text,
                            "storyId" to draftPartId
                        )
                        firestore.collection("story_parts").document(draftPartId ?: "")
                            .set(draftPart)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Draft part saved with ID: $draftPartId")
                                navController.popBackStack() // Navigate back to EditDraftScreen
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error saving draft part", e)
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(60.dp),
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
            }
        }
    )
}


