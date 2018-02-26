package yaschenkovv.com.tscprintingtestapp.bitmapconverter;

import android.graphics.Bitmap;

/**
 * Created by Yaschenko.VV on 15.02.2018.
 */

public interface BitmapConverter {
    public byte[] convert(Bitmap inputBitmap, int factor);
}
