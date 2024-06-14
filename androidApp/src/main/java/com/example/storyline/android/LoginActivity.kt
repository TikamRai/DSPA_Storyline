package com.example.storyline.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            Theme {
                LoginScreen(
                    onSignUpClick = {
                        val intent = Intent(this, SignupActivity::class.java)
                        startActivity(intent)
                    },
                    onLoginClick = { email, password, rememberMe ->
                        loginUser(email, password, rememberMe)
                    },
                    onForgotPasswordClick = { email ->
                        sendPasswordResetEmail(email)
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String, rememberMe: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val loggedInUserId = auth.currentUser?.uid // Get logged-in user's ID
                    if (rememberMe) {
                        val sharedPreferences =
                            getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("savedEmail", email)
                            putString("loggedInUserId", loggedInUserId)
                            apply()
                        }
                    }
                    Toast.makeText(baseContext, "Login Successful.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, "Password reset email sent.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Failed to send password reset email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}

@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit,
    onLoginClick: (String, String, Boolean) -> Unit,
    onForgotPasswordClick: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    var email by remember { mutableStateOf(sharedPreferences.getString("savedEmail", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(email.isNotEmpty()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 6.dp)
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                }
                Column {
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
                        onClick = { onForgotPasswordClick(email) },
                        modifier = Modifier
                            .padding(2.dp)
                            .align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF2DAAFF)
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
                        onLoginClick(email, password, rememberMe)
                    } else {
                        Toast.makeText(context, "Email or password is empty!!", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
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
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF2DAAFF)
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
