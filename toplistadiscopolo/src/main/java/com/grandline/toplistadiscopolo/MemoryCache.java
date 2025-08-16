package com.grandline.toplistadiscopolo;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MemoryCache {
    private final Map<String, SoftReference<Bitmap>> cache=Collections.synchronizedMap(new HashMap<>());

    public Bitmap get(String id){
        if(!cache.containsKey(id))
            return null;
        SoftReference<Bitmap> ref=cache.get(id);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    public void put(String id, Bitmap bitmap){
        if (bitmap != null && id != null) {
            cache.put(id, new SoftReference<>(bitmap));
        }
    }

    public void clear() {
        cache.clear();
    }
}