		CoordinatorLayout rootView = findViewById(R.id.root);
		if (rootView != null) {
			spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);
			youTubeBottomSheetController = new YouTubeBottomSheetController(this, rootView);
		}