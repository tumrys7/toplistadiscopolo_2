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

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.LazyAdapter;

public class NotowaniaFragment extends Fragment {
    private ListView listNotowania;
    private LazyAdapter adapterNotowania;
    private Spinner spinnerNotowaniaPrzedzialy;
    private ArrayAdapter<String> adapterNotowPrzedzialy;
    private ListaPrzebojowDiscoPolo parentActivity;
    private boolean isSpinnerInitialized = false;

    public static NotowaniaFragment newInstance() {
        return new NotowaniaFragment();
    }

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
        View view = inflater.inflate(R.layout.fragment_notowania, container, false);
        
        listNotowania = view.findViewById(R.id.listNotowania);
        spinnerNotowaniaPrzedzialy = view.findViewById(R.id.spinnerNotowaniaPrzedzialy);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapterNotowania = new LazyAdapter(getActivity(), parentActivity.notowaniaList);
            listNotowania.setAdapter(adapterNotowania);
            
            adapterNotowPrzedzialy = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, parentActivity.listNotowPrzedzialy);
            adapterNotowPrzedzialy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerNotowaniaPrzedzialy.setAdapter(adapterNotowPrzedzialy);
            
            listNotowania.setOnItemClickListener((parent, clickedView, position, id) ->
                parentActivity.showSongMenu(position, Constants.KEY_LISTA_NOTOWANIA));

            spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    // Only handle selection after the spinner has been fully initialized
                    // This prevents the initial selection from triggering a refresh
                    if (isSpinnerInitialized && parentActivity != null && parentActivity.notowPrzedzialyList != null && pos < parentActivity.notowPrzedzialyList.size()) {
                        String selectedNotowanieId = parentActivity.notowPrzedzialyList.get(pos).get(Constants.KEY_NOTOWANIE_ZA);
                        if (selectedNotowanieId != null && !selectedNotowanieId.isEmpty()) {
                            parentActivity.handleSpinnerSelection(selectedNotowanieId, pos);
                        }
                    } else {
                        // Mark spinner as initialized after the first selection
                        isSpinnerInitialized = true;
                    }
                }

                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    public void updateAdapter() {
        if (adapterNotowania != null && isAdded() && !isRemoving() && getView() != null) {
            adapterNotowania.notifyDataSetChanged();
        }
    }

    public void updateSpinnerAdapter() {
        if (adapterNotowPrzedzialy != null && isAdded() && !isRemoving() && getView() != null) {
            adapterNotowPrzedzialy.notifyDataSetChanged();
        }
    }

    public void setSpinnerSelection(int position) {
        if (spinnerNotowaniaPrzedzialy != null && position >= 0 && position < spinnerNotowaniaPrzedzialy.getCount()) {
            // Temporarily disable the listener to prevent triggering refresh during programmatic selection
            spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(null);
            spinnerNotowaniaPrzedzialy.setSelection(position);
            
            // Re-enable the listener after a short delay
            spinnerNotowaniaPrzedzialy.post(() -> {
                if (spinnerNotowaniaPrzedzialy != null) {
                    spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            if (isSpinnerInitialized && parentActivity != null && parentActivity.notowPrzedzialyList != null && pos < parentActivity.notowPrzedzialyList.size()) {
                                String selectedNotowanieId = parentActivity.notowPrzedzialyList.get(pos).get(Constants.KEY_NOTOWANIE_ZA);
                                if (selectedNotowanieId != null && !selectedNotowanieId.isEmpty()) {
                                    parentActivity.handleSpinnerSelection(selectedNotowanieId, pos);
                                }
                            }
                        }

                        public void onNothingSelected(AdapterView<?> arg0) {
                            // TODO Auto-generated method stub
                        }
                    });
                }
            });
        }
    }

    public void refreshWithAdReward() {
        if (parentActivity != null) {
            parentActivity.refreshNotowaniaWithAdReward();
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
        if (listNotowania != null) {
            listNotowania.clearAnimation();
            listNotowania.clearFocus();
            listNotowania.setOnItemClickListener(null);
            listNotowania.setOnItemLongClickListener(null);
            listNotowania.setOnScrollListener(null);
            listNotowania.setAdapter(null);
        }
        
        if (spinnerNotowaniaPrzedzialy != null) {
            spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(null);
            spinnerNotowaniaPrzedzialy.setAdapter(null);
        }
    }

    private void cleanupReferences() {
        adapterNotowania = null;
        adapterNotowPrzedzialy = null;
        listNotowania = null;
        spinnerNotowaniaPrzedzialy = null;
        parentActivity = null;
    }
}