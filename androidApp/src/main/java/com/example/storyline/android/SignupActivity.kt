package com.example.storyline.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storyline.SignupViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class SignupActivity : ComponentActivity() {
    private val viewModel = SignupViewModel()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            auth = Firebase.auth
            firestore = FirebaseFirestore.getInstance()
            Theme {
                SignupScreen(onLoginClick = {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }, viewModel = viewModel, auth = auth, firestore = firestore, onSignUpSuccess = {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                })
            }
        }
    }
}

private fun isEmailValid(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun signUpWithEmailAndPassword(
    name: String,
    email: String,
    password: String,
    confirmationPassword: String,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onSignUpSuccess: () -> Unit,
    onSignUpFailure: (String) -> Unit
) {
    if (name.isEmpty()) {
        onSignUpFailure("Name is required")
        return
    }

    if (email.isEmpty()) {
        onSignUpFailure("Email is required")
        return
    }

    if (!isEmailValid(email)) {
        onSignUpFailure("Enter valid email")
        return
    }

    if (password.isEmpty()) {
        onSignUpFailure("Password is required")
        return
    }

    if (password != confirmationPassword) {
        onSignUpFailure("The passwords do not match")
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val userId = user?.uid
                if (userId != null) {
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email
                    )
                    firestore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            onSignUpSuccess()
                        }
                        .addOnFailureListener { e ->
                            onSignUpFailure("Failed to save user: ${e.message}")
                        }
                } else {
                    onSignUpFailure("Failed to get user ID")
                }
            } else {
                task.exception?.message?.let { onSignUpFailure(it) }
            }
        }
}

@Composable
fun SignupScreen(
    onLoginClick: () -> Unit,
    viewModel: SignupViewModel,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onSignUpSuccess: () -> Unit
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmationPassword by viewModel.confirmationPassword.collectAsState()
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign Up",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Name",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp)
            )

            TextField(
                value = name,
                onValueChange = { viewModel.onNameChanged(it) },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Email Address",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp)
            )

            TextField(
                value = email,
                onValueChange = { viewModel.onEmailChanged(it) },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
                onValueChange = { viewModel.onPasswordChanged(it) },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Confirm Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp)
            )

            TextField(
                value = confirmationPassword,
                onValueChange = { viewModel.onConfirmationPasswordChanged(it) },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            ElevatedButton(
                onClick = {
                    signUpWithEmailAndPassword(
                        name,
                        email,
                        password,
                        confirmationPassword,
                        auth,
                        firestore,
                        onSignUpSuccess = {
                            Toast.makeText(
                                context,
                                "Registered successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSignUpSuccess()
                        },
                        onSignUpFailure = {
                            errorMessage = it
                        }
                    )
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
                    "Sign Up",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight
                        .Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Already have an account?",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally)
            )

            TextButton(
                onClick = onLoginClick,
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
                    text = "Login",
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}
