package yaschenkovv.com.tscprintingtestapp.bitmapconverter;

import android.graphics.Bitmap;

/**
 * Created by Yaschenko.VV on 15.02.2018.
 */

public interface BitmapConvertSaver {
    public boolean save(Bitmap bitmap, String filePath);
}
