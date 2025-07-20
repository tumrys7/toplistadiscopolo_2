package com.grandline.toplistadiscopolo.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ImageLoader;
import com.grandline.toplistadiscopolo.R;

public class MojaAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader; 
    
    public MojaAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
            vi = inflater.inflate(R.layout.list_row_moja, parent, false);
        TextView id_listy = vi.findViewById(R.id.id_listy); // id_listy
        TextView title = vi.findViewById(R.id.title); // title
        TextView artist = vi.findViewById(R.id.artist); // artist name
        TextView duration = vi.findViewById(R.id.ile_glosow); // duration
        ImageView thumb_image = vi.findViewById(R.id.list_image); // thumb image
        TextView listPosition = vi.findViewById(R.id.miejsce); // listPosition
        TextView createDate = vi.findViewById(R.id.data_dodania); // createDate
        ProgressBar votesProgress = vi.findViewById(R.id.votesProgress); // progres

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
        id_listy.setText(song.get(Constants.KEY_ID));
        title.setText(song.get(Constants.KEY_TITLE));
        artist.setText(song.get(Constants.KEY_ARTIST));
        duration.setText(song.get(Constants.KEY_VOTES));
        listPosition.setText(song.get(Constants.KEY_POSITION));
        createDate.setText(song.get(Constants.KEY_CREATE_DATE));
        imageLoader.DisplayImage(song.get(Constants.KEY_THUMB_URL), thumb_image);
        if (Objects.equals(song.get(Constants.KEY_SHOW_VOTES_PROGRESS), "TRUE")){
            votesProgress.setVisibility(View.VISIBLE);//visible
            //votesProgress.setProgressDrawable((R.drawable.gradient_bg_hover));
            if (song.get(Constants.KEY_VOTES_PROGRESS) != null){
                votesProgress.setProgress(Integer.parseInt(Objects.requireNonNull(song.get(Constants.KEY_VOTES_PROGRESS))));
            }
        } else {
            votesProgress.setVisibility(View.INVISIBLE);//invisible
        }
        return vi;
    }
}