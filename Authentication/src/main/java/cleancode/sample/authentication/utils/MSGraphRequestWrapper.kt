package cleancode.sample.authentication.utils

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import cleancode.sample.authentication.activity.AuthenticationActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*

object MSGraphRequestWrapper {

    private val TAG: String = AuthenticationActivity::class.java.simpleName

    // See: https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
    const val MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/"

    fun callGraphAPIUsingVolley(@NonNull context: Context,
                                @NonNull graphResourceUrl: String,
                                @NonNull accessToken: String,
                                @NonNull responseListener: Response.Listener<JSONObject>,
                                @NonNull errorListener: Response.ErrorListener) {
        Log.d(TAG, "Starting volley request to graph")

        /* Make sure we have a token to send to graph */
        if (accessToken.isEmpty()) {
            return
        }

        val queue: RequestQueue = Volley.newRequestQueue(context)
        var parameters: JSONObject = JSONObject()

        try {
            parameters.put("key", "value")
        } catch (ex: Exception) {
            Log.d(TAG, "Failed to put parameters: $ex")
        }

        val request: JsonObjectRequest = object : JsonObjectRequest(Method.GET, graphResourceUrl,
            parameters, responseListener, errorListener) {
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["Authorization"] = "Bearer $accessToken"
                return headers
            }
        }

        Log.d(TAG,"Adding HTTP GET to Queue, Request: $request")

        request.retryPolicy = DefaultRetryPolicy(
            3000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }
}