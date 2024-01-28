package com.grandline.toplistadiscopolo;

import android.content.Context;

import java.io.File;
import java.nio.file.SecureDirectoryStream;

public class FileCache {
    
    private final File cacheDir;
    
    public FileCache(Context context){
        //Find the dir to save cached images
     ///   if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
     ///       cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"LazyList");
     ///   else
            cacheDir=context.getCacheDir();
      //  if(!cacheDir.exists())
      //      cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename=String.valueOf(url.hashCode());
        //Another possible solution (thanks to grantland)
       // String filename = URLEncoder.encode(url);
        return new File(cacheDir, filename);
    }
    
    public void clear(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        SecureDirectoryStream<Object> context;
        for(File f:files)
        f.delete();
    }

}