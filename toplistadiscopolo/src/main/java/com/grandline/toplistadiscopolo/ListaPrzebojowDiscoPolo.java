		// Initialize Spotify Bottom Sheet Controller
		ViewGroup rootView = findViewById(R.id.root);
		if (rootView != null) {
			spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);
			youTubeBottomSheetController = new YouTubeBottomSheetController(this, rootView);
		}