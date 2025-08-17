package com.grandline.toplistadiscopolo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class SpotifyBottomSheetController implements SpotifyService.SpotifyPlayerListener {
    private static final String TAG = "SpotifyBottomSheet";
    
    private Context context;
    private ViewGroup rootView;
    private CoordinatorLayout bottomSheetContainer;
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private SpotifyService spotifyService;
    
    // UI Elements - Mini Player
    private LinearLayout miniPlayer;
    private ImageView miniAlbumArt;
    private TextView miniTrackTitle;
    private TextView miniTrackArtist;
    private MaterialButton miniPlayPauseButton;
    private MaterialButton miniCloseButton;
    private ProgressBar loadingIndicator;
    
    // UI Elements - Expanded Player
    private LinearLayout expandedPlayer;
    private ImageView expandedAlbumArt;
    private TextView expandedTrackTitle;
    private TextView expandedTrackArtist;
    private Slider seekBar;
    private TextView currentTime;
    private TextView totalTime;
    private MaterialButton previousButton;
    private FloatingActionButton playPauseButton;
    private MaterialButton nextButton;
    
    // State
    private boolean isPlaying = false;
    private long trackDuration = 0;
    private long currentPosition = 0;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private String currentTrackId;
    private String currentTrackTitle;
    private String currentTrackArtist;
    
    // Track to play after connection
    private String pendingTrackId;
    private String pendingTrackTitle;
    private String pendingTrackArtist;
    private boolean hasPendingTrack = false;
    
    public SpotifyBottomSheetController(Context context, ViewGroup rootView) {
        this.context = context;
        this.rootView = rootView;
        this.spotifyService = SpotifyService.getInstance(context);
        
        initializeBottomSheet();
        setupListeners();
    }
    
    private void initializeBottomSheet() {
        Log.d(TAG, "Initializing bottom sheet");
        
        try {
            // Check if bottom sheet already exists in the view hierarchy
            View existingBottomSheet = rootView.findViewById(R.id.spotify_bottom_sheet);
            if (existingBottomSheet != null) {
                Log.d(TAG, "Bottom sheet already exists, using existing view");
                bottomSheet = (LinearLayout) existingBottomSheet;
            } else {
                // Inflate the bottom sheet layout directly (without CoordinatorLayout wrapper)
                LayoutInflater inflater = LayoutInflater.from(context);
                bottomSheet = (LinearLayout) inflater.inflate(R.layout.spotify_bottom_sheet_content, rootView, false);
                
                // Add the bottom sheet to the root CoordinatorLayout
                rootView.addView(bottomSheet);
                Log.d(TAG, "Bottom sheet inflated and added to view hierarchy");
            }
            
            // For compatibility, set bottomSheetContainer to rootView since we're not using a separate container
            bottomSheetContainer = (CoordinatorLayout) rootView;
            
            // Get the BottomSheetBehavior from the bottom sheet
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            
            // Configure bottom sheet behavior
            bottomSheetBehavior.setPeekHeight(0); // Initially hidden
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setSkipCollapsed(false);
            
            Log.d(TAG, "Bottom sheet behavior configured - state: " + getStateString(bottomSheetBehavior.getState()));
            
            // Initialize UI elements
            initializeMiniPlayer();
            initializeExpandedPlayer();
            
            // Set Spotify listener
            spotifyService.setPlayerListener(this);
            
            Log.d(TAG, "Bottom sheet initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing bottom sheet", e);
        }
    }
    
    private void initializeMiniPlayer() {
        miniPlayer = bottomSheet.findViewById(R.id.mini_player);
        miniAlbumArt = bottomSheet.findViewById(R.id.mini_album_art);
        miniTrackTitle = bottomSheet.findViewById(R.id.mini_track_title);
        miniTrackArtist = bottomSheet.findViewById(R.id.mini_track_artist);
        miniPlayPauseButton = bottomSheet.findViewById(R.id.mini_play_pause_button);
        miniCloseButton = bottomSheet.findViewById(R.id.mini_close_button);
        loadingIndicator = bottomSheet.findViewById(R.id.loading_indicator);
        
        // Log which views were found
        Log.d(TAG, "Mini player views initialized: " +
            "miniPlayer=" + (miniPlayer != null) +
            ", miniAlbumArt=" + (miniAlbumArt != null) +
            ", miniTrackTitle=" + (miniTrackTitle != null) +
            ", miniTrackArtist=" + (miniTrackArtist != null) +
            ", miniPlayPauseButton=" + (miniPlayPauseButton != null) +
            ", miniCloseButton=" + (miniCloseButton != null) +
            ", loadingIndicator=" + (loadingIndicator != null));
    }
    
    private void initializeExpandedPlayer() {
        expandedPlayer = bottomSheet.findViewById(R.id.expanded_player);
        expandedAlbumArt = bottomSheet.findViewById(R.id.expanded_album_art);
        expandedTrackTitle = bottomSheet.findViewById(R.id.expanded_track_title);
        expandedTrackArtist = bottomSheet.findViewById(R.id.expanded_track_artist);
        seekBar = bottomSheet.findViewById(R.id.seek_bar);
        currentTime = bottomSheet.findViewById(R.id.current_time);
        totalTime = bottomSheet.findViewById(R.id.total_time);
        previousButton = bottomSheet.findViewById(R.id.previous_button);
        playPauseButton = bottomSheet.findViewById(R.id.play_pause_button);
        nextButton = bottomSheet.findViewById(R.id.next_button);
    }
    
    private void setupListeners() {
        // Mini player click to expand
        miniPlayer.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        
        // Play/Pause buttons
        miniPlayPauseButton.setOnClickListener(v -> togglePlayPause());
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        
        // Close button
        miniCloseButton.setOnClickListener(v -> hideBottomSheet());
        
        // Previous/Next buttons
        previousButton.setOnClickListener(v -> spotifyService.skipPrevious());
        nextButton.setOnClickListener(v -> spotifyService.skipNext());
        
        // Seek bar
        seekBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // Stop updating progress while user is seeking
                stopProgressUpdate();
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // Seek to position
                long position = (long) (slider.getValue() * trackDuration / 100);
                spotifyService.seekToPosition(position);
                // Resume progress updates
                startProgressUpdate();
            }
        });
        
        // Bottom sheet state callback
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // Load large album art when expanded
                        if (currentTrackId != null) {
                            loadAlbumArt(true);
                        }
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        // Stop progress updates when hidden
                        stopProgressUpdate();
                        break;
                }
            }
            
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: Add animations based on slide offset
            }
        });
    }
    
    // Public methods
    
    public void playTrack(String trackId, String title, String artist) {
        Log.d(TAG, "playTrack called - trackId: " + trackId + ", title: " + title + ", artist: " + artist);
        
        // Extract track ID from Spotify URL if needed
        String actualTrackId = extractSpotifyTrackId(trackId);
        Log.d(TAG, "Extracted track ID: " + actualTrackId);
        
        // Store current track info
        currentTrackId = actualTrackId;
        currentTrackTitle = title;
        currentTrackArtist = artist;
        
        // Update UI with track info immediately
        updateTrackInfo(title, artist);
        
        // Show the bottom sheet
        showBottomSheet(true);
        
        // Check if already connected
        if (spotifyService.isConnected()) {
            Log.d(TAG, "Already connected to Spotify, playing track immediately");
            spotifyService.playTrack(actualTrackId);
        } else {
            Log.d(TAG, "Spotify not connected, setting up connection");
            
            // Store pending track info
            pendingTrackId = actualTrackId;
            pendingTrackTitle = title;
            pendingTrackArtist = artist;
            hasPendingTrack = true;
            
            // Show loading state
            showLoadingState(true);
            
            // Create a connection listener specifically for this playback request
            SpotifyService.SpotifyConnectionListener playbackConnectionListener = new SpotifyService.SpotifyConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "Spotify connected callback - checking for pending track");
                    // Hide loading state
                    showLoadingState(false);
                    
                    // Play the pending track if we have one
                    if (hasPendingTrack && pendingTrackId != null) {
                        Log.d(TAG, "Playing pending track: " + pendingTrackId);
                        spotifyService.playTrack(pendingTrackId);
                        hasPendingTrack = false;
                        pendingTrackId = null;
                        pendingTrackTitle = null;
                        pendingTrackArtist = null;
                    }
                    
                    // Remove this listener after use
                    spotifyService.removeConnectionListener(this);
                }
                
                @Override
                public void onConnectionFailed(Throwable error) {
                    Log.e(TAG, "Failed to connect to Spotify", error);
                    // Hide loading state and show error
                    showLoadingState(false);
                    hasPendingTrack = false;
                    
                    // Keep the track info displayed even if connection failed
                    // This way users can still see what track they tried to play
                    
                    // Show appropriate error message to the user
                    String errorMessage = error != null ? error.getMessage() : "Unknown error";
                    if (errorMessage.contains("not installed") || errorMessage.contains("CouldNotFindSpotifyApp")) {
                        updateTrackInfo("Spotify Not Installed", "Install Spotify app to play: " + (pendingTrackTitle != null ? pendingTrackTitle : "this track"));
                    } else if (errorMessage.contains("UserNotAuthorizedException") || errorMessage.contains("not authorized")) {
                        updateTrackInfo("Authorization Required", "Please authorize the app in Spotify");
                    } else if (errorMessage.contains("OfflineException") || errorMessage.contains("offline")) {
                        updateTrackInfo("Spotify Offline", "Please check your connection and try again");
                    } else {
                        updateTrackInfo("Connection Failed", "Unable to connect. Track: " + (pendingTrackTitle != null ? pendingTrackTitle : ""));
                    }
                    
                    // Clear pending track data after displaying the error
                    pendingTrackId = null;
                    pendingTrackTitle = null;
                    pendingTrackArtist = null;
                    
                    // Remove this listener after use
                    spotifyService.removeConnectionListener(this);
                }
                
                @Override
                public void onDisconnected() {
                    Log.d(TAG, "Spotify disconnected");
                    // Handle disconnection
                    showLoadingState(false);
                    hasPendingTrack = false;
                    pendingTrackId = null;
                    pendingTrackTitle = null;
                    pendingTrackArtist = null;
                    
                    // Remove this listener after use
                    spotifyService.removeConnectionListener(this);
                }
            };
            
            // Add the connection listener
            Log.d(TAG, "Setting up connection listener for pending track");
            spotifyService.addConnectionListener(playbackConnectionListener);
            
            // Check if we need to start a new connection
            if (!spotifyService.isConnecting()) {
                Log.d(TAG, "Starting new Spotify connection");
                spotifyService.connect();
            } else {
                Log.d(TAG, "Spotify is already connecting, track will be played when connection completes");
            }
        }
    }
    
    public void showBottomSheet(boolean expanded) {
        Log.d(TAG, "showBottomSheet called - expanded: " + expanded);
        
        if (bottomSheetBehavior == null) {
            Log.e(TAG, "BottomSheetBehavior is null!");
            return;
        }
        
        if (bottomSheet == null) {
            Log.e(TAG, "BottomSheet is null!");
            return;
        }
        
        // Ensure the bottom sheet is visible
        bottomSheet.setVisibility(View.VISIBLE);
        
        // Force the bottom sheet to the front
        bottomSheet.bringToFront();
        
        // Request focus to ensure it's interactive
        bottomSheet.requestFocus();
        
        // Set peek height before changing state
        bottomSheetBehavior.setPeekHeight(dpToPx(72));
        
        // Force layout update to ensure the bottom sheet is properly measured
        bottomSheet.requestLayout();
        
        // Use post to ensure the state change happens after any pending UI operations
        bottomSheet.post(() -> {
            try {
                if (expanded) {
                    // First make sure it's not hidden
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        // Then expand after a short delay
                        bottomSheet.postDelayed(() -> {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            Log.d(TAG, "Bottom sheet set to EXPANDED state");
                        }, 100);
                    } else {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        Log.d(TAG, "Bottom sheet set to EXPANDED state");
                    }
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    Log.d(TAG, "Bottom sheet set to COLLAPSED state");
                }
                
                // Log the current state for debugging
                Log.d(TAG, "Bottom sheet current state: " + getStateString(bottomSheetBehavior.getState()));
            } catch (Exception e) {
                Log.e(TAG, "Error setting bottom sheet state", e);
            }
        });
    }
    
    public void hideBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        stopProgressUpdate();
        // Optionally pause playback
        if (isPlaying) {
            spotifyService.pause();
        }
    }
    
    public boolean isBottomSheetVisible() {
        return bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
    }
    
    // SpotifyPlayerListener implementation
    
    @Override
    public void onPlayerStateUpdated(PlayerState playerState) {
        if (playerState == null) return;
        
        isPlaying = !playerState.isPaused;
        currentPosition = playerState.playbackPosition;
        
        // Update play/pause button icons
        updatePlayPauseButtons(isPlaying);
        
        // Update progress
        if (playerState.track != null) {
            trackDuration = playerState.track.duration;
            updateProgress(currentPosition, trackDuration);
            
            if (isPlaying) {
                startProgressUpdate();
            } else {
                stopProgressUpdate();
            }
        }
    }
    
    @Override
    public void onTrackChanged(Track track) {
        if (track == null) return;
        
        currentTrackId = track.uri.split(":")[2]; // Extract track ID from URI
        currentTrackTitle = track.name;
        currentTrackArtist = track.artist.name;
        trackDuration = track.duration;
        
        // Update UI
        updateTrackInfo(track.name, track.artist.name);
        
        // Load album art
        if (track.imageUri != null) {
            spotifyService.getAlbumArt(track.imageUri, Image.Dimension.THUMBNAIL);
        }
    }
    
    @Override
    public void onAlbumArtLoaded(Bitmap bitmap) {
        if (bitmap != null) {
            miniAlbumArt.setImageBitmap(bitmap);
            expandedAlbumArt.setImageBitmap(bitmap);
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Spotify error: " + error);
    }
    
    // Private helper methods
    
    private void togglePlayPause() {
        if (isPlaying) {
            spotifyService.pause();
        } else {
            spotifyService.resume();
        }
    }
    
    private void updateTrackInfo(String title, String artist) {
        Log.d(TAG, "Updating track info - Title: " + title + ", Artist: " + artist);
        
        String displayTitle = title != null ? title : context.getString(R.string.no_track_playing);
        String displayArtist = artist != null ? artist : "";
        
        if (miniTrackTitle != null) {
            miniTrackTitle.setText(displayTitle);
        } else {
            Log.w(TAG, "miniTrackTitle is null!");
        }
        
        if (miniTrackArtist != null) {
            miniTrackArtist.setText(displayArtist);
        } else {
            Log.w(TAG, "miniTrackArtist is null!");
        }
        
        if (expandedTrackTitle != null) {
            expandedTrackTitle.setText(displayTitle);
        } else {
            Log.w(TAG, "expandedTrackTitle is null!");
        }
        
        if (expandedTrackArtist != null) {
            expandedTrackArtist.setText(displayArtist);
        } else {
            Log.w(TAG, "expandedTrackArtist is null!");
        }
    }
    
    private void updatePlayPauseButtons(boolean playing) {
        int iconRes = playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        miniPlayPauseButton.setIconResource(iconRes);
        playPauseButton.setImageResource(iconRes);
    }
    
    private void updateProgress(long position, long duration) {
        if (duration > 0) {
            float progress = (float) position / duration * 100;
            seekBar.setValue(progress);
            currentTime.setText(formatTime(position));
            totalTime.setText(formatTime(duration));
        }
    }
    
    private void startProgressUpdate() {
        stopProgressUpdate();
        
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    currentPosition += 1000; // Update every second
                    updateProgress(currentPosition, trackDuration);
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.postDelayed(progressRunnable, 1000);
    }
    
    private void stopProgressUpdate() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }
    
    private void loadAlbumArt(boolean large) {
        spotifyService.getPlayerState(playerState -> {
            if (playerState != null && playerState.track != null && playerState.track.imageUri != null) {
                Image.Dimension dimension = large ? Image.Dimension.LARGE : Image.Dimension.THUMBNAIL;
                spotifyService.getAlbumArt(playerState.track.imageUri, dimension);
            }
        });
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void showLoadingState(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        // Disable playback controls while loading
        if (miniPlayPauseButton != null) {
            miniPlayPauseButton.setEnabled(!show);
        }
        if (playPauseButton != null) {
            playPauseButton.setEnabled(!show);
        }
        if (previousButton != null) {
            previousButton.setEnabled(!show);
        }
        if (nextButton != null) {
            nextButton.setEnabled(!show);
        }
        if (seekBar != null) {
            seekBar.setEnabled(!show);
        }
        
        // Update text to show loading state
        if (show && currentTrackTitle == null) {
            updateTrackInfo(context.getString(R.string.connecting_spotify), "");
        }
    }
    
    /**
     * Extracts the Spotify track ID from various URL formats
     * Supports:
     * - Plain track ID: "2LxgXxai3bBNcIeiQxb9PL"
     * - Spotify URI: "spotify:track:2LxgXxai3bBNcIeiQxb9PL"
     * - Open URL: "https://open.spotify.com/track/2LxgXxai3bBNcIeiQxb9PL"
     * - Embed URL: "https://open.spotify.com/embed/track/2LxgXxai3bBNcIeiQxb9PL?theme=0"
     */
    private String extractSpotifyTrackId(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Remove any query parameters first
        String cleanInput = input.split("\\?")[0];
        
        // Check if it's already a plain track ID (22 characters)
        if (cleanInput.matches("[a-zA-Z0-9]{22}")) {
            return cleanInput;
        }
        
        // Check for Spotify URI format
        if (cleanInput.startsWith("spotify:track:")) {
            return cleanInput.substring("spotify:track:".length());
        }
        
        // Check for Open Spotify URL format
        if (cleanInput.contains("open.spotify.com/track/")) {
            String[] parts = cleanInput.split("/track/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        
        // Check for Embed Spotify URL format
        if (cleanInput.contains("open.spotify.com/embed/track/")) {
            String[] parts = cleanInput.split("/embed/track/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        
        // If no pattern matches, return the original input
        Log.w(TAG, "Could not extract track ID from: " + input);
        return cleanInput;
    }
    
    private String getStateString(int state) {
        switch (state) {
            case BottomSheetBehavior.STATE_EXPANDED:
                return "EXPANDED";
            case BottomSheetBehavior.STATE_COLLAPSED:
                return "COLLAPSED";
            case BottomSheetBehavior.STATE_HIDDEN:
                return "HIDDEN";
            case BottomSheetBehavior.STATE_DRAGGING:
                return "DRAGGING";
            case BottomSheetBehavior.STATE_SETTLING:
                return "SETTLING";
            case BottomSheetBehavior.STATE_HALF_EXPANDED:
                return "HALF_EXPANDED";
            default:
                return "UNKNOWN";
        }
    }
    
    // Cleanup
    public void onDestroy() {
        stopProgressUpdate();
        spotifyService.disconnect();
    }
}