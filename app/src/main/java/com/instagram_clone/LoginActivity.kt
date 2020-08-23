package com.instagram_clone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        btn_email_login.setOnClickListener {
            signInAndSignUp()
        }
    }

    override fun onStart() {
        super.onStart()
        // last ID or no
        moveMainPage(auth?.currentUser)
    }

    private fun signInAndSignUp(){
        auth?.createUserWithEmailAndPassword(et_email.text.toString(), et_password.text.toString())?.addOnCompleteListener{
            task->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else if(task.exception?.message.isNullOrEmpty()){
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }else{
                    signInEmail()
                }
        }
    }

    private fun signInEmail(){
        auth?.signInWithEmailAndPassword(et_email.text.toString(), et_password.text.toString())
            ?.addOnCompleteListener{
                    task->
            if(task.isSuccessful){
                moveMainPage(task.result?.user)
            }else{
                Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun moveMainPage(user:FirebaseUser?){
        if(user != null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}