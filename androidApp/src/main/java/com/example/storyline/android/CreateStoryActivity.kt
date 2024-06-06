package com.example.storyline.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class CreateStoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "create_screen") {
                    composable("create_screen") { CreateScreen(navController) }
                    composable("creation_screen") { CreationScreen(navController)}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    contentColor = Color.Black)
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

            val drafts = listOf("Draft 1", "Draft 2", "Draft 3","Draft 4", "Draft 5", "Draft 6")

            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.99f)
            ) {
                items(drafts.size) { index ->
                    val draft = drafts[index]
                    OutlinedButton(
                        onClick = { /* Handle draft click */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .padding(6.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3D3D3),
                            contentColor = Color.Black)
                    ) {
                        Text(text = draft, fontSize = 16.sp)
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
            onClick = { /* Navigate to Home */ }
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_search), contentDescription = "Search") },
            selected = currentRoute == "search",
            onClick = {
                val intent = Intent(context, SearchUserActivity::class.java)
                context.startActivity(intent)
            }
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_create), contentDescription = "Create") },
            selected = currentRoute == "create",
            onClick = { /* Navigate to Write Story Page */ }
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_profile), contentDescription = "Profile") },
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
fun CreationScreen(navController: NavHostController) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.LightGray)
                        .clickable { /* Add story cover logic */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Story Cover")
                }

                OutlinedTextField(
                    value = remember { TextFieldValue() },
                    onValueChange = { /* Handle title input */ },
                    label = { Text("Story Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remember { TextFieldValue() },
                    onValueChange = { /* Handle description input */ },
                    label = { Text("Story Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remember { TextFieldValue() },
                    onValueChange = { /* Handle tags input */ },
                    label = { Text("Tags") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remember { TextFieldValue() },
                    onValueChange = { /* Handle category input */ },
                    label = { Text("Story Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { /* Handle start writing story */ },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text(text = "Start Writing")
                }
            }
        }
    )
}
