package com.kinnect.i230507_i230017

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Calendar

class SignupActivity : AppCompatActivity() {
    private val localNetwork = LocalNetworkPermission(this)

    private var birthday: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        findViewById<TextView>(R.id.tvLogin)?.setOnClickListener { finish() }

        setupGender()
        setupBirthday()
        setupPasswordStrength()

        findViewById<Button>(R.id.btnCreateAccount2)?.setOnClickListener { createAccount() }
        if (!LocalNetworkPermission.granted(this)) localNetwork.ensure()
    }

    private fun setupGender() {
        val options = listOf<RadioButton>(
            findViewById(R.id.rbFemale), findViewById(R.id.rbMale), findViewById(R.id.rbCustom)
        )
        options.forEach { selected ->
            selected.setOnClickListener {
                options.forEach { option ->
                    if (option == selected) {
                        option.setBackgroundResource(R.drawable.kn_card)
                        option.setTextColor(getColor(R.color.kn_teal))
                    } else {
                        option.setBackgroundResource(R.drawable.kn_card)
                        option.setTextColor(getColor(R.color.kn_text))
                    }
                }
            }
        }
    }

    private fun selectedGender(): String = when {
        findViewById<RadioButton>(R.id.rbFemale).isChecked -> "Female"
        findViewById<RadioButton>(R.id.rbMale).isChecked -> "Male"
        findViewById<RadioButton>(R.id.rbCustom).isChecked -> "Custom"
        else -> ""
    }

    private fun setupBirthday() {
        val etDay = findViewById<EditText>(R.id.etDay)
        val etMonth = findViewById<EditText>(R.id.etMonth)
        val etYear = findViewById<EditText>(R.id.etYear)
        val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

        val open = View.OnClickListener {
            val c = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
            DatePickerDialog(this, { _, y, m, d ->
                etDay.setText(d.toString())
                etMonth.setText(months[m])
                etYear.setText(y.toString())
                birthday = "%04d-%02d-%02d".format(y, m + 1, d)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        etDay.setOnClickListener(open)
        etMonth.setOnClickListener(open)
        etYear.setOnClickListener(open)
    }

    private fun setupPasswordStrength() {
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val bars = listOf<View>(findViewById(R.id.bar1), findViewById(R.id.bar2), findViewById(R.id.bar3))
        val tv = findViewById<TextView>(R.id.tvStrength)

        fun update(pw: String) {
            var score = 0
            if (pw.length >= 8) score++
            if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) score++
            if (pw.any { it.isDigit() }) score++
            if (pw.any { !it.isLetterOrDigit() }) score++

            val (filled, label, color) = when {
                pw.isEmpty() -> Triple(0, "", R.color.kn_text2)
                score <= 1 -> Triple(1, "Weak", R.color.kn_rust)
                score <= 3 -> Triple(2, "Medium", R.color.kn_rust)
                else -> Triple(3, "Strong", R.color.kn_teal)
            }
            bars.forEachIndexed { i, v ->
                v.setBackgroundColor(getColor(if (i < filled) color else R.color.kn_chip))
            }
            tv.text = label
            tv.setTextColor(getColor(color))
        }

        update("")
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = update(s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun createAccount() {
        if (!LocalNetworkPermission.granted(this)) {
            localNetwork.ensure { createAccount() }
            return
        }
        val first = findViewById<EditText>(R.id.etFirstName).text.toString().trim()
        val last = findViewById<EditText>(R.id.etLastName).text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString()
        val gender = selectedGender()

        when {
            first.isEmpty() || last.isEmpty() -> return toast("Enter your first and last name")
            birthday.isEmpty() -> return toast("Select your birthday")
            gender.isEmpty() -> return toast("Select your gender")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> return toast("Enter a valid email")
            password.length < 8 -> return toast("Password must be at least 8 characters")
            !findViewById<CheckBox>(R.id.cbTerms).isChecked -> return toast("Please accept the Terms")
        }

        val url = "${ApiClient.BASE_URL}signup.php"
        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val token = json.getString("token")
                        val fullName = "$first $last"
                        RecentLoginStore.save(this, fullName, email, token, json.getJSONObject("user").getString("id"), json.getLong("expires_at"), false)
                        PushRegistration.register(this)

                        // First-time users navigate to Edit Profile (Screen 16) before Home
                        val intent = Intent(this, EditProfileActivity::class.java).apply {
                            putExtra("firstTime", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    } else {
                        toast(json.getString("message"))
                    }
                } catch (e: Exception) {
                    toast("Parsing error: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                toast("Network error: ${error.message ?: "Could not connect to server"}")
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "first_name" to first,
                    "last_name" to last,
                    "email" to email,
                    "password" to password,
                    "gender" to gender,
                    "birthday" to birthday
                )
            }
        }
        queue.add(request)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
