package com.example.storyline.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import com.example.storyline.android.LoginScreen
import com.example.storyline.android.SignupActivity
import com.example.storyline.android.Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContent {
            Theme {
                LoginScreen(
                    onSignUpClick = {
                        val intent = Intent(this, SignupActivity::class.java)
                        startActivity(intent)
                    },
                    onLoginClick = { email, password ->
                        loginUser(email, password)
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Handle login failure
                }
            }
    }
}

@Composable
fun LoginScreen(onSignUpClick: () -> Unit, onLoginClick: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.applogo),
                contentDescription = "AppLogo",
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(100.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Email Address",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp)
            )

            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("username@email.com") },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp)
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Column(
                    modifier = Modifier
                        .padding(start = 6.dp)
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                }
                Column() {
                    Text(
                        text = "Remember Me",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(top = 14.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                ) {
                    TextButton(
                        onClick = { /* Handle forgot password */ },
                        modifier = Modifier
                            .padding(2.dp)
                            .align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF2DAAFF),
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 18.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            ElevatedButton(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        onLoginClick(email, password)
                    } else {
                        // Handle empty email or password
                    }
                },
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.20f)
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonColors(
                    containerColor = Color(0xFF8BBF8C),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text(
                    "Log In",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Want to read or create amazing stories?",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally)
            )

            TextButton(
                onClick = onSignUpClick,
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
                    "Sign Up",
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}