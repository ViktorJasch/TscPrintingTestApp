package yaschenkovv.com.tscprintingtestapp.bitmapconverter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by Yaschenko.VV on 15.02.2018.
 */

public class FloydSteinbergConverter implements BitmapConverter {
    public static final String TAG = "FloydSteinbergConverter";
    private int mWidth;
    private int mSize;

    @Override
    public byte[] convert(Bitmap inputBitmap, int factor) {
        int bitmapWidth = inputBitmap.getWidth();
        int bitmapHeight = inputBitmap.getHeight();
        Log.d(TAG, "Width is " + bitmapWidth + "\nHeight is " + bitmapHeight);
        mWidth = ((bitmapWidth +31)/32)*4*8;
        Log.d(TAG, "Recalculated width = " + mWidth);
        mSize = mWidth * bitmapHeight;
        Log.d(TAG, "Начали конвертировать файл в потоке " + Thread.currentThread().getName());
        byte[] monochrome = convertArgbToGrayScale(floydSteinberg(inputBitmap));
        return monochrome;
        //return createRawMonochromeData(monochrome);
    }

    private byte[] convertArgbToGrayScale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int bytesInRow = bitmap.getRowBytes();
        Log.d(TAG, "byte in row = " + bytesInRow);
        int size = bytesInRow * height;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        Log.d(TAG, "byteArray size = " + byteArray.length);
        byte[] outputData = new byte[mSize];

        boolean isPremultiplied = bitmap.isPremultiplied();

        int k = 0;
        byte R, A;
        final int BLACK = 0;
        final int WHITE = 1;
        int i;
        for(i = 0; i < byteArray.length; i += 4) {
            if (i > 0 && i % bytesInRow == 0) {
                for(int j = width; j < mWidth; j++, k++){
                    outputData[k] = WHITE;
                }
            }
            // retrieve color of all channels
            A = byteArray[i + 3];
            R = byteArray[i + 2];
            if (isPremultiplied && A != 0) {
                //R change in range -127...127
                R = (byte) (R * 255 / A);
            }
            // set new pixel color to output bitmap
            if (A == 0 || (R < 0)) {
                outputData[k] = WHITE;
            } else {
                outputData[k] = BLACK;
            }
            k++;
        }
        if (i > 0 && i % bytesInRow == 0) {
            for(int j = width; j < mWidth; j++, k++){
                outputData[k] = 1;
            }
        }
        Log.d(TAG, "Output data size = " + outputData.length);
        return outputData;
    }

    private byte[] createRawMonochromeData(byte[] inputData){
        int length = 0;
        byte[] outputData = new byte[mSize / 8];
        for (int i = 0; i < inputData.length; i = i + 8) {
            byte first = inputData[i];
            for (int j = 0; j < 7; j++) {
                byte second = (byte) ((first << 1) | inputData[i + j]);
                first = second;
            }
            outputData[length] = first;
            length++;
        }
        return outputData;
    }

    public Bitmap floydSteinberg(Bitmap src) {

//Getting configuration to get the width and height of the bitmap
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(),src.getConfig());

        int alpha, red;
        int pixel;
        int gray;
        int threshold=128;

        int width = src.getWidth();
        int height = src.getHeight();
        int error = 0;
        int errors[][] = new int[width][height];
        for (int y = 0; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                pixel = src.getPixel(x, y);

                alpha = Color.alpha(pixel);
                red = Color.red(pixel);
                int green=Color.green(pixel);
                int blue=Color.blue(pixel);

//using red,green,blue with constant values to get gray color

                int grayC=(int) (0.21 * red + 0.72 * green + 0.07 * blue);
                gray = grayC;
                if (gray + errors[x][y] < threshold) {
                    error = gray + errors[x][y];
                    gray = 0;
                } else {
                    error = gray + errors[x][y] - 255;
                    gray = 255;
                }
                errors[x + 1][y] += (7 * error) / 16;
                errors[x  - 1][y + 1] += (3 * error) / 16;
                errors[x][y + 1] += (5 * error) / 16;
                errors[x + 1][y + 1] += (1* error) / 16;

                out.setPixel(x, y, Color.argb(alpha, gray, gray, gray));

            }
        }

        return out;
    }
}
