#!/bin/bash

# Patch script for YouTube Bottom Sheet integration

echo "Adding YouTube Bottom Sheet integration..."

# 1. Add initialization in UtworyWykonawcy.java
sed -i '/spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);/a\\t\t\tyouTubeBottomSheetController = new YouTubeBottomSheetController(this);' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java

# 2. Replace teledysk handling in UtworyWykonawcy.java
sed -i '/} else if (wykItems\[item\] == getString(R.string.teledysk)) {/,/^[[:space:]]*} else if(wykItems\[item\]==getString(R.string.spotify)){/{
s/glosTeledysk = "1";/glosTeledysk = "0";/
/zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, glosTeledysk);/a\\t\t\t\t\t\t// Use YouTube Bottom Sheet instead of browser\n\t\t\t\t\t\tif (youTubeBottomSheetController != null) {\n\t\t\t\t\t\t\tyouTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);\n\t\t\t\t\t\t}
/try {/,/} catch (Exception e) {/d
/Log.e("VideoPlayback", "Error launching video: " + e.getMessage());/d
/^[[:space:]]*}$/d
}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java

# 3. Add onPause, onResume, onDestroy methods for YouTube controller
# First check if onPause exists
if ! grep -q "youTubeBottomSheetController.onPause()" /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java; then
    # Add to onPause
    sed -i '/spotifyBottomSheetController.onPause();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onPause();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java
    
    # Add to onResume
    sed -i '/spotifyBottomSheetController.onResume();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onResume();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java
    
    # Add to onDestroy
    sed -i '/spotifyBottomSheetController.onDestroy();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onDestroy();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/UtworyWykonawcy.java
fi

# 4. Do the same for ListaPrzebojowDiscoPolo.java
# Add declaration
if ! grep -q "private YouTubeBottomSheetController youTubeBottomSheetController;" /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java; then
    sed -i '/private SpotifyBottomSheetController spotifyBottomSheetController;/a\\tprivate YouTubeBottomSheetController youTubeBottomSheetController;' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java
fi

# Add initialization
sed -i '/spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);/a\\t\t\tyouTubeBottomSheetController = new YouTubeBottomSheetController(this);' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java

# Replace teledysk handling for RewardItems
sed -i '/} else if (RewardItems\[item\] == getString(R.string.teledysk)) {/,/^[[:space:]]*} else if (RewardItems\[item\] == getString(R.string.spotify)) {/{
s/glosTeledysk = "1";/glosTeledysk = "0";/
/zaglosuj(idListy, listType, null, glosTeledysk);/a\\t\t\t\t\t\t// Use YouTube Bottom Sheet instead of browser\n\t\t\t\t\t\tif (youTubeBottomSheetController != null) {\n\t\t\t\t\t\t\tyouTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);\n\t\t\t\t\t\t}
/Intent browserIntent/,/startActivity(browserIntent);/d
}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java

# Replace teledysk handling for items
sed -i '/} else if (items\[item\] == getString(R.string.teledysk)) {/,/^[[:space:]]*} else if (items\[item\] == getString(R.string.spotify)) {/{
s/glosTeledysk = "1";/glosTeledysk = "0";/
/zaglosuj(idListy, listType, null, glosTeledysk);/a\\t\t\t\t\t// Use YouTube Bottom Sheet instead of browser\n\t\t\t\t\tif (youTubeBottomSheetController != null) {\n\t\t\t\t\t\tyouTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);\n\t\t\t\t\t}
/Intent browserIntent/,/startActivity(browserIntent);/d
}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java

# Replace teledysk handling for wykItems
sed -i '/else if(wykItems\[item\]==getString(R.string.teledysk)){/,/^[[:space:]]*} else if(wykItems\[item\]==getString(R.string.spotify)){/{
s/glosTeledysk = "1";/glosTeledysk = "0";/
/zaglosuj(idListy, listType, idWykonawcy, glosTeledysk);/a\\t\t\t\t// Use YouTube Bottom Sheet instead of browser\n\t\t\t\tif (youTubeBottomSheetController != null) {\n\t\t\t\t\tyouTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);\n\t\t\t\t}
/Intent browserIntent/,/startActivity(browserIntent);/d
}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java

# Add lifecycle methods for ListaPrzebojowDiscoPolo
if ! grep -q "youTubeBottomSheetController.onPause()" /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java; then
    sed -i '/spotifyBottomSheetController.onPause();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onPause();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java
    
    sed -i '/spotifyBottomSheetController.onResume();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onResume();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java
    
    sed -i '/spotifyBottomSheetController.onDestroy();/a\\t\tif (youTubeBottomSheetController != null) {\n\t\t\tyouTubeBottomSheetController.onDestroy();\n\t\t}' /workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/ListaPrzebojowDiscoPolo.java
fi

echo "YouTube Bottom Sheet integration completed!"