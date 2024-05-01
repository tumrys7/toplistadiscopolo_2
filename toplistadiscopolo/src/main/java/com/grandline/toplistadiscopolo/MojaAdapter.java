package com.grandline.toplistadiscopolo;

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
        return data.size();
    }

    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row_moja, null);
        TextView id_listy = (TextView)vi.findViewById(R.id.id_listy); // id_listy
        TextView title = (TextView)vi.findViewById(R.id.title); // title
        TextView artist = (TextView)vi.findViewById(R.id.artist); // artist name
        TextView duration = (TextView)vi.findViewById(R.id.ile_glosow); // duration
        ImageView thumb_image = (ImageView)vi.findViewById(R.id.list_image); // thumb image
        TextView listPosition = (TextView)vi.findViewById(R.id.miejsce); // listPosition
        TextView createDate = (TextView)vi.findViewById(R.id.data_dodania); // createDate
        ProgressBar votesProgress = (ProgressBar)vi.findViewById(R.id.votesProgress); // progres

        HashMap<String, String> song;
        song = data.get(position);
        
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