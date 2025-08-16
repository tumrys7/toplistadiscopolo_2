package com.grandline.toplistadiscopolo.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.grandline.toplistadiscopolo.R;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * Adapter wrapper that inserts NativeAd rows at fixed repeating positions in the ListView.
 * This wrapper delegates content rows to the provided LazyAdapter and renders native ad rows
 * at positions 3, 6, 9, ... (1-based positions) when an ad is available.
 */
public class NativeAdAdapterWrapper extends BaseAdapter {

    private static final int VIEW_TYPE_CONTENT = 0;
    private static final int VIEW_TYPE_AD = 1;

    // First ad at 0-based index 2 (which is position 3 in 1-based terms)
    private static final int FIRST_AD_POSITION = 2;
    // Repeat ads every 3 list items (positions 3, 6, 9, ...)
    private static final int AD_INTERVAL = 3;

    private final LazyAdapter contentAdapter;
    private final LayoutInflater layoutInflater;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Maintain a pool of loaded native ads to show different ads in different slots
    private final List<NativeAd> nativeAds = new ArrayList<>();

    public NativeAdAdapterWrapper(Context context, LazyAdapter contentAdapter) {
        this.contentAdapter = contentAdapter;
        this.layoutInflater = LayoutInflater.from(context);
    }

    // Backward-compatible: replace any existing ads with a single ad
    public synchronized void setNativeAd(NativeAd nativeAd) {
        destroyAds();
        if (nativeAd != null) {
            nativeAds.add(nativeAd);
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyDataSetChanged();
        } else {
            mainHandler.post(this::notifyDataSetChanged);
        }
    }

    // Add an ad to the pool; UI will refresh and fill the next ad slot
    public synchronized void addNativeAd(NativeAd nativeAd) {
        if (nativeAd != null) {
            nativeAds.add(nativeAd);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                notifyDataSetChanged();
            } else {
                mainHandler.post(this::notifyDataSetChanged);
            }
        }
    }

    // Replace all ads with a provided list
    public synchronized void setNativeAds(List<NativeAd> ads) {
        destroyAds();
        if (ads != null) {
            nativeAds.addAll(ads);
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyDataSetChanged();
        } else {
            mainHandler.post(this::notifyDataSetChanged);
        }
    }

    // Backward-compatible method name
    public void destroyAd() {
        destroyAds();
    }

    public synchronized void destroyAds() {
        for (NativeAd ad : nativeAds) {
            try {
                ad.destroy();
            } catch (Exception ignored) { }
        }
        nativeAds.clear();
    }

    public boolean isAdPosition(int position) {
        if (!shouldShowAd(position)) return false;
        return position >= FIRST_AD_POSITION
                && ((position - FIRST_AD_POSITION) % AD_INTERVAL == 0)
                && adSlotIndexForAdapterPosition(position) < nativeAds.size();
    }

    private boolean shouldShowAd(int position) {
        return !nativeAds.isEmpty() && contentAdapter != null && contentAdapter.getCount() > FIRST_AD_POSITION;
    }

    private static int computeAdCountBeforePosition(int position) {
        // Count how many theoretical ad slots occur at indexes strictly less than the provided position
        if (position <= FIRST_AD_POSITION) {
            return 0;
        }
        int adjusted = position - 1 - FIRST_AD_POSITION;
        return 1 + (adjusted / AD_INTERVAL);
    }

    private static int adSlotIndexForAdapterPosition(int position) {
        if (position < FIRST_AD_POSITION) return -1;
        return (position - FIRST_AD_POSITION) / AD_INTERVAL;
    }

    private static int computeAdSlotCountForContent(int contentCount) {
        if (contentCount <= FIRST_AD_POSITION) {
            return 0;
        }
        int remainingAfterFirst = contentCount - FIRST_AD_POSITION;
        // Each block after the first ad consists of (AD_INTERVAL - 1) content rows per ad
        return 1 + (remainingAfterFirst - 1) / (AD_INTERVAL - 1);
    }

    public int toContentPosition(int adapterPosition) {
        if (nativeAds.isEmpty()) return adapterPosition;
        int theoreticalAdsBefore = computeAdCountBeforePosition(adapterPosition);
        int adsBefore = Math.min(theoreticalAdsBefore, nativeAds.size());
        return adapterPosition - adsBefore;
    }

    @Override
    public int getCount() {
        int base = contentAdapter != null ? contentAdapter.getCount() : 0;
        if (base == 0) return 0;
        int theoreticalAdSlots = computeAdSlotCountForContent(base);
        int filledAdSlots = Math.min(theoreticalAdSlots, nativeAds.size());
        return base + Math.max(filledAdSlots, 0);
    }

    @Override
    public Object getItem(int position) {
        if (isAdPosition(position)) {
            return null;
        }
        int contentPos = toContentPosition(position);
        return contentAdapter.getItem(contentPos);
    }

    @Override
    public long getItemId(int position) {
        if (isAdPosition(position)) {
            return -1;
        }
        return contentAdapter.getItemId(toContentPosition(position));
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return isAdPosition(position) ? VIEW_TYPE_AD : VIEW_TYPE_CONTENT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isAdPosition(position)) {
            NativeAdView adView;
            if (!(convertView instanceof NativeAdView)) {
                adView = (NativeAdView) layoutInflater.inflate(R.layout.native_ad_row, parent, false);
            } else {
                adView = (NativeAdView) convertView;
            }
            int adIndex = adSlotIndexForAdapterPosition(position);
            if (adIndex >= 0 && adIndex < nativeAds.size()) {
                populateNativeAdView(nativeAds.get(adIndex), adView);
                return adView;
            }
            // Fallback to content if something went wrong
        }
        int contentPos = toContentPosition(position);
        return contentAdapter.getView(contentPos, convertView instanceof NativeAdView ? null : convertView, parent);
    }

    private static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Assign asset views
        adView.setMediaView(adView.findViewById(R.id.ad_media));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // Populate the native ad view
        ((android.widget.TextView) Objects.requireNonNull(adView.getHeadlineView())).setText(nativeAd.getHeadline());

        if (nativeAd.getBody() == null) {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.VISIBLE);
            ((android.widget.TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
            ((android.widget.Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            if (adView.getIconView() != null) {
                adView.getIconView().setVisibility(View.GONE);
            }
        } else {
            if (adView.getIconView() != null) {
                ((android.widget.ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        }

        if (nativeAd.getAdvertiser() == null) {
            if (adView.getAdvertiserView() != null) adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            if (adView.getAdvertiserView() != null) {
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
                ((android.widget.TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            }
        }

        adView.setNativeAd(nativeAd);
    }
}