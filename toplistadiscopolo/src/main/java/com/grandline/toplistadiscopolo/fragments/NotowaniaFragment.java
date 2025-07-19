package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.LazyAdapter;

public class NotowaniaFragment extends Fragment {
    private ListView list2012;
    private LazyAdapter adapter2012;
    private ArrayList<HashMap<String, String>> notowaniaList;
    private Spinner spinnerNotowaniaPrzedzialy;
    private ArrayList<String> listNotowPrzedzialy;
    private ArrayAdapter<String> adapterNotowPrzedzialy;
    private ArrayList<HashMap<String, String>> notowPrzedzialyList;
    private boolean isSpinnerClicked = false;

    public NotowaniaFragment() {
        // Required empty public constructor
    }

    public static NotowaniaFragment newInstance() {
        return new NotowaniaFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notowaniaList = new ArrayList<>();
        listNotowPrzedzialy = new ArrayList<>();
        notowPrzedzialyList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notowania, container, false);
        
        list2012 = view.findViewById(R.id.list2012);
        spinnerNotowaniaPrzedzialy = view.findViewById(R.id.spinnerNotowaniaPrzedzialy);
        
        adapter2012 = new LazyAdapter(getActivity(), notowaniaList);
        list2012.setAdapter(adapter2012);
        
        adapterNotowPrzedzialy = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, listNotowPrzedzialy);
        adapterNotowPrzedzialy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotowaniaPrzedzialy.setAdapter(adapterNotowPrzedzialy);
        
        // Set click listener for ListView
        list2012.setOnItemClickListener((parent, view1, position, id) -> {
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showSongMenu(position, Constants.KEY_LISTA_2012);
            }
        });

        // Set spinner listener
        spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isSpinnerClicked && getActivity() instanceof ListaPrzebojowDiscoPolo) {
                    HashMap<String, String> o = notowPrzedzialyList.get(pos);
                    String notowanieId = o.get(Constants.KEY_NOTOWANIE_ZA);
                    ((ListaPrzebojowDiscoPolo) getActivity()).handleSpinnerSelection(notowanieId, pos);
                }
                isSpinnerClicked = true;
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
        
        return view;
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newNotowaniaList) {
        if (notowaniaList != null && adapter2012 != null) {
            notowaniaList.clear();
            notowaniaList.addAll(newNotowaniaList);
            adapter2012.notifyDataSetChanged();
        }
    }

    public void updateSpinnerAdapter(ArrayList<HashMap<String, String>> newNotowPrzedzialyList, ArrayList<String> newListNotowPrzedzialy) {
        if (notowPrzedzialyList != null && listNotowPrzedzialy != null && adapterNotowPrzedzialy != null) {
            notowPrzedzialyList.clear();
            notowPrzedzialyList.addAll(newNotowPrzedzialyList);
            listNotowPrzedzialy.clear();
            listNotowPrzedzialy.addAll(newListNotowPrzedzialy);
            adapterNotowPrzedzialy.notifyDataSetChanged();
        }
    }

    public void setSpinnerSelection(int position) {
        if (spinnerNotowaniaPrzedzialy != null) {
            spinnerNotowaniaPrzedzialy.setSelection(position);
        }
    }

    public ArrayList<HashMap<String, String>> getNotowaniaList() {
        return notowaniaList;
    }

    public LazyAdapter getAdapter2012() {
        return adapter2012;
    }

    public ArrayList<HashMap<String, String>> getNotowPrzedzialyList() {
        return notowPrzedzialyList;
    }

    public ArrayList<String> getListNotowPrzedzialy() {
        return listNotowPrzedzialy;
    }

    public ArrayAdapter<String> getAdapterNotowPrzedzialy() {
        return adapterNotowPrzedzialy;
    }
}