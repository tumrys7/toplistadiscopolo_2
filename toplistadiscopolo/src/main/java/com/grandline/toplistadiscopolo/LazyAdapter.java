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

public class LazyAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader; 
    
    public LazyAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
            vi = inflater.inflate(R.layout.list_row, null);

        TextView id_listy = vi.findViewById(R.id.id_listy); // id_listy
        TextView title = vi.findViewById(R.id.title); // title
        TextView artist = vi.findViewById(R.id.artist); // artist name
        TextView duration = vi.findViewById(R.id.ile_glosow); // duration
        ImageView thumb_image= vi.findViewById(R.id.list_image); // thumb image
        ImageView arrow_image= vi.findViewById(R.id.imageView1); // thumb image
        TextView listPosition = vi.findViewById(R.id.miejsce); // listPosition
        TextView createDate = vi.findViewById(R.id.data_dodania); // listPosition
        TextView placeChange = vi.findViewById(R.id.zmiana); // title
        ProgressBar votesProgress = vi.findViewById(R.id.votesProgress); // title
        
        HashMap<String, String> song = new HashMap<String, String>();
        song = data.get(position);
        
        // Setting all values in listview
        id_listy.setText(song.get(Constants.KEY_ID));
        title.setText(song.get(Constants.KEY_TITLE));
        artist.setText(song.get(Constants.KEY_ARTIST));
        duration.setText(song.get(Constants.KEY_VOTES));
        listPosition.setText(song.get(Constants.KEY_POSITION));
        createDate.setText(song.get(Constants.KEY_CREATE_DATE));
        imageLoader.DisplayImage(song.get(Constants.KEY_THUMB_URL), thumb_image);
        if (song.get(Constants.KEY_ARROW_TYPE)==Constants.KEY_ARROW_UP) {
        	arrow_image.setImageResource(R.drawable.arrow_up);
        } else if (song.get(Constants.KEY_ARROW_TYPE)==Constants.KEY_ARROW_DOWN) {
        	arrow_image.setImageResource(R.drawable.arrow_down);
        } else if (song.get(Constants.KEY_ARROW_TYPE)==Constants.KEY_ARROW_NO_CHANGE) {
        	arrow_image.setImageResource(R.drawable.arrow_no_change);
        } else {
        	arrow_image.setImageResource(R.drawable.arrow);
        }
        placeChange.setText(song.get(Constants.KEY_PLACE_CHANGE));
        if (song.get(Constants.KEY_SHOW_VOTES_PROGRESS).equals("TRUE")){
        	votesProgress.setVisibility(View.VISIBLE);//visible setVisibility(0)
        	//votesProgress.setProgressDrawable((R.drawable.gradient_bg_hover));
	        if (song.get(Constants.KEY_VOTES_PROGRESS) != null){
	        	votesProgress.setProgress(Integer.parseInt(song.get(Constants.KEY_VOTES_PROGRESS)));
	        }
        } else {
        	votesProgress.setVisibility(View.INVISIBLE);//invisible
        }
        return vi;
    }
}