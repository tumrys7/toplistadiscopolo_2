package com.grandline.toplistadiscopolo.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ImageLoader;
import com.grandline.toplistadiscopolo.R;

import java.util.ArrayList;
import java.util.HashMap;

public class WykAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> originalData;
    private ArrayList<HashMap<String, String>> workingData;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;
    private final Handler mainHandler;
    private final Object dataLock = new Object(); // Synchronization lock for data access
    private volatile int cachedSize; // Cache size to prevent inconsistencies
    
    public WykAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.wykon_row, parent, false);

        TextView id_wykonawcy = vi.findViewById(R.id.id_wykonawcy); // id wykonawcy
        TextView artist = vi.findViewById(R.id.artist); // artist name

        HashMap<String, String> wyk;
        synchronized(dataLock) {
            // Double-check bounds with both working data and cached size
            if (workingData == null || position < 0 || position >= workingData.size() || position >= cachedSize) {
                // Return empty view if data is invalid or position is out of bounds
                return vi;
            }
            wyk = workingData.get(position);
        }
        
        if (wyk == null) {
            return vi;
        }
        
        // Setting all values in listview
        id_wykonawcy.setText(wyk.get(Constants.KEY_ID_WYKON));
        artist.setText(wyk.get(Constants.KEY_WYKONAWCA));
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
     * Safely notify data set changed on the main UI thread
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