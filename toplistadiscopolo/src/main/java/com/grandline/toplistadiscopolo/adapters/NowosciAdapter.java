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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ImageLoader;
import com.grandline.toplistadiscopolo.R;

public class NowosciAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> originalData;
    private ArrayList<HashMap<String, String>> workingData;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;
    private final Handler mainHandler;
    private final Object dataLock = new Object(); // Synchronization lock for data access
    private volatile int cachedSize; // Cache size to prevent inconsistencies
    
    public NowosciAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        originalData = d;
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
     * Safe version of notifyDataSetChanged() that includes null checks and synchronization
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

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row_nowosci, parent, false);

        TextView id_listy = vi.findViewById(R.id.id_listy); // id_listy
        TextView title = vi.findViewById(R.id.title); // title
        TextView artist = vi.findViewById(R.id.artist); // artist name
        TextView duration = vi.findViewById(R.id.ile_glosow); // duration
        ImageView thumb_image= vi.findViewById(R.id.list_image); // thumb image
        TextView createDate = vi.findViewById(R.id.data_dodania); // listPosition
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
//        listPosition.setText(song.get(Constants.KEY_POSITION));
        createDate.setText(song.get(Constants.KEY_CREATE_DATE));
        spotify.setText(song.get(Constants.KEY_SPOTIFY));
        
        // Load image asynchronously to prevent main thread blocking
        String thumbUrl = song.get(Constants.KEY_THUMB_URL);
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            imageLoader.DisplayImage(thumbUrl, thumb_image);
        } else {
            thumb_image.setImageResource(R.drawable.ic_launcher);
        }
        
        return vi;
    }
}