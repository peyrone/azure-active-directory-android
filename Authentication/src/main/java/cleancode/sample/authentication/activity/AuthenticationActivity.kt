package cleancode.sample.authentication.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import cleancode.sample.authentication.R
import cleancode.sample.authentication.utils.MSGraphRequestWrapper
import com.android.volley.Response
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject


class AuthenticationActivity : BaseActivity() {

    private val TAG: String = AuthenticationActivity::class.java.simpleName

    private lateinit var signInButton: Button
    private lateinit var signOutButton: Button
    private lateinit var callGraphApiInteractiveButton: Button
    private lateinit var callGraphApiSilentButton: Button
    private lateinit var scopeTextView: TextView
    private lateinit var graphResourceTextView: TextView
    private lateinit var logTextView: TextView
    private lateinit var currentUserTextView: TextView
    private lateinit var deviceModeTextView: TextView

    /* Azure AD Variables */
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_authentication
    }

    override fun baseInit(savedInstanceState: Bundle?) {
        initView()

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createSingleAccountPublicClientApplication(applicationContext,
            R.raw.auth_config_single_account,
            object: IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    loadAccount()
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
    }

    private fun initView() {
        signInButton = findViewById(R.id.btn_signIn)
        signInButton.setOnClickListener(clickListener)

        signOutButton = findViewById(R.id.btn_removeAccount)
        signOutButton.setOnClickListener(clickListener)

        callGraphApiInteractiveButton = findViewById(R.id.btn_callGraphInteractively)
        callGraphApiInteractiveButton.setOnClickListener(clickListener)

        callGraphApiSilentButton = findViewById(R.id.btn_callGraphSilently)
        callGraphApiSilentButton.setOnClickListener(clickListener)

        scopeTextView = findViewById(R.id.scope)
        graphResourceTextView = findViewById(R.id.msgraph_url)
        logTextView = findViewById(R.id.txt_log)
        currentUserTextView = findViewById(R.id.current_user)
        deviceModeTextView = findViewById(R.id.device_mode)

        val defaultGraphResourceUrl = MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me"
        graphResourceTextView.text = defaultGraphResourceUrl
    }

    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.btn_signIn -> {
                if (mSingleAccountApp != null) {
                    mSingleAccountApp?.signIn(this, "", getScopes(), getAuthInteractiveCallback())
                }
            }
            R.id.btn_removeAccount -> {
                if (mSingleAccountApp != null) {

                    mSingleAccountApp?.signOut(object :
                        ISingleAccountPublicClientApplication.SignOutCallback {
                        override fun onSignOut() {
                            mAccount = null
                            updateUI()
                            showToastOnSignOut()
                        }

                        override fun onError(exception: MsalException) {
                            displayError(exception)
                        }
                    })
                }
            }
            R.id.btn_callGraphInteractively -> {
                if (mSingleAccountApp != null) {
                    /**
                     * If acquireTokenSilent() returns an error that requires an interaction (MsalUiRequiredException),
                     * invoke acquireToken() to have the user resolve the interrupt interactively.
                     *
                     * Some example scenarios are
                     *  - password change
                     *  - the resource you're acquiring a token for has a stricter set of requirement than your Single Sign-On refresh token.
                     *  - you're introducing a new scope which the user has never consented for.
                     */
                    Log.d(TAG, getScopes().joinToString { "," })
                    mSingleAccountApp?.acquireToken(this, getScopes(), getAuthInteractiveCallback())
                }
            }
            R.id.btn_callGraphSilently -> {
                if (mSingleAccountApp == null) {
                    mSingleAccountApp?.acquireTokenSilentAsync(getScopes(), mAccount!!.authority, getAuthSilentCallback())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("FRD_FDA", "onResume")
    }

    /**
     * Extracts a scope array from a text field,
     * i.e. from "User.Read User.ReadWrite" to ["user.read", "user.readwrite"]
     */
    private fun getScopes(): Array<String> {
        return scopeTextView.text.toString().toLowerCase().split(" ").toTypedArray()
    }

    /**
     * Load the currently signed-in account, if there's any.
     */
    private fun loadAccount() {
        if (mSingleAccountApp == null)
        {
            return
        }
        mSingleAccountApp?.getCurrentAccountAsync(object: ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                mAccount = activeAccount
                updateUI()
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
                    showToastOnSignOut()
                }
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }

    /**
     * Callback used in for silent acquireToken calls.
     */
    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
        return object: SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d(TAG, "Successfully authenticated")
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)

                if (exception is MsalClientException)
                {
                    /* Exception inside MSAL, more info inside MsalError.java */
                }
                else if (exception is MsalServiceException)
                {
                    /* Exception when communicating with the STS, likely config issue */
                }
                else if (exception is MsalUiRequiredException)
                {
                    /* Tokens expired or no session, retry with interactive */
                }
            }

        }
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object: AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.idToken)

                /* Update account */
                mAccount = authenticationResult.account
                updateUI()

                /* call graph */
                callGraphAPI(authenticationResult)
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.")
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)

                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }
        }
    }

    /**
     * Make an HTTP request to obtain MSGraph data
     */
    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            applicationContext,
            graphResourceTextView.text.toString(),
            authenticationResult.accessToken,
            /* Successfully called graph, process data and send to UI */
            Response.Listener<JSONObject> { response ->
                Log.d(TAG, "Response: $response")
                displayGraphResult(response)
            },
            Response.ErrorListener { error ->
                Log.d(TAG, "Error: $error")
                displayError(error)
            })
    }

    //
    // Helper methods manage UI updates
    // ================================
    // displayGraphResult() - Display the graph response
    // displayError() - Display the graph response
    // updateSignedInUI() - Updates UI when the user is signed in
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //

    /**
     * Display the graph response
     */
    private fun displayGraphResult(@NonNull graphResponse: JSONObject) {
        logTextView.text = graphResponse.toString()
    }

    /**
     * Display the error message
     */
    private fun displayError(@NonNull exception: Exception) {
        logTextView.text = exception.toString()
    }

    /**
     * Updates UI based on the current account.
     */
    private fun updateUI() {
        if (mAccount != null)
        {
            signInButton.isEnabled = false
            signOutButton.isEnabled = true
            callGraphApiInteractiveButton.isEnabled = true
            callGraphApiSilentButton.isEnabled = true
            currentUserTextView.text = mAccount!!.getUsername()
        }
        else
        {
            signInButton.isEnabled = true
            signOutButton.isEnabled = false
            callGraphApiInteractiveButton.isEnabled = false
            callGraphApiSilentButton.isEnabled = false
            currentUserTextView.text = "None"
        }
        deviceModeTextView.text = if (mSingleAccountApp!!.isSharedDevice) "Shared" else "Non-shared"
    }

    /**
     * Updates UI when app sign out succeeds
     */
    private fun showToastOnSignOut() {
        val signOutText = "Signed Out."
        currentUserTextView.text = ""
        showShortToast(signOutText)
    }
}