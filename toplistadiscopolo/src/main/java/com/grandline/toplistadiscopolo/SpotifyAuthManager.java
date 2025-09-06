package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

/**
 * Manages Spotify OAuth authorization for the app
 */
public class SpotifyAuthManager {
    private static final String TAG = "SpotifyAuthManager";
    private static final String PREFS_NAME = "spotify_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    
    public static final int REQUEST_CODE = 1337;
    
    private final Context context;
    private static SpotifyAuthManager instance;
    
    // Authorization listener interface
    public interface AuthorizationListener {
        void onAuthorizationComplete(String accessToken);
        void onAuthorizationFailed(String error);
    }
    
    private AuthorizationListener authorizationListener;
    
    private SpotifyAuthManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized SpotifyAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpotifyAuthManager(context);
        }
        return instance;
    }
    
    // Check if device has internet connectivity
    private boolean hasInternetConnection() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean hasConnection = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d(TAG, "Internet connection available: " + hasConnection);
                return hasConnection;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking internet connection: " + e.getMessage());
        }
        return true; // Assume connection is available if we can't check
    }
    
    /**
     * Start the authorization process
     */
    public void startAuthorization(Activity activity, AuthorizationListener listener) {
        this.authorizationListener = listener;
        
        // Check if we have a valid token first
        String existingToken = getStoredAccessToken();
        if (existingToken != null && !isTokenExpired()) {
            Log.d(TAG, "Using existing valid access token");
            if (listener != null) {
                listener.onAuthorizationComplete(existingToken);
            }
            return;
        }
        
        // Check internet connectivity before starting authorization
        if (!hasInternetConnection()) {
            Log.e(TAG, "No internet connection available for authorization");
            if (listener != null) {
                listener.onAuthorizationFailed("NETWORK_ERROR: No internet connection. Please check your network settings and try again.");
            }
            return;
        }
        
        Log.d(TAG, "Starting Spotify authorization flow");
        
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
            Constants.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            Constants.SPOTIFY_REDIRECT_URI
        );
        
        // Request the app-remote-control scope which is required for App Remote SDK
        builder.setScopes(new String[]{"app-remote-control", "streaming"});
        
        // Enable PKCE for Authorization Code Flow
        builder.setShowDialog(false);
        
        AuthorizationRequest request = builder.build();
        
        try {
            AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start authorization", e);
            if (listener != null) {
                listener.onAuthorizationFailed("Failed to start authorization: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle the authorization response
     */
    public void handleAuthorizationResponse(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        
        AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
        Log.d(TAG, "Authorization response received - Type: " + response.getType() + ", ResultCode: " + resultCode);
        
        switch (response.getType()) {
            case CODE:
                Log.d(TAG, "Authorization code received");
                String authorizationCode = response.getCode();
                
                Log.d(TAG, "Authorization code received, exchanging for access token");
                
                // Exchange authorization code for access token
                exchangeCodeForToken(authorizationCode);
                break;
                
            case ERROR:
                String errorMsg = response.getError();
                Log.e(TAG, "Authorization error: " + errorMsg);
                
                // Handle specific error cases
                if (errorMsg != null) {
                    if (errorMsg.equals("NO_INTERNET_CONNECTION")) {
                        Log.e(TAG, "Network connection issue during authorization");
                        if (authorizationListener != null) {
                            authorizationListener.onAuthorizationFailed("NETWORK_ERROR: Please check your internet connection and try again.");
                        }
                    } else if (errorMsg.equals("USER_CANCELLED")) {
                        Log.w(TAG, "User cancelled authorization");
                        if (authorizationListener != null) {
                            authorizationListener.onAuthorizationFailed("Authorization cancelled by user");
                        }
                    } else {
                        if (authorizationListener != null) {
                            authorizationListener.onAuthorizationFailed("Authorization error: " + errorMsg);
                        }
                    }
                } else {
                    if (authorizationListener != null) {
                        authorizationListener.onAuthorizationFailed("Unknown authorization error");
                    }
                }
                break;
                
            case EMPTY:
                Log.w(TAG, "Authorization cancelled by user or empty response");
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationFailed("Authorization cancelled by user");
                }
                break;
                
            default:
                Log.w(TAG, "Unknown authorization response type: " + response.getType());
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationFailed("Unknown response type: " + response.getType());
                }
                break;
        }
        
        // Clear the listener after handling the response (except for CODE case, cleared in exchangeCodeForToken)
        if (response.getType() != AuthorizationResponse.Type.CODE) {
            authorizationListener = null;
        }
    }
    
    /**
     * Exchange authorization code for access token using Spotify's token endpoint
     */
    private void exchangeCodeForToken(String authorizationCode) {
        Log.d(TAG, "Exchanging authorization code for access token");
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        FormBody formBody = new FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authorizationCode)
            .add("redirect_uri", Constants.SPOTIFY_REDIRECT_URI)
            .add("client_id", Constants.SPOTIFY_CLIENT_ID)
            .build();
        
        Request request = new Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to exchange code for token", e);
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationFailed("Token exchange failed: " + e.getMessage());
                    authorizationListener = null;
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Token exchange response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String accessToken = jsonResponse.getString("access_token");
                            int expiresIn = jsonResponse.getInt("expires_in");
                            
                            Log.d(TAG, "Access token received successfully, expires in: " + expiresIn + " seconds");
                            
                            // Store the token
                            storeAccessToken(accessToken, expiresIn);
                            
                            if (authorizationListener != null) {
                                authorizationListener.onAuthorizationComplete(accessToken);
                                authorizationListener = null;
                            }
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse token response", e);
                            if (authorizationListener != null) {
                                authorizationListener.onAuthorizationFailed("Failed to parse token response: " + e.getMessage());
                                authorizationListener = null;
                            }
                        }
                    } else {
                        Log.e(TAG, "Token exchange failed with response: " + responseBody);
                        
                        String errorMessage = "Token exchange failed";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("error_description")) {
                                errorMessage = errorJson.getString("error_description");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "Could not parse error response", e);
                        }
                        
                        if (authorizationListener != null) {
                            authorizationListener.onAuthorizationFailed(errorMessage);
                            authorizationListener = null;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during token exchange", e);
                    if (authorizationListener != null) {
                        authorizationListener.onAuthorizationFailed("Unexpected error: " + e.getMessage());
                        authorizationListener = null;
                    }
                }
            }
        });
    }
    
    /**
     * Get the stored access token if it's still valid
     */
    public String getValidAccessToken() {
        String token = getStoredAccessToken();
        if (token != null && !isTokenExpired()) {
            return token;
        }
        return null;
    }
    
    /**
     * Check if the user is authorized (has a valid token)
     */
    public boolean isAuthorized() {
        String token = getValidAccessToken();
        boolean authorized = token != null;
        Log.d(TAG, "Authorization check: " + (authorized ? "AUTHORIZED" : "NOT AUTHORIZED"));
        if (authorized) {
            Log.d(TAG, "Valid token available");
        } else {
            String storedToken = getStoredAccessToken();
            if (storedToken != null) {
                Log.d(TAG, "Token exists but expired");
            } else {
                Log.d(TAG, "No token stored");
            }
        }
        return authorized;
    }
    
    /**
     * Clear the stored authorization
     */
    public void clearAuthorization() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "Authorization cleared");
    }
    
    private void storeAccessToken(String token, int expiresInSeconds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply();
        
        Log.d(TAG, "Access token stored, expires at: " + new java.util.Date(expiryTime));
    }
    
    private String getStoredAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    private boolean isTokenExpired() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        
        // Add a 5-minute buffer to avoid using tokens that are about to expire
        long currentTime = System.currentTimeMillis() + (5 * 60 * 1000);
        
        boolean expired = currentTime >= expiryTime;
        if (expired) {
            Log.d(TAG, "Token expired at: " + new java.util.Date(expiryTime));
        }
        
        return expired;
    }
}