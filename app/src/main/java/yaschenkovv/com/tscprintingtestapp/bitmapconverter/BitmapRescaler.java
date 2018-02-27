package yaschenkovv.com.tscprintingtestapp.bitmapconverter;

import android.util.Log;

/**
 * Created by Yaschenko.VV on 16.02.2018.
 */

public class BitmapRescaler {
    private static final String TAG = "Rescaler";
    byte[] outputBytes;

    public byte[] rescaleDown(int rescaleFactor, byte[] inputBytes, int rowCount) {
        // outputBytes = new byte[inputBytes.length * rescaleFactor * rescaleFactor];
        //showMinBar(inputBytes, rowCount);
        if(inputBytes.length % (rescaleFactor * rescaleFactor) != 0) {
            throw new IllegalArgumentException("inputBytes.length должен быть кратен квадрату rescaleFactor.\n" +
                    "rescaleFactor^2 = " + rescaleFactor*rescaleFactor + "\n" +
                    "inputBytes.length = " + inputBytes.length);
        }
        outputBytes = new byte[inputBytes.length / (rescaleFactor*rescaleFactor)];
        nearNeighborRescale(rescaleFactor, inputBytes, rowCount);
        return createRawMonochromeData(outputBytes);
    }

    private void nearNeighborRescale(int rescaleFactor, byte[] inputData, int rowCount) {
        checkRowCountValid(inputData, rowCount);
        int byteInInputRow = inputData.length / rowCount;
        int outIndex = 0;
        for(int y = 0; y < rowCount; y += rescaleFactor) {
            for(int x = 0; x < byteInInputRow; x += rescaleFactor) {
                byte pixel = inputData[y * byteInInputRow + x];
                outputBytes[outIndex++] = pixel;
            }
        }
    }

    private void checkRowCountValid(byte[] inputData, int rowCount) {
        Log.d(TAG, "Size of input byte = " + inputData.length);
        if(inputData.length % rowCount != 0) {
            throw new IllegalArgumentException("rowCount должно быть кратно inputData.length.\n" +
                    "rowCount = " + rowCount + "\n" +
                    "inputData.length = " + inputData.length + ".");
        }
    }

    private byte[] createRawMonochromeData(byte[] inputData){
        int length = 0;
        byte[] outputData = new byte[inputData.length / 8];
        for (int i = 0; i < inputData.length; i = i + 8) {
            byte first = inputData[i];
            for (int j = 0; j <= 7; j++) {
                byte second = (byte) ((first << 1) | inputData[i + j]);
                first = second;
            }
            outputData[length] = first;
            length++;
        }
        return outputData;
    }
}
