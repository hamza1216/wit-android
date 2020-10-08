package com.waterloo.wit.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.waterloo.wit.MainApplication
import com.waterloo.wit.R
import com.waterloo.wit.data.UserItem
import com.waterloo.wit.helpers.WitHelper
import com.waterloo.wit.utils.LogUtils
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.edit_email
import kotlinx.android.synthetic.main.activity_login.edit_password
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*
import kotlin.collections.HashMap

class RegisterActivity : BaseActivity() {
    private val TAG = RegisterActivity::class.java.simpleName

    private var firstName: String? = null
    private var lastName: String? = null
    private var email: String? = null
    private var password: String? = null
    private var passwordConfirm: String? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //action bar
        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.text_register)
        actionbar.setDisplayHomeAsUpEnabled(true)
        //actionbar.setHomeButtonEnabled(true)
    }

    override fun onStart() {
        super.onStart()

        but_register.setOnClickListener{
            creeateNewAccount()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun creeateNewAccount() {
        firstName = edit_firstname.text.toString()
        lastName = edit_lastname.text.toString()
        email = edit_email.text.toString()
        password = edit_password.text.toString()
        passwordConfirm = edit_password_confirm.text.toString()
        if(!isEmailValid(email!!)){
            Toast.makeText(this, "Enter the valid email address!.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)
            && !TextUtils.isEmpty(passwordConfirm) && !TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
            if(password == passwordConfirm){
/*
                val dbHelper = MainApplication.instance.dbHelper
                val organizationList = dbHelper.readAllOrganization()
                if(organizationList.count()>0) {
                    val organizationItem = organizationList.get(0)
                    dbHelper.insertUser(UserItem(WitHelper.generateUniqId(), organizationItem.UID, firstName!!, lastName!!, email!!, password!!))
                    Toast.makeText(this, "Registration success.", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }
                else{
                    Toast.makeText(this, "Failed to register", Toast.LENGTH_SHORT).show()

                }
 */
                // Firebase
                mAuth = FirebaseAuth.getInstance()
                mFirebaseFirestore = FirebaseFirestore.getInstance()
                showProgressDialog("Please wait...")
                mAuth.createUserWithEmailAndPassword(email!!, password!!)
                    .addOnSuccessListener{
                        val userId = mAuth.currentUser!!.uid
                        hideProgressDialog()
                        val hashMap = HashMap<String, String>()
                        hashMap.put("firstname", firstName!!)
                        hashMap.put("lastname", lastName!!)
                        hashMap.put("email", email!!)
                        mFirebaseFirestore.collection("users").document(userId).set(hashMap).addOnSuccessListener {
                            Toast.makeText(this, "Registration success.", Toast.LENGTH_SHORT).show()
                            mAuth.signOut()
                            onBackPressed()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to register", Toast.LENGTH_SHORT).show()
                            mAuth.signOut()
                        }
                    }
                    .addOnFailureListener {
                        LogUtils.e(TAG, it.localizedMessage)
                        hideProgressDialog()
                        Toast.makeText(this, "Failed to register", Toast.LENGTH_SHORT).show()
                    }
            }
            else{
                Toast.makeText(this, getString(R.string.text_password_notmatcing), Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, getString(R.string.text_invalid_userinfo), Toast.LENGTH_SHORT).show()
        }
    }}
