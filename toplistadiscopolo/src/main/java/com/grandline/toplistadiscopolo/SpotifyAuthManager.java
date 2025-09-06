package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_CODE_VERIFIER = "code_verifier";
    
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
     * Validate Spotify app configuration
     */
    private boolean validateSpotifyConfiguration() {
        if (Constants.SPOTIFY_CLIENT_ID == null || Constants.SPOTIFY_CLIENT_ID.isEmpty() || Constants.SPOTIFY_CLIENT_ID.equals("YOUR_CLIENT_ID_HERE")) {
            Log.e(TAG, "Spotify Client ID is not configured properly");
            return false;
        }
        
        if (Constants.SPOTIFY_REDIRECT_URI == null || Constants.SPOTIFY_REDIRECT_URI.isEmpty() || Constants.SPOTIFY_REDIRECT_URI.equals("YOUR_REDIRECT_URI_HERE")) {
            Log.e(TAG, "Spotify Redirect URI is not configured properly");
            return false;
        }
        
        if (!Constants.SPOTIFY_REDIRECT_URI.startsWith(context.getPackageName() + "://")) {
            Log.w(TAG, "Redirect URI should typically start with package name for deep linking");
        }
        
        Log.d(TAG, "Spotify configuration validated successfully");
        return true;
    }
    
    /**
     * Start the authorization process with PKCE
     */
    public void startAuthorization(Activity activity, AuthorizationListener listener) {
        this.authorizationListener = listener;
        
        // Validate Spotify configuration first
        if (!validateSpotifyConfiguration()) {
            Log.e(TAG, "Invalid Spotify configuration");
            if (listener != null) {
                listener.onAuthorizationFailed("Invalid Spotify app configuration. Please check client ID and redirect URI.");
            }
            return;
        }
        
        // Check if we have a valid token first
        String existingToken = getStoredAccessToken();
        if (existingToken != null && !isTokenExpired()) {
            Log.d(TAG, "Using existing valid access token");
            if (listener != null) {
                listener.onAuthorizationComplete(existingToken);
            }
            return;
        }
        
        // Try to refresh token if we have a refresh token
        String refreshToken = getStoredRefreshToken();
        if (refreshToken != null) {
            Log.d(TAG, "Attempting to refresh access token");
            refreshAccessToken(refreshToken, listener);
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
        
        Log.d(TAG, "Starting Spotify PKCE authorization flow");
        Log.d(TAG, "Client ID: " + Constants.SPOTIFY_CLIENT_ID);
        Log.d(TAG, "Redirect URI: " + Constants.SPOTIFY_REDIRECT_URI);
        Log.d(TAG, "IMPORTANT: App must be configured as 'Mobile App' in Spotify Dashboard for PKCE to work");
        
        try {
            // Generate PKCE parameters
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            
            // Store code verifier for later use
            storeCodeVerifier(codeVerifier);
            
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                Constants.SPOTIFY_CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                Constants.SPOTIFY_REDIRECT_URI
            );
            
            // Request the app-remote-control scope which is required for App Remote SDK
            builder.setScopes(new String[]{"app-remote-control", "streaming"});
            
            // Add PKCE parameters
            builder.setCustomParam("code_challenge_method", "S256");
            builder.setCustomParam("code_challenge", codeChallenge);
            
            // Enable PKCE for Authorization Code Flow
            builder.setShowDialog(false);
            
            AuthorizationRequest request = builder.build();
            
            AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request);
            Log.d(TAG, "PKCE authorization request sent");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PKCE authorization", e);
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
                
                if (authorizationCode != null && !authorizationCode.isEmpty()) {
                    Log.d(TAG, "Authorization code received, exchanging for access token");
                    
                    // Exchange authorization code for access token
                    exchangeCodeForToken(authorizationCode);
                } else {
                    Log.e(TAG, "Authorization code is null or empty");
                    if (authorizationListener != null) {
                        authorizationListener.onAuthorizationFailed("Invalid authorization code received");
                        authorizationListener = null;
                    }
                }
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
                    } else if (errorMsg.equals("USER_CANCELLED") || errorMsg.equals("access_denied")) {
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
     * Exchange authorization code for access token using PKCE
     * 
     * This method implements the Authorization Code with PKCE flow.
     * For Mobile App configuration: Uses only code_verifier/code_challenge (no client_secret)
     * For Web App configuration: Uses both PKCE and client_secret for compatibility
     */
    private void exchangeCodeForToken(String authorizationCode) {
        Log.d(TAG, "Exchanging authorization code for access token using PKCE");
        
        String codeVerifier = getStoredCodeVerifier();
        if (codeVerifier == null) {
            Log.e(TAG, "No code verifier found for PKCE");
            if (authorizationListener != null) {
                authorizationListener.onAuthorizationFailed("PKCE code verifier not found");
                authorizationListener = null;
            }
            return;
        }
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        // Build form body for token exchange
        // For Web App configuration, client_secret is required even with PKCE
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("grant_type", "authorization_code");
        formBuilder.add("code", authorizationCode);
        formBuilder.add("redirect_uri", Constants.SPOTIFY_REDIRECT_URI);
        formBuilder.add("client_id", Constants.SPOTIFY_CLIENT_ID);
        formBuilder.add("code_verifier", codeVerifier);
        
        // Add client_secret for Web App configuration compatibility
        if (Constants.SPOTIFY_CLIENT_SECRET != null && !Constants.SPOTIFY_CLIENT_SECRET.equals("YOUR_CLIENT_SECRET_HERE")) {
            formBuilder.add("client_secret", Constants.SPOTIFY_CLIENT_SECRET);
        }
        
        FormBody formBody = formBuilder.build();
        
        // Additional verification: log the actual form body content
        Log.d(TAG, "Form body size: " + formBody.size() + " parameters");
        
        Log.d(TAG, "Token exchange request parameters:");
        Log.d(TAG, "- grant_type: authorization_code");
        Log.d(TAG, "- client_id: " + Constants.SPOTIFY_CLIENT_ID);
        Log.d(TAG, "- redirect_uri: " + Constants.SPOTIFY_REDIRECT_URI);
        Log.d(TAG, "- code_verifier: [PRESENT]");
        
        boolean hasClientSecret = Constants.SPOTIFY_CLIENT_SECRET != null && !Constants.SPOTIFY_CLIENT_SECRET.equals("YOUR_CLIENT_SECRET_HERE");
        Log.d(TAG, "- client_secret: " + (hasClientSecret ? "[INCLUDED - WEB APP CONFIG]" : "[NOT INCLUDED - MOBILE APP CONFIG]"));
        
        Request request = new Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build();
            
        Log.d(TAG, "Making PKCE token exchange request to: " + request.url());
        Log.d(TAG, "Request headers: " + request.headers());
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to exchange code for token", e);
                
                String errorMessage = "Token exchange failed";
                if (e != null) {
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("timeout")) {
                            errorMessage = "Network timeout during token exchange. Please check your internet connection and try again.";
                        } else if (e.getMessage().contains("network") || e.getMessage().contains("connection")) {
                            errorMessage = "Network error during token exchange. Please check your internet connection and try again.";
                        } else {
                            errorMessage = "Token exchange failed: " + e.getMessage();
                        }
                    }
                }
                
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationFailed(errorMessage);
                    authorizationListener = null;
                }
                // Clear stored code verifier
                clearCodeVerifier();
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Token exchange response code: " + response.code());
                    Log.d(TAG, "Token exchange response headers: " + response.headers());
                    
                    // Log response body for debugging (be careful with sensitive data in production)
                    if (!response.isSuccessful()) {
                        Log.d(TAG, "Token exchange error response: " + responseBody);
                    }
                    
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String accessToken = jsonResponse.getString("access_token");
                            int expiresIn = jsonResponse.getInt("expires_in");
                            
                            // Get refresh token if available
                            String refreshToken = null;
                            if (jsonResponse.has("refresh_token")) {
                                refreshToken = jsonResponse.getString("refresh_token");
                            }
                            
                            Log.d(TAG, "Access token received successfully, expires in: " + expiresIn + " seconds");
                            if (refreshToken != null) {
                                Log.d(TAG, "Refresh token also received");
                            }
                            
                            // Store the tokens
                            storeTokens(accessToken, refreshToken, expiresIn);
                            
                            // Clear the code verifier as it's no longer needed
                            clearCodeVerifier();
                            
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
                            clearCodeVerifier();
                        }
                    } else {
                        Log.e(TAG, "Token exchange failed with HTTP " + response.code() + ": " + responseBody);
                        
                        String errorMessage = "Token exchange failed (HTTP " + response.code() + ")";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("error_description")) {
                                String errorDesc = errorJson.getString("error_description");
                                Log.e(TAG, "Spotify error description: " + errorDesc);
                                errorMessage = errorDesc;
                                
                                // Handle specific Spotify errors
                                if (errorDesc.contains("invalid_client")) {
                                    errorMessage = "Invalid Spotify client configuration. Please check app settings.";
                                } else if (errorDesc.contains("invalid_grant")) {
                                    errorMessage = "Authorization code expired or invalid. Please try again.";
                                } else if (errorDesc.contains("invalid_request")) {
                                    errorMessage = "Invalid authorization request. Please try again.";
                                }
                            } else if (errorJson.has("error")) {
                                String error = errorJson.getString("error");
                                Log.e(TAG, "Spotify error: " + error);
                                errorMessage = "Authorization failed: " + error;
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "Could not parse error response", e);
                            // Use HTTP status code to provide better error messages
                            if (response.code() == 400) {
                                errorMessage = "Bad request - Invalid authorization parameters";
                            } else if (response.code() == 401) {
                                errorMessage = "Unauthorized - Invalid client credentials";
                            } else if (response.code() == 403) {
                                errorMessage = "Forbidden - Access denied";
                            } else if (response.code() >= 500) {
                                errorMessage = "Spotify server error. Please try again later.";
                            }
                        }
                        
                        if (authorizationListener != null) {
                            authorizationListener.onAuthorizationFailed(errorMessage);
                            authorizationListener = null;
                        }
                        clearCodeVerifier();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during token exchange", e);
                    if (authorizationListener != null) {
                        authorizationListener.onAuthorizationFailed("Unexpected error: " + e.getMessage());
                        authorizationListener = null;
                    }
                    clearCodeVerifier();
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
                // Log when the token expired for debugging
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                long expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
                if (expiryTime > 0) {
                    Log.d(TAG, "Token expired at: " + new java.util.Date(expiryTime));
                    Log.d(TAG, "Current time: " + new java.util.Date());
                }
            } else {
                Log.d(TAG, "No token stored");
            }
        }
        return authorized;
    }
    
    /**
     * Get authorization status details for debugging
     */
    public String getAuthorizationStatusDebug() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder debug = new StringBuilder();
        
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        long expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        String codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null);
        
        debug.append("Authorization Status Debug:\n");
        debug.append("- Access Token: ").append(accessToken != null ? "Present" : "Missing").append("\n");
        debug.append("- Refresh Token: ").append(refreshToken != null ? "Present" : "Missing").append("\n");
        debug.append("- Code Verifier: ").append(codeVerifier != null ? "Present" : "Missing").append("\n");
        
        if (expiryTime > 0) {
            debug.append("- Token Expiry: ").append(new java.util.Date(expiryTime)).append("\n");
            debug.append("- Current Time: ").append(new java.util.Date()).append("\n");
            debug.append("- Token Expired: ").append(isTokenExpired()).append("\n");
        } else {
            debug.append("- Token Expiry: Not set\n");
        }
        
        debug.append("- Is Authorized: ").append(isAuthorized());
        
        return debug.toString();
    }
    
    /**
     * Check if authorization process is in progress (has code verifier but no token)
     */
    public boolean isAuthorizationInProgress() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null);
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        
        return codeVerifier != null && accessToken == null;
    }
    
    /**
     * Force check if authorization completed without callback
     * This can be used when the authorization callback might have been missed
     */
    public void checkAuthorizationCompletion(AuthorizationListener listener) {
        if (isAuthorized()) {
            Log.d(TAG, "Authorization already completed");
            if (listener != null) {
                listener.onAuthorizationComplete(getValidAccessToken());
            }
            return;
        }
        
        if (isAuthorizationInProgress()) {
            Log.w(TAG, "Authorization appears to be in progress but no token found");
            Log.d(TAG, getAuthorizationStatusDebug());
            
            // Clear the stale code verifier and restart authorization
            clearCodeVerifier();
            if (listener != null) {
                listener.onAuthorizationFailed("Authorization process incomplete. Please try again.");
            }
        } else {
            Log.d(TAG, "No authorization in progress");
            if (listener != null) {
                listener.onAuthorizationFailed("No authorization process found. Please start authorization.");
            }
        }
    }
    
    /**
     * Clear the stored authorization
     */
    public void clearAuthorization() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "Authorization cleared");
    }
    
    private void storeTokens(String accessToken, String refreshToken, int expiresInSeconds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        
        SharedPreferences.Editor editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime);
        
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        
        editor.apply();
        
        Log.d(TAG, "Access token stored, expires at: " + new java.util.Date(expiryTime));
        if (refreshToken != null) {
            Log.d(TAG, "Refresh token also stored");
        }
    }
    
    private void storeAccessToken(String token, int expiresInSeconds) {
        storeTokens(token, null, expiresInSeconds);
    }
    
    private String getStoredAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    private String getStoredRefreshToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_REFRESH_TOKEN, null);
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
    
    // PKCE helper methods
    
    /**
     * Generate a cryptographically secure random string for PKCE code verifier
     */
    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.encodeToString(codeVerifier, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
    
    /**
     * Generate code challenge from code verifier using SHA256
     */
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.encodeToString(digest, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
    
    /**
     * Store the code verifier for later use in token exchange
     */
    private void storeCodeVerifier(String codeVerifier) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply();
        Log.d(TAG, "Code verifier stored for PKCE");
    }
    
    /**
     * Get the stored code verifier
     */
    private String getStoredCodeVerifier() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CODE_VERIFIER, null);
    }
    
    /**
     * Clear the stored code verifier
     */
    private void clearCodeVerifier() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CODE_VERIFIER).apply();
        Log.d(TAG, "Code verifier cleared");
    }
    
    /**
     * Refresh the access token using the refresh token
     * 
     * For Mobile App configuration: Uses only refresh_token (no client_secret)
     * For Web App configuration: Uses both refresh_token and client_secret
     */
    private void refreshAccessToken(String refreshToken, AuthorizationListener listener) {
        Log.d(TAG, "Refreshing access token");
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        // Build form body for token refresh
        FormBody.Builder formBuilder = new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", Constants.SPOTIFY_CLIENT_ID);
            
        // Add client_secret for Web App configuration compatibility
        boolean hasClientSecret = Constants.SPOTIFY_CLIENT_SECRET != null && !Constants.SPOTIFY_CLIENT_SECRET.equals("YOUR_CLIENT_SECRET_HERE");
        if (hasClientSecret) {
            formBuilder.add("client_secret", Constants.SPOTIFY_CLIENT_SECRET);
        }
        
        FormBody formBody = formBuilder.build();
        
        Log.d(TAG, "Token refresh request parameters:");
        Log.d(TAG, "- grant_type: refresh_token");
        Log.d(TAG, "- client_id: " + Constants.SPOTIFY_CLIENT_ID);
        Log.d(TAG, "- client_secret: " + (hasClientSecret ? "[INCLUDED - WEB APP CONFIG]" : "[NOT INCLUDED - MOBILE APP CONFIG]"));
        
        Request request = new Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to refresh token", e);
                // If refresh fails, clear tokens and start fresh authorization
                clearAuthorization();
                if (listener != null) {
                    listener.onAuthorizationFailed("Token refresh failed: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Token refresh response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String accessToken = jsonResponse.getString("access_token");
                            int expiresIn = jsonResponse.getInt("expires_in");
                            
                            // Some refresh responses might include a new refresh token
                            String newRefreshToken = refreshToken; // Keep the old one by default
                            if (jsonResponse.has("refresh_token")) {
                                newRefreshToken = jsonResponse.getString("refresh_token");
                            }
                            
                            Log.d(TAG, "Access token refreshed successfully, expires in: " + expiresIn + " seconds");
                            
                            // Store the new tokens
                            storeTokens(accessToken, newRefreshToken, expiresIn);
                            
                            if (listener != null) {
                                listener.onAuthorizationComplete(accessToken);
                            }
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse refresh token response", e);
                            clearAuthorization();
                            if (listener != null) {
                                listener.onAuthorizationFailed("Failed to parse refresh response: " + e.getMessage());
                            }
                        }
                    } else {
                        Log.e(TAG, "Token refresh failed with response: " + responseBody);
                        // If refresh fails, clear tokens and start fresh authorization
                        clearAuthorization();
                        
                        String errorMessage = "Token refresh failed";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("error_description")) {
                                errorMessage = errorJson.getString("error_description");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "Could not parse refresh error response", e);
                        }
                        
                        if (listener != null) {
                            listener.onAuthorizationFailed(errorMessage);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during token refresh", e);
                    clearAuthorization();
                    if (listener != null) {
                        listener.onAuthorizationFailed("Unexpected refresh error: " + e.getMessage());
                    }
                }
            }
        });
    }
}