package com.grandline.toplistadiscopolo.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ImageLoader;
import com.grandline.toplistadiscopolo.R;

public class WykAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;

    
    public WykAdapter(Activity a, ArrayList<HashMap<String, String>> d) {

        data=d;
        inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoader(a.getApplicationContext());
    }

    public int getCount() {
        synchronized(data) {
            return data.size();
        }
    }

    public Object getItem(int position) {
        synchronized(data) {
            if (position >= 0 && position < data.size()) {
                return data.get(position);
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

        HashMap<String, String> song;
        synchronized(data) {
            // Check bounds to prevent IndexOutOfBoundsException
            if (position >= 0 && position < data.size()) {
                song = data.get(position);
            } else {
                // Return empty view if position is invalid
                return vi;
            }
        }
        
        // Setting all values in listview
        id_wykonawcy.setText(song.get(Constants.KEY_ID_WYKON));
        artist.setText(song.get(Constants.KEY_WYKONAWCA));
        return vi;
    }
}