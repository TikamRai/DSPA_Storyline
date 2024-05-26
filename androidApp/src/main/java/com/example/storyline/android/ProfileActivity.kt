package com.example.storyline.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.storyline.android.R

class ProfileActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        setContent {
            Theme {
                ProfileScreen(onProfilePictureClick = { pickImage() }, onLogoutClick =  { logout()})
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImage(uri: Uri) {
        val user = auth.currentUser ?: return
        val storageRef = storage.reference.child("profile_pictures/${user.uid}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateProfilePictureInFireStore(downloadUri)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Failed to update profile picture: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun ProfileScreen(onProfilePictureClick: () -> Unit, onLogoutClick: () -> Unit) {
    var name by remember { mutableStateOf("John Green") }
    var email by remember { mutableStateOf("johngreen@email.com") }
    var description by remember { mutableStateOf("abcdefgihjklmnopqrstuvwxyz") }
    var followers by remember { mutableStateOf(0) }
    var following by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .background(Color(0xFFF5F5F5)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8BBF8C))
        ) {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
                    .align(alignment = Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

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
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = email, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        ElevatedButton(onClick = { /* Handle edit profile */ },
            modifier = Modifier
                .fillMaxWidth(0.50f),
            colors = ButtonColors(
                containerColor = Color(0xFFE6F0E1),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.DarkGray)
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
                    Text(
                        text = "$followers Followers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally),
                    )
                }
                Column() {
                    Text(
                        text = "$following Following",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* Handle published stories */ },
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.20f)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonColors(
                containerColor = Color(0xFF8BBF8C),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.DarkGray)
        ) {
            Text(
                text = "Published Stories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
                )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* Handle reading list */ },
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonColors(
                containerColor = Color(0xFF8BBF8C),
                contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.DarkGray)
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
    }
}
