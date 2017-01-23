package org.open311.android.helpers;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;

import org.open311.android.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import okhttp3.MediaType;

public class Utils {
    private static final String OPEN311_SETTINGS = "open311_settings";

    public static void hideKeyBoard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static SharedPreferences getSettings(Activity activity) {
        return activity.getSharedPreferences(OPEN311_SETTINGS, 0);
    }

    private static String removeAllRedundantSpaces(String str) {
        str = str.trim();
        str = str.replaceAll(" +", " ");
        return str;
    }

    public static String formatAddress(Address address) {
        //get the Locale
        String housenumber = address.getSubThoroughfare() == null ? "" : address.getSubThoroughfare();
        String street = address.getThoroughfare() == null ? "" : address.getThoroughfare();
        String locality = address.getLocality() == null ? "" : address.getLocality();
        if (Locale.getDefault() == Locale.US) {
            //housenumber first
            return removeAllRedundantSpaces(housenumber + " " + street + ", " + locality);
        } else {
            return removeAllRedundantSpaces(street + " " + housenumber + ", " + locality);
        }

    }

    public static String addressString(Address address) {


        StringBuilder sb = new StringBuilder();
        int n = address.getMaxAddressLineIndex();
        for (int i = 0; i <= n; i++) {
            if (i != 0)
                sb.append(", ");
            sb.append(address.getAddressLine(i));
        }
        return sb.toString();
    }

    public static String saveSetting(Activity activity, String key, Float value) {
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        String result = null;
        editor.putFloat(key.replace(',', '_').replace(' ', '_').toLowerCase(), value);
        if (editor.commit()) {
            result = activity.getString(R.string.settings_saved);
        } else {
            result = activity.getString(R.string.error_occurred);
        }
        return result;
    }

    public static String saveSetting(Activity activity, String key, String value) {
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        String result = null;
        editor.putString(key.replace(',', '_').replace(' ', '_').toLowerCase(), value);
        if (editor.commit()) {
            result = activity.getString(R.string.settings_saved);
        } else {
            result = activity.getString(R.string.error_occurred);
        }
        return result;
    }

    public static String saveSetting(Activity activity, String key, List<String> values) {
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        String result;
        Gson gson = new Gson();
        List<String> mList = new ArrayList<String>();
        mList.addAll(values);
        String jsonText = gson.toJson(mList);
        editor.putString(key.replace(',', '_').replace(' ', '_').toLowerCase(), jsonText);
        if (editor.commit()) {
            result = activity.getString(R.string.settings_saved);
        } else {
            result = activity.getString(R.string.error_occurred);
        }
        return result;
    }

    public static Boolean removeSetting(Activity activity, String key) {
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key.replace(',', '_').replace(' ', '_').toLowerCase());
        return editor.commit();
    }

    public static String[] updateReports(Activity activity, String server, String value) {
        List<String> mList = new ArrayList<String>();
        String[] existingReports = getReports(activity, server);
        if (existingReports != null) {
            Collections.addAll(mList, existingReports);
        }
        mList.add(value);
        saveSetting(activity, server, mList);
        return getReports(activity, server);
    }

    public static String[] getReports(Activity activity, String server) {
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        Gson gson = new Gson();
        String jsonText = settings.getString(server.replace(',', '_').replace(' ', '_').toLowerCase(), null);
        return gson.fromJson(jsonText, String[].class);
    }

    public static String randomString(int MAX_LENGTH) {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public static Date randomDate() {

        SimpleDateFormat dfDateTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss", Locale.getDefault());
        int year = randBetween(2015, 2016);// Here you can set Range of years you need
        int month = randBetween(0, 11);
        int hour = randBetween(9, 22); //Hours will be displayed in between 9 to 22
        int min = randBetween(0, 59);
        int sec = randBetween(0, 59);


        GregorianCalendar gc = new GregorianCalendar(year, month, 1);
        int day = randBetween(1, gc.getActualMaximum(Calendar.DAY_OF_MONTH));

        gc.set(year, month, day, hour, min, sec);

        return gc.getTime();

    }

    private static int randBetween(int start, int end) {
        return start + (int) Math.round(Math.random() * (end - start));
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        assert dir != null;
        return dir.delete();
    }

    public static okhttp3.MediaType getMediaType(Context context, Uri uri1) {
        Uri uri = Uri.parse(uri1.toString());
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return okhttp3.MediaType.parse(mimeType);
    }

    public static String getExtensionForMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType))
            return "";

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String fileExtensionFromMimeType = mimeTypeMap.getExtensionFromMimeType(mimeType);
        if (TextUtils.isEmpty(fileExtensionFromMimeType)) {
            // We're still without an extension - split the mime type and retrieve it
            String[] split = mimeType.split("/");
            fileExtensionFromMimeType = split.length > 1 ? split[1] : split[0];
        }

        return fileExtensionFromMimeType.toLowerCase();
    }

    public static String niceName(Context context, Uri uri) {
        String scheme = uri.getScheme();
        MediaType mediaType = getMediaType(context, uri);
        if (scheme.equals("file")) {
            return uri.getLastPathSegment();
        } else if (scheme.equals("content")) {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            int nameIndex = 0;
            if (returnCursor != null) {
                nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String name = returnCursor.getString(nameIndex);
                String extension = getExtensionForMimeType(String.valueOf(mediaType));
                name = name + "." + extension;
                return name;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
