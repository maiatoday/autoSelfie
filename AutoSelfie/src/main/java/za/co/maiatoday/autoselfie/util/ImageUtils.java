package za.co.maiatoday.autoselfie.util;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for image related methods.
 * Created by maia on 2013/08/29.
 */
public class ImageUtils {

    public static String getUniqueImageFilename() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileName = formatter.format(now) + ".jpg";
        return fileName;
    }

    public static Bitmap getSizedBitmap(Context context, Uri uri, int size) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1)
            return null;

        int originalSize = onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = originalSize > size ? originalSize / (double) size : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else return k;
    }

    public static boolean saveBitmapToFile(Bitmap pic, Context context) {
        boolean ret = false;
        final File path = new File(Environment.getExternalStorageDirectory() + File.separator + "autoSelfie" + File.separator);
        final String fname = getUniqueImageFilename();
        final File file = new File(path, fname);

        if (pic != null) {
            try {
                // build directory
                if (file.getParent() != null && !path.isDirectory()) {
                    path.mkdirs();
                }
                // output image to file
                FileOutputStream fos = new FileOutputStream(file);
                pic.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
                addImageToContentProvider(context, file);
                ret = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static void addImageToContentProvider(Context context, File imageFile) {
        ContentValues image = new ContentValues();

        image.put(Images.Media.TITLE, imageFile.getName());
        image.put(Images.Media.DISPLAY_NAME, imageFile.getName());
        image.put(Images.Media.DESCRIPTION, "autoselfie modified image");
        image.put(Images.Media.DATE_ADDED, imageFile.lastModified());
        image.put(Images.Media.DATE_TAKEN, imageFile.lastModified());
        image.put(Images.Media.DATE_MODIFIED, imageFile.lastModified());
        image.put(Images.Media.MIME_TYPE, "image/png");
        image.put(Images.Media.ORIENTATION, 0);

        File parent = imageFile.getParentFile();
        String path = parent.toString().toLowerCase();
        String name = parent.getName().toLowerCase();
        image.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
        image.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
        image.put(Images.Media.SIZE, imageFile.length());

        image.put(Images.Media.DATA, imageFile.getAbsolutePath());

        Uri result = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, image);
    }
}
