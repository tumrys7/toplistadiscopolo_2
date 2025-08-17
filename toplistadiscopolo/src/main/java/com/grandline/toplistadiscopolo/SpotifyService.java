package com.grandline.toplistadiscopolo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class SpotifyService {
    private static final String TAG = "SpotifyService";
    private static final String CLIENT_ID = "1ef55d5630814a3dafc946ef58e266b5";
    private static final String REDIRECT_URI = "com.grandline.toplistadiscopolo://callback";
    
    private static SpotifyService instance;
    private SpotifyAppRemote mSpotifyAppRemote;
    private Context context;
    private boolean isConnecting = false;
    private int connectionRetryCount = 0;
    private static final int MAX_CONNECTION_RETRIES = 3;
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    
    // Listeners
    private SpotifyConnectionListener connectionListener;
    private SpotifyPlayerListener playerListener;
    
    // Private constructor for singleton
    private SpotifyService(Context context) {
        this.context = context.getApplicationContext();
    }
    
    // Singleton getInstance
    public static synchronized SpotifyService getInstance(Context context) {
        if (instance == null) {
            instance = new SpotifyService(context);
        }
        return instance;
    }
    
    // Connection listener interface
    public interface SpotifyConnectionListener {
        void onConnected();
        void onConnectionFailed(Throwable error);
        void onDisconnected();
    }
    
    // Player state listener interface
    public interface SpotifyPlayerListener {
        void onPlayerStateUpdated(PlayerState playerState);
        void onTrackChanged(Track track);
        void onAlbumArtLoaded(Bitmap bitmap);
        void onError(String error);
    }
    
    // Set listeners
    public void setConnectionListener(SpotifyConnectionListener listener) {
        Log.d(TAG, "Setting connection listener: " + (listener != null ? "not null" : "null"));
        this.connectionListener = listener;
        
        // If we're already connected, notify the listener immediately
        if (isConnected() && listener != null) {
            Log.d(TAG, "Already connected, notifying new listener immediately");
            listener.onConnected();
        }
    }
    
    public void setPlayerListener(SpotifyPlayerListener listener) {
        this.playerListener = listener;
    }
    
    // Check if connected
    public boolean isConnected() {
        return mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected();
    }
    
    // Check if currently connecting
    public boolean isConnecting() {
        return isConnecting;
    }
    
    // Check if Spotify app is installed
    public boolean isSpotifyInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.spotify.music", 0);
            Log.d(TAG, "Spotify app is installed");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Spotify app is not installed: " + e.getMessage());
            return false;
        }
    }
    
    // Connect to Spotify
    public void connect() {
        // Check if Spotify is installed first
        if (!isSpotifyInstalled()) {
            Log.e(TAG, "Spotify app is not installed on this device");
            if (connectionListener != null) {
                connectionListener.onConnectionFailed(new Exception("Spotify app is not installed. Please install Spotify from the Play Store."));
            }
            return;
        }
        
        // If already connected, notify listener immediately
        if (isConnected()) {
            Log.d(TAG, "Already connected to Spotify");
            connectionRetryCount = 0; // Reset retry count on successful connection
            if (connectionListener != null) {
                connectionListener.onConnected();
            }
            return;
        }
        
        // If already connecting, don't start a new connection
        // The current connectionListener will be called when the ongoing connection completes
        if (isConnecting) {
            Log.d(TAG, "Already connecting to Spotify - connection listener will be notified when complete");
            return;
        }
        
        isConnecting = true;
        Log.d(TAG, "Attempting to connect to Spotify (attempt " + (connectionRetryCount + 1) + " of " + MAX_CONNECTION_RETRIES + ")");
        Log.d(TAG, "Using context: " + context.getClass().getName() + ", Package: " + context.getPackageName());
        
        Log.d(TAG, "Creating ConnectionParams with CLIENT_ID: " + CLIENT_ID + ", REDIRECT_URI: " + REDIRECT_URI);
        
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();
        
        Log.d(TAG, "Calling SpotifyAppRemote.connect()");
        
        SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                isConnecting = false;
                connectionRetryCount = 0; // Reset retry count on successful connection
                Log.d(TAG, "Successfully connected to Spotify AppRemote");
                
                // Subscribe to player state
                subscribeToPlayerState();
                
                if (connectionListener != null) {
                    Log.d(TAG, "Notifying connection listener of successful connection");
                    connectionListener.onConnected();
                } else {
                    Log.w(TAG, "No connection listener set");
                }
            }
            
            @Override
            public void onFailure(Throwable throwable) {
                isConnecting = false;
                Log.e(TAG, "Failed to connect to Spotify (attempt " + (connectionRetryCount + 1) + ") - Error: " + throwable.getMessage(), throwable);
                
                // Check the type of error
                String errorMessage = throwable.getMessage();
                boolean shouldRetry = true;
                
                // Check for specific error types that shouldn't be retried
                if (errorMessage != null) {
                    if (errorMessage.contains("CouldNotFindSpotifyApp") || 
                        errorMessage.contains("Spotify is not installed")) {
                        Log.e(TAG, "Spotify app not found - not retrying");
                        shouldRetry = false;
                    } else if (errorMessage.contains("UserNotAuthorizedException") ||
                               errorMessage.contains("not authorized")) {
                        Log.e(TAG, "User not authorized - will retry to show auth view");
                        // User needs to authorize, retry will show auth view
                    } else if (errorMessage.contains("OfflineException") ||
                               errorMessage.contains("offline")) {
                        Log.e(TAG, "Spotify is offline - will retry");
                    }
                }
                
                // Check if we should retry
                if (shouldRetry && connectionRetryCount < MAX_CONNECTION_RETRIES - 1) {
                    connectionRetryCount++;
                    Log.d(TAG, "Retrying connection in 2 seconds...");
                    
                    // Retry after a delay
                    retryHandler.postDelayed(() -> {
                        Log.d(TAG, "Retrying Spotify connection...");
                        connect();
                    }, 2000); // 2 second delay before retry
                } else {
                    // Max retries reached or non-retryable error, notify listener of failure
                    connectionRetryCount = 0; // Reset for next time
                    if (!shouldRetry) {
                        Log.e(TAG, "Non-retryable error occurred. Connection failed.");
                    } else {
                        Log.e(TAG, "Max connection retries reached. Connection failed.");
                    }
                    
                    if (connectionListener != null) {
                        connectionListener.onConnectionFailed(throwable);
                    }
                }
            }
        });
    }
    
    // Disconnect from Spotify
    public void disconnect() {
        if (mSpotifyAppRemote != null) {
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
            mSpotifyAppRemote = null;
            
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
        }
    }
    
    // Subscribe to player state changes
    private void subscribeToPlayerState() {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    if (playerListener != null) {
                        playerListener.onPlayerStateUpdated(playerState);
                        
                        Track track = playerState.track;
                        if (track != null) {
                            playerListener.onTrackChanged(track);
                        }
                    }
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "Error subscribing to player state", throwable);
                    if (playerListener != null) {
                        playerListener.onError(throwable.getMessage());
                    }
                });
    }
    
    // Play a track by Spotify URI
    public void playTrack(String spotifyUri) {
        if (!isConnected()) {
            Log.w(TAG, "Not connected to Spotify - track will be played after connection is established");
            // Don't try to connect here - let the controller handle the connection
            // The controller will call playTrack again after connection is established
            return;
        }
        
        // Convert track ID to Spotify URI if needed
        final String finalSpotifyUri;
        if (!spotifyUri.startsWith("spotify:track:")) {
            finalSpotifyUri = "spotify:track:" + spotifyUri;
        } else {
            finalSpotifyUri = spotifyUri;
        }
        
        Log.d(TAG, "Attempting to play track: " + finalSpotifyUri);
        
        mSpotifyAppRemote.getPlayerApi()
                .play(finalSpotifyUri)
                .setResultCallback(empty -> {
                    Log.d(TAG, "Successfully started playing track: " + finalSpotifyUri);
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "Error playing track: " + finalSpotifyUri, throwable);
                    if (playerListener != null) {
                        playerListener.onError("Failed to play track: " + throwable.getMessage());
                    }
                });
    }
    
    // Resume playback
    public void resume() {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .resume()
                .setResultCallback(empty -> Log.d(TAG, "Resumed playback"))
                .setErrorCallback(throwable -> Log.e(TAG, "Error resuming playback", throwable));
    }
    
    // Pause playback
    public void pause() {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .pause()
                .setResultCallback(empty -> Log.d(TAG, "Paused playback"))
                .setErrorCallback(throwable -> Log.e(TAG, "Error pausing playback", throwable));
    }
    
    // Skip to next track
    public void skipNext() {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .skipNext()
                .setResultCallback(empty -> Log.d(TAG, "Skipped to next track"))
                .setErrorCallback(throwable -> Log.e(TAG, "Error skipping to next", throwable));
    }
    
    // Skip to previous track
    public void skipPrevious() {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .skipPrevious()
                .setResultCallback(empty -> Log.d(TAG, "Skipped to previous track"))
                .setErrorCallback(throwable -> Log.e(TAG, "Error skipping to previous", throwable));
    }
    
    // Seek to position
    public void seekToPosition(long positionMs) {
        if (!isConnected()) return;
        
        mSpotifyAppRemote.getPlayerApi()
                .seekTo(positionMs)
                .setResultCallback(empty -> Log.d(TAG, "Seeked to position: " + positionMs))
                .setErrorCallback(throwable -> Log.e(TAG, "Error seeking", throwable));
    }
    
    // Get current player state
    public void getPlayerState(CallResult.ResultCallback<PlayerState> callback) {
        if (!isConnected()) {
            Log.w(TAG, "Not connected to Spotify");
            return;
        }
        
        mSpotifyAppRemote.getPlayerApi()
                .getPlayerState()
                .setResultCallback(callback)
                .setErrorCallback(throwable -> Log.e(TAG, "Error getting player state", throwable));
    }
    
    // Get album art for current track
    public void getAlbumArt(ImageUri imageUri, Image.Dimension dimension) {
        if (!isConnected() || imageUri == null) return;
        
        mSpotifyAppRemote.getImagesApi()
                .getImage(imageUri, dimension)
                .setResultCallback(bitmap -> {
                    if (playerListener != null) {
                        playerListener.onAlbumArtLoaded(bitmap);
                    }
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "Error loading album art", throwable);
                    if (playerListener != null) {
                        playerListener.onError("Failed to load album art");
                    }
                });
    }
    
    // Toggle play/pause
    public void togglePlayPause() {
        if (!isConnected()) return;
        
        getPlayerState(playerState -> {
            if (playerState.isPaused) {
                resume();
            } else {
                pause();
            }
        });
    }
}