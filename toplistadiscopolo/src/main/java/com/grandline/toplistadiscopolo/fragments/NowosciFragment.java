package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.NowosciAdapter;

public class NowosciFragment extends Fragment {

    private ListView listNowosci;
    private NowosciAdapter adapterNowosci;
    private ListaPrzebojowDiscoPolo parentActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            parentActivity = (ListaPrzebojowDiscoPolo) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nowosci, container, false);

        listNowosci = view.findViewById(R.id.listNowosci);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (parentActivity != null && getActivity() != null) {
            adapterNowosci = new NowosciAdapter(getActivity(), parentActivity.songsListNowosci);
            listNowosci.setAdapter(adapterNowosci);

            listNowosci.setOnItemClickListener((parent, clickedView, position, id) -> {
                // Validate position before calling showSongMenu
                if (parentActivity != null && parentActivity.songsListNowosci != null && 
                    position >= 0 && position < parentActivity.songsListNowosci.size()) {
                    parentActivity.showSongMenu(position, Constants.KEY_NOWOSCI);
                } else {
                    Log.e("NowosciFragment", "Invalid click position " + position + 
                          " for list size " + (parentActivity != null && parentActivity.songsListNowosci != null ? 
                          parentActivity.songsListNowosci.size() : 0));
                }
            });
                    
            // Notify parent activity that this fragment is ready for updates
            Log.i("NowosciFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("NowosciFragment");
        }
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && listNowosci != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.songsListNowosci != null) ? 
                    parentActivity.songsListNowosci.size() : 0;
                Log.i("NowosciFragment", "Updating adapter with " + dataSize + " items");
                
                if (adapterNowosci != null && dataSize > 0) {
                    adapterNowosci.safeNotifyDataSetChanged();
                } else if (parentActivity != null && parentActivity.songsListNowosci != null && dataSize > 0) {
                    // Recreate adapter if it's null but we have data
                    Log.i("NowosciFragment", "Recreating adapter with " + dataSize + " items");
                    adapterNowosci = new NowosciAdapter(getActivity(), parentActivity.songsListNowosci);
                    listNowosci.setAdapter(adapterNowosci);
                }
            } catch (Exception e) {
                Log.e("NowosciFragment", "Error updating adapter: " + e.getMessage());
            }
        }
    }

    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.songsListNowosci != null && listNowosci != null &&
                isAdded() && !isRemoving() && getView() != null) {
            Log.i("NowosciFragment", "Force refreshing adapter");
            assert getActivity() != null;
            adapterNowosci = new NowosciAdapter(getActivity(), parentActivity.songsListNowosci);
            listNowosci.setAdapter(adapterNowosci);
        }
    }

    @Override
    public void onDestroyView() {
        cleanupViews();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        cleanupReferences();
        super.onDestroy();
    }

    private void cleanupViews() {
        if (listNowosci != null) {
            listNowosci.clearAnimation();
            listNowosci.clearFocus();
            listNowosci.setOnItemClickListener(null);
            listNowosci.setOnItemLongClickListener(null);
            listNowosci.setOnScrollListener(null);
            listNowosci.setAdapter(null);
        }
    }

    private void cleanupReferences() {
        adapterNowosci = null;
        listNowosci = null;
        parentActivity = null;
    }
}