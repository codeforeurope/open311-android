package org.open311.android.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Image {

    public static final int WIDTH  = 640;
    public static final int HEIGHT = 480;

    /**
     * Calculate the sample size value based on a target width and height
     *
     * @param options Options for the BitmapFactory
     * @param reqWidth target width
     * @param reqHeight target height
     * @return int
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width  = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width  / (float)reqWidth);
            }
        }
        return inSampleSize;
    }

    /**
     * Decode a sampled bitmap from file path
     *
     * @param path path to Bitmap
     * @param reqWidth target width
     * @param reqHeight target height
     * @return Bitmap
     */
    public static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }
}
