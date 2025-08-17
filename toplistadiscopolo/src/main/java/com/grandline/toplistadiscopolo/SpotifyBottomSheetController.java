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
            // Inflate the bottom sheet layout directly (without CoordinatorLayout wrapper)
            LayoutInflater inflater = LayoutInflater.from(context);
            bottomSheet = (LinearLayout) inflater.inflate(R.layout.spotify_bottom_sheet_content, rootView, false);
            
            // Add the bottom sheet to the root CoordinatorLayout
            rootView.addView(bottomSheet);
            
            // For compatibility, set bottomSheetContainer to rootView since we're not using a separate container
            bottomSheetContainer = (CoordinatorLayout) rootView;
            
            // Get the BottomSheetBehavior from the bottom sheet
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            
            // Configure bottom sheet behavior
            bottomSheetBehavior.setPeekHeight(0); // Initially hidden
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setSkipCollapsed(false);
            
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
    
    public void playTrack(String spotifyTrackId, String title, String artist) {
        Log.d(TAG, "playTrack called - trackId: " + spotifyTrackId + ", title: " + title + ", artist: " + artist);
        
        // Store track info
        this.currentTrackId = spotifyTrackId;
        this.currentTrackTitle = title;
        this.currentTrackArtist = artist;
        
        // Update UI immediately
        updateTrackInfo(title, artist);
        
        // Show bottom sheet immediately for user feedback
        showBottomSheet(true);
        
        // Check if already connected
        if (spotifyService.isConnected()) {
            Log.d(TAG, "Spotify already connected, playing track immediately");
            // Already connected, play immediately
            spotifyService.playTrack(spotifyTrackId);
        } else {
            Log.d(TAG, "Spotify not connected, setting up connection listener");
            
            // Show loading state while connecting
            showLoadingState(true);
            
            // Set up connection listener for when connection completes
            // This will be called even if connection is already in progress
            spotifyService.setConnectionListener(new SpotifyService.SpotifyConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "Spotify connected callback - playing track: " + spotifyTrackId);
                    // Hide loading state
                    showLoadingState(false);
                    // Play track after connection
                    spotifyService.playTrack(spotifyTrackId);
                }
                
                @Override
                public void onConnectionFailed(Throwable error) {
                    Log.e(TAG, "Failed to connect to Spotify", error);
                    // Hide loading state and show error
                    showLoadingState(false);
                    // Optionally show an error message to the user
                    updateTrackInfo(context.getString(R.string.connection_failed), error.getMessage());
                }
                
                @Override
                public void onDisconnected() {
                    Log.d(TAG, "Spotify disconnected");
                    // Handle disconnection
                    showLoadingState(false);
                }
            });
            
            // If already connecting, the listener above will be called when connection completes
            // If not connecting, this will start a new connection
            if (spotifyService.isConnecting()) {
                Log.d(TAG, "Spotify is already connecting, waiting for connection to complete");
                // The listener we just set will be called when the ongoing connection completes
            } else {
                Log.d(TAG, "Starting new Spotify connection");
                spotifyService.connect();
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
        
        if (expanded) {
            bottomSheetBehavior.setPeekHeight(dpToPx(72));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            Log.d(TAG, "Bottom sheet set to EXPANDED state");
        } else {
            bottomSheetBehavior.setPeekHeight(dpToPx(72));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            Log.d(TAG, "Bottom sheet set to COLLAPSED state");
        }
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
        miniTrackTitle.setText(title != null ? title : context.getString(R.string.no_track_playing));
        miniTrackArtist.setText(artist != null ? artist : "");
        expandedTrackTitle.setText(title != null ? title : context.getString(R.string.no_track_playing));
        expandedTrackArtist.setText(artist != null ? artist : "");
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
    
    // Cleanup
    public void onDestroy() {
        stopProgressUpdate();
        spotifyService.disconnect();
    }
}