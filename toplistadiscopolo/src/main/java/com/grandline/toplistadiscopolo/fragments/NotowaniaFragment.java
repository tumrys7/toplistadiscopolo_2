package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

            listNotowania.setOnItemClickListener((parent, clickedView, position, id) -> {
                // Validate position before calling showSongMenu
                if (parentActivity != null && parentActivity.notowaniaList != null && 
                    position >= 0 && position < parentActivity.notowaniaList.size()) {
                    parentActivity.showSongMenu(position, Constants.KEY_LISTA_NOTOWANIA);
                } else {
                    Log.e("NotowaniaFragment", "Invalid click position " + position + 
                          " for list size " + (parentActivity != null && parentActivity.notowaniaList != null ? 
                          parentActivity.notowaniaList.size() : 0));
                }
            });

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
            
            // Notify parent activity that this fragment is ready for updates
            Log.i("NotowaniaFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("NotowaniaFragment");
        }
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && listNotowania != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.notowaniaList != null) ? 
                    parentActivity.notowaniaList.size() : 0;
                Log.i("NotowaniaFragment", "Updating adapter with " + dataSize + " items");
                Log.i("NotowaniaFragment", "Fragment state - isAdded: " + isAdded() + ", adapter exists: " + (adapterNotowania != null) + 
                    ", list exists: " + (listNotowania != null) + ", parentActivity exists: " + (parentActivity != null));
                
                if (dataSize > 0) {
                    if (adapterNotowania != null) {
                        Log.i("NotowaniaFragment", "Notifying existing adapter of data changes");
                        adapterNotowania.safeNotifyDataSetChanged();
                    } else {
                        // Recreate adapter if it's null but we have data
                        Log.i("NotowaniaFragment", "Recreating adapter with " + dataSize + " items");
                        adapterNotowania = new LazyAdapter(getActivity(), parentActivity.notowaniaList);
                        listNotowania.setAdapter(adapterNotowania);
                    }
                } else {
                    Log.w("NotowaniaFragment", "No data available to display (dataSize: " + dataSize + ")");
                    // Still try to notify adapter in case data was cleared
                    if (adapterNotowania != null) {
                        adapterNotowania.safeNotifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                Log.e("NotowaniaFragment", "Error updating adapter: " + e.getMessage(), e);
            }
        } else {
            Log.w("NotowaniaFragment", "Cannot update adapter - Fragment state - isAdded: " + isAdded() + 
                ", isRemoving: " + isRemoving() + ", hasView: " + (getView() != null) + 
                ", hasList: " + (listNotowania != null));
        }
    }

    public void updateSpinnerAdapter() {
        if (adapterNotowPrzedzialy != null && isAdded() && !isRemoving() && getView() != null) {
            safeNotifySpinnerDataSetChanged();
        }
    }

    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.notowaniaList != null && listNotowania != null &&
                isAdded() && !isRemoving() && getView() != null) {
            Log.i("NotowaniaFragment", "Force refreshing adapter");
            assert getActivity() != null;
            adapterNotowania = new LazyAdapter(getActivity(), parentActivity.notowaniaList);
            listNotowania.setAdapter(adapterNotowania);
        }
    }
    
    /**
     * Safely notify spinner adapter data set changed on the main UI thread
     */
    private void safeNotifySpinnerDataSetChanged() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            if (adapterNotowPrzedzialy != null) {
                adapterNotowPrzedzialy.notifyDataSetChanged();
            }
        } else {
            // Post to main thread
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if (adapterNotowPrzedzialy != null) {
                    adapterNotowPrzedzialy.notifyDataSetChanged();
                }
            });
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