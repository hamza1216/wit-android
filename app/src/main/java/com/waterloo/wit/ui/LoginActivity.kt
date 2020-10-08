package com.waterloo.wit.ui

import android.content.Intent
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.utils.LogUtils
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : BaseActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mFirebaseFirestore = FirebaseFirestore.getInstance()
    }

    override fun onStart() {
        super.onStart()
        but_login.setOnClickListener{
            checkLogin()
        }
        but_signup.setOnClickListener{
            launchRegisterActivity()
        }
        // auto login
        /*
        val userId = MainApplication.instance.prefs.getUserId()
        if(!userId!!.isEmpty()) {
            val userList = MainApplication.instance.dbHelper.readUser(userId)
            if(userList.count() > 0) {
                launchMainActivity()
            }
        }
         */

        mAuth = FirebaseAuth.getInstance()
        var userId = MainApplication.instance.prefs.getUserId()
        var userPassword = MainApplication.instance.prefs.getPassword()
        if(mAuth.currentUser != null && userId != null && !userId.isEmpty())
        {
            showProgressDialog("Loading...")
            mFirebaseFirestore.collection("users").document(userId).get().addOnSuccessListener {
                hideProgressDialog()
                val organization = it.get("organization_id")
                val db_password = it.get("password")
                if(userPassword != null && userPassword!!.equals(db_password)){
                    launchMainActivity()
                }
                else{
                    Toast.makeText(this, "Failed to login.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
            }

        }
    }
    fun checkLogin(): Boolean{
        val email = edit_email.text.toString()
        val password = edit_password.text.toString()
        /*
        val userList = MainApplication.instance.dbHelper.readUser(email, password)
        if(userList.count() > 0){
            val userInfo = userList.get(0)
            MainApplication.instance.prefs.setUserId(userInfo.UID)
            MainApplication.instance.prefs.setEmail(userInfo.Email)
            return true
        }
        return false
         */
        mAuth = FirebaseAuth.getInstance()
        showProgressDialog("Signing...")
        mAuth.signInAnonymously().addOnSuccessListener {
            mFirebaseFirestore.collection("users").document(email).get().addOnSuccessListener {
                hideProgressDialog()
                val organization = it.get("organization_id")
                val db_password = it.get("password")
                if(db_password != null && password.equals(db_password)){
                    if(organization != null) {
                        MainApplication.instance.prefs.setUserId(email)
                        MainApplication.instance.prefs.setEmail(email)
                        MainApplication.instance.prefs.setPassword(password)
                        launchMainActivity()
                    }
                    else{
                        Toast.makeText(this, "Your account is not proved yet.", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                hideProgressDialog()
                Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            hideProgressDialog()
            Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show()
        }
        return true
    }
    fun launchMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun launchRegisterActivity(){
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

}
