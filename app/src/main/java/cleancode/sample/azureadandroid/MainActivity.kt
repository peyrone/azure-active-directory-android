package cleancode.sample.azureadandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cleancode.sample.authentication.activity.AuthenticationActivity
import cleancode.sample.authentication.activity.BaseActivity

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun baseInit(savedInstanceState: Bundle?) {
        Log.d("FRD_MA", "baseInit")
        gotoStartActivity()
    }

    private fun gotoStartActivity() {
        Log.d("FRD_MA", "gotoStartActivity")
        val intent = Intent(this, AuthenticationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(intent)
        finish()
    }
}
