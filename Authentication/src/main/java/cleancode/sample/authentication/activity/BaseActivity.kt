package cleancode.sample.authentication.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutId = getLayoutId()
        Log.d("FRD_BA", "onCreate: $layoutId")
        if (layoutId > 0) {
            setContentView(layoutId)
        }

        baseInit(savedInstanceState)
    }

    fun changeLanguage(lang: String) {
        Log.d("FRD_BA", "changeLanguage: $lang")
        val res = resources
        // Change locale settings in the app.
        val dm = res.displayMetrics
        val conf = res.configuration
        conf.locale = Locale(lang)
        res.updateConfiguration(conf, dm)
    }

    protected abstract fun getLayoutId(): Int

    protected abstract fun baseInit(savedInstanceState: Bundle?)

    protected fun showShortToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    protected fun openActivity(toActivity: Class<out BaseActivity>) {
        openActivity(toActivity, null)
    }

    protected fun openActivity(toActivity: Class<out BaseActivity>, parameter: Bundle? = null) {
        val intent = Intent(this, toActivity)
        if (parameter != null) {
            intent.putExtras(parameter)
        }
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun closeActivity(intent: Intent, resultStatus: Int){
        setResult(resultStatus, intent)
        finish()
    }
}