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
            vi = inflater.inflate(R.layout.wykon_row, parent, false);

        TextView id_wykonawcy = vi.findViewById(R.id.id_wykonawcy); // id wykonawcy
        TextView artist = vi.findViewById(R.id.artist); // artist name


        HashMap<String, String> wyk;
        wyk = data.get(position);
        
        // Setting all values in listview
        id_wykonawcy.setText(wyk.get(Constants.KEY_ID_WYKON));
        artist.setText(wyk.get(Constants.KEY_WYKONAWCA));
        return vi;
    }
}