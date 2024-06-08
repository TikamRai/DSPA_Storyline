package com.example.storyline.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        setContent {
            val navController = rememberNavController()
            Theme {
                SearchApp(navController, firestore)
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
fun SearchApp(navController: NavHostController, firestore: FirebaseFirestore) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF8BBF8C))
            )
        },
        bottomBar = {
            BottomNavigationBar(currentRoute = "search", navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            SearchUserApp(navController, firestore)
        }
    }
}

@Composable
fun SearchUserApp(navController: NavHostController, firestore: FirebaseFirestore) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search Users",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.LightGray, CircleShape)
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions.Default.copy(autoCorrect = true),
                textStyle = TextStyle(fontSize = 16.sp),
                keyboardActions = KeyboardActions.Default
            )
            Button(
                onClick = {
                    searchUser(query, firestore, context) { user ->
                        searchResults = user
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(50.dp)
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(searchResults) { user ->
                SearchResultItem(user = user, navController = navController)
            }
        }
    }
}

@Composable
fun SearchResultItem(user: User, navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate("userProfile/${user.uid}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(user.profilePictureUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = user.email, fontSize = 14.sp, color = Color.Gray)
        }

        Button(
            onClick = { /* Handle follow action */ },
            modifier = Modifier
                .padding(start = 8.dp)
                .height(50.dp)
        ) {
            Text("Follow")
        }
    }
}

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val profilePictureUrl: String?
)

fun searchUser(
    query: String,
    firestore: FirebaseFirestore,
    context: android.content.Context,
    onResults: (List<User>) -> Unit
) {
    if (query.isBlank()) {
        onResults(emptyList())
        return
    }

    firestore.collection("users")
        .whereGreaterThanOrEqualTo("name", query)
        .whereLessThanOrEqualTo("name", "$query\uf8ff")
        .get()
        .addOnSuccessListener { documents ->
            val users = documents.mapNotNull { document ->
                val uid = document.id
                val name = document.getString("name") ?: return@mapNotNull null
                val email = document.getString("email") ?: return@mapNotNull null
                val profilePictureUrl = document.getString("profilePictureUrl")
                User(uid, name, email, profilePictureUrl)
            }
            onResults(users)
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error getting documents: $exception", Toast.LENGTH_SHORT)
                .show()
        }
}

@Composable
fun BottomNavigationBar(currentRoute: String, navController: NavHostController) {
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
            onClick = { }
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
