package com.grandline.toplistadiscopolo.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ImageLoader;
import com.grandline.toplistadiscopolo.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LazyAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> originalData;
    private ArrayList<HashMap<String, String>> workingData;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;
    private final Handler mainHandler;
    private final Object dataLock = new Object(); // Synchronization lock for data access
    private volatile int cachedSize; // Cache size to prevent inconsistencies
    private final Context context;
    
    public LazyAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        originalData = d;
        context = a;
        synchronized(dataLock) {
            // Create a defensive copy of the data to prevent external modifications
            workingData = d != null ? new ArrayList<>(d) : new ArrayList<>();
            cachedSize = workingData.size();
        }
        inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoader(a.getApplicationContext());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public int getCount() {
        synchronized(dataLock) {
            return cachedSize;
        }
    }

    public Object getItem(int position) {
        synchronized(dataLock) {
            if (workingData != null && position >= 0 && position < workingData.size() && position < cachedSize) {
                return workingData.get(position);
            }
            return null;
        }
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row, parent, false);

        TextView id_listy = vi.findViewById(R.id.id_listy); // id_listy
        TextView title = vi.findViewById(R.id.title); // title
        TextView artist = vi.findViewById(R.id.artist); // artist name
        TextView duration = vi.findViewById(R.id.ile_glosow); // duration
        ImageView thumb_image= vi.findViewById(R.id.list_image); // thumb image
        ImageView arrow_image= vi.findViewById(R.id.imageView1); // arrow image
        TextView listPosition = vi.findViewById(R.id.miejsce); // listPosition
        TextView createDate = vi.findViewById(R.id.data_dodania); // listPosition
        TextView placeChange = vi.findViewById(R.id.zmiana); // zmiana
        ProgressBar votesProgress = vi.findViewById(R.id.votesProgress); // progress
        TextView spotify = vi.findViewById(R.id.spotify); // spotify track id
        
        HashMap<String, String> song;
        synchronized(dataLock) {
            // Double-check bounds with both working data and cached size
            if (workingData == null || position < 0 || position >= workingData.size() || position >= cachedSize) {
                // Return empty view if data is invalid or position is out of bounds
                return vi;
            }
            song = workingData.get(position);
        }
        
        if (song == null) {
            return vi;
        }
        
        // Setting all values in listview
        id_listy.setText(song.get(Constants.KEY_ID));
        title.setText(song.get(Constants.KEY_TITLE));
        artist.setText(song.get(Constants.KEY_ARTIST));
        duration.setText(song.get(Constants.KEY_VOTES));
        listPosition.setText(song.get(Constants.KEY_POSITION));
        createDate.setText(song.get(Constants.KEY_CREATE_DATE));
        spotify.setText(song.get(Constants.KEY_SPOTIFY));
        
        // Load image asynchronously to prevent main thread blocking
        String thumbUrl = song.get(Constants.KEY_THUMB_URL);
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            imageLoader.DisplayImage(thumbUrl, thumb_image);
        } else {
            thumb_image.setImageResource(R.drawable.ic_launcher);
        }
        
        if (Objects.equals(song.get(Constants.KEY_ARROW_TYPE), Constants.KEY_ARROW_UP)) {
        	arrow_image.setImageResource(R.drawable.arrow_up);
        } else if (Objects.equals(song.get(Constants.KEY_ARROW_TYPE), Constants.KEY_ARROW_DOWN)) {
        	arrow_image.setImageResource(R.drawable.arrow_down);
        } else if (Objects.equals(song.get(Constants.KEY_ARROW_TYPE), Constants.KEY_ARROW_NO_CHANGE)) {
        	arrow_image.setImageResource(R.drawable.arrow_no_change);
        } else {
        	arrow_image.setImageResource(R.drawable.arrow);
        }
        
        // Set place change text
        String placeChangeText = song.get(Constants.KEY_PLACE_CHANGE);
        placeChange.setText(placeChangeText);
        
        // Set color based on text content
        if (placeChangeText != null) {
            if (placeChangeText.equals("Nowość") || placeChangeText.contains("Nowość")) {
                // Text is "Nowość" - set blue color
                placeChange.setTextColor(ContextCompat.getColor(context, R.color.text_nowosc_color));
            } else if (placeChangeText.startsWith("↑")) {
                // Text starts with ↑ (text_awans) - set green color
                placeChange.setTextColor(ContextCompat.getColor(context, R.color.text_awans_color));
            } else if (placeChangeText.startsWith("↓")) {
                // Text starts with ↓ (text_spadek) - set red color
                placeChange.setTextColor(ContextCompat.getColor(context, R.color.text_spadek_color));
            } else {
                // Default color for other cases
                placeChange.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimary));
            }
        }
        
        if (Objects.equals(song.get(Constants.KEY_SHOW_VOTES_PROGRESS), "TRUE")){
        	votesProgress.setVisibility(View.VISIBLE);//visible  setVisibility(0)
        	//votesProgress.setProgressDrawable((R.drawable.gradient_bg_hover));
	        if (song.get(Constants.KEY_VOTES_PROGRESS) != null){
	        	votesProgress.setProgress(Integer.parseInt(Objects.requireNonNull(song.get(Constants.KEY_VOTES_PROGRESS))));
	        }
        } else {
        	votesProgress.setVisibility(View.INVISIBLE);//invisible
        }
        return vi;
    }
    
    /**
     * Update the adapter's working data from the original data source
     * This should be called before notifying data set changed
     */
    public void refreshDataFromSource() {
        synchronized(dataLock) {
            if (originalData != null) {
                // Create a new defensive copy
                workingData = new ArrayList<>(originalData);
                cachedSize = workingData.size();
            } else {
                workingData = new ArrayList<>();
                cachedSize = 0;
            }
        }
    }
    
    /**
     * Safely notify data set changed on the main UI thread with additional validation
     */
    public void safeNotifyDataSetChanged() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread - refresh data and validate before notifying
            refreshDataFromSource();
            synchronized(dataLock) {
                if (workingData != null) {
                    notifyDataSetChanged();
                }
            }
        } else {
            // Post to main thread with data refresh and validation
            mainHandler.post(() -> {
                refreshDataFromSource();
                synchronized(dataLock) {
                    if (workingData != null) {
                        notifyDataSetChanged();
                    }
                }
            });
        }
    }
    

}