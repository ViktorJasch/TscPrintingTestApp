package yaschenkovv.com.tscprintingtestapp.bitmapconverter;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;

public class OneBitBmpConverter implements BitmapConverter{
	private static final String TAG = "BitmapConverter";

    public byte[] convert(Bitmap inputBitmap, int factor) {
		int bitmapWidth = inputBitmap.getWidth();
		int bitmapHeight = inputBitmap.getHeight();
		//Log.d(TAG, "Width is " + bitmapWidth + "\nHeight is " + bitmapHeight);
		int width = ((bitmapWidth +31)/32)*4*8 + 32 * factor;
		//Log.d(TAG, "Recalculated width = " + mWidth);
		int size = width * bitmapHeight;
		//Log.d(TAG, "Начали конвертировать файл в потоке " + Thread.currentThread().getName());
		byte[] monochrome = convertArgbToGrayScale(inputBitmap, width, size);
		return monochrome;
		//return createRawMonochromeData(monochrome);
	}

	private byte[] convertArgbToGrayScale(Bitmap bitmap, int outputWidth, int outputSize) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		int bytesInRow = bitmap.getRowBytes();
		int size = bytesInRow * height;
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		bitmap.copyPixelsToBuffer(byteBuffer);
		byte[] byteArray = byteBuffer.array();
		byte[] outputData = new byte[outputSize];

		boolean isPremultiplied = bitmap.isPremultiplied();

    	int k = 0;
		byte R, A;
    	final int BLACK = 0;
    	final int WHITE = 1;
		int i;
    	for(i = 0; i < byteArray.length; i += 4) {
			if (i > 0 && i % bytesInRow == 0) {
				for(int j = width; j < outputWidth; j++, k++){
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
			for(int j = width; j < outputWidth; j++, k++){
				outputData[k] = 1;
			}
		}
		//Log.d(TAG, "Output data size = " + outputData.length);
        return outputData;
    }

//    private byte[] createRawMonochromeData(byte[] inputData){
//    	int length = 0;
//		byte[] outputData = new byte[mSize / 8];
//    	for (int i = 0; i < inputData.length; i = i + 8) {
//			byte first = inputData[i];
//			for (int j = 0; j < 7; j++) {
//				byte second = (byte) ((first << 1) | inputData[i + j]);
//				first = second;
//			}
//			outputData[length] = first;
//			length++;
//		}
//		return outputData;
//    }
}
