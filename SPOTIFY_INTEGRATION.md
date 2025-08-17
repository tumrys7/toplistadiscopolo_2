# Spotify App Remote SDK Integration with Material 3 Bottom Sheet

## 📋 Overview
Implementation of Spotify App Remote SDK with Material Design 3 Bottom Sheet for the Disco Polo Music Chart application. This integration allows users to play music directly from Spotify while maintaining transparency and compliance with Spotify Developer Policy.

## 🎯 Key Features

### Spotify App Remote SDK Integration
- **Client ID**: `1ef55d5630814a3dafc946ef58e266b5`
- **Redirect URI**: `com.grandline.toplistadiscopolo://callback`
- Full playback control (play, pause, skip, seek)
- Real-time player state synchronization
- Album artwork loading with multiple dimensions

### Material 3 Bottom Sheet Design
Two-state responsive panel that doesn't interfere with app navigation:

#### Mini Player (Collapsed State - 72dp)
- Compact view with album art thumbnail
- Track title and artist display
- Play/pause control
- Close button
- Expandable on tap

#### Expanded Player (Full State)
- Large album artwork (300dp)
- Track title and artist
- Progress slider with time indicators
- Full playback controls (previous, play/pause, next)
- Spotify attribution
- Smooth animations and transitions

## 🏗️ Architecture

### Core Components

#### 1. SpotifyService (Singleton)
```java
SpotifyService.java
- Connection management
- Playback control API
- Player state subscriptions
- Album art fetching
- Error handling
```

#### 2. SpotifyBottomSheetController
```java
SpotifyBottomSheetController.java
- Bottom Sheet behavior management
- UI state synchronization
- Progress tracking
- User interaction handling
- Lifecycle management
```

#### 3. MainActivity Integration
```java
ListaPrzebojowDiscoPolo.java
- Bottom Sheet initialization
- Public API for track playback
- Context menu integration
- Cleanup on destroy
```

## 📱 User Flow

1. User clicks "Posłuchaj" (Play music) in song context menu
2. Bottom Sheet appears in expanded state
3. Spotify App Remote connects automatically (if not connected)
4. Track starts playing with full controls
5. User can:
   - Minimize to mini player
   - Control playback
   - Seek to any position
   - Close player
   - Continue browsing with mini player visible

## 🔧 Technical Implementation

### Dependencies Added
```gradle
implementation 'com.spotify.android:spotify-app-remote:0.8.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

### Layout Updates
- Added hidden `spotify` field to all list layouts:
  - `list_row.xml`
  - `list_row_moja.xml`
  - `list_row_nowosci.xml`
  - `utwory_wykonawcy.xml`

### Adapter Modifications
Updated adapters to handle Spotify track IDs:
- `LazyAdapter.java`
- `MojaAdapter.java`
- `NowosciAdapter.java`

### AndroidManifest Configuration
```xml
<!-- Spotify callback handling -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data 
        android:scheme="com.grandline.toplistadiscopolo"
        android:host="callback" />
</intent-filter>

<!-- Query for Spotify app -->
<queries>
    <package android:name="com.spotify.music" />
</queries>
```

## 🎨 UI Components

### Custom Drawables Created
- `ic_play_arrow.xml` - Play button icon
- `ic_pause.xml` - Pause button icon
- `ic_skip_next.xml` - Skip to next track
- `ic_skip_previous.xml` - Skip to previous track
- `ic_close.xml` - Close button
- `ic_spotify_logo.xml` - Spotify branding

### Color Resources
```xml
<color name="spotify_green">#1DB954</color>
```

### String Resources
```xml
<string name="album_art">Album Art</string>
<string name="no_track_playing">No track playing</string>
<string name="powered_by_spotify">Powered by Spotify</string>
```

## 📐 Design Specifications

### Bottom Sheet Behavior
- **Peek Height**: 72dp (mini player)
- **Expanded Height**: Wrap content
- **Hideable**: Yes
- **Skip Collapsed**: No
- **Elevation**: 8dp

### Layout Hierarchy
```
CoordinatorLayout (main.xml)
└── LinearLayout (content)
    ├── AdView (banner)
    ├── TabLayout
    └── ViewPager2
└── BottomSheet (dynamically added)
    ├── Mini Player
    └── Expanded Player
```

## ✅ Spotify Developer Policy Compliance

### Transparency
- Clear Spotify branding in player UI
- "Powered by Spotify" attribution
- Spotify green color for brand recognition

### App Purpose
- Music playback is supplementary feature
- Main purpose: voting charts and artist discovery
- Google Ads remain primary monetization
- No competition with Spotify's core service

### User Experience
- Seamless integration with existing UI
- Non-intrusive player controls
- Preserves app's primary functionality
- Optional feature (can be closed/hidden)

## 🚀 Usage

### For Developers
```java
// Play a Spotify track from anywhere in MainActivity
playSpotifyTrack(spotifyTrackId, title, artist);

// Access Bottom Sheet Controller
SpotifyBottomSheetController controller = getSpotifyBottomSheetController();
```

### For Users
1. Browse music charts
2. Long press on any song
3. Select "Posłuchaj" from menu
4. Enjoy music with full controls
5. Continue browsing with mini player

## 🔒 Security Considerations

- Client ID is embedded (required for SDK)
- No client secret in mobile app
- Authentication handled by Spotify app
- User tokens managed by SDK
- No sensitive data stored locally

## 📊 Performance

- Lazy loading of album artwork
- Asynchronous API calls
- Progress updates throttled to 1 second
- Efficient bitmap caching
- Minimal memory footprint

## 🐛 Error Handling

- Graceful fallback when Spotify not installed
- Connection retry logic
- Player state validation
- Null safety checks
- User-friendly error messages

## 📝 Future Enhancements

Potential improvements for future versions:
- [ ] Queue management
- [ ] Playlist integration
- [ ] Shuffle and repeat modes
- [ ] Volume control
- [ ] Lyrics display
- [ ] Share functionality
- [ ] Recently played history
- [ ] Offline mode detection

## 📄 License & Attribution

This implementation complies with:
- Spotify Developer Terms of Service
- Spotify Design Guidelines
- Android App Development Best Practices
- Material Design 3 Specifications

## 🤝 Credits

Developed for **Disco Polo Music Chart** by Grand Line
- Integration Design & Implementation: 2024
- Spotify SDK Version: 0.8.0
- Material Design Version: 1.12.0

---

*This feature enhances user experience by providing seamless music playback while maintaining the app's core functionality of chart voting and artist discovery.*