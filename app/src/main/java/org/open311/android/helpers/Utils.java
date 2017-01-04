package org.open311.android.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;

import org.open311.android.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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
        String city = address.getLocality() == null ? "" : address.getLocality();
        if (Locale.getDefault() == Locale.US) {
            //housenumber first
            return removeAllRedundantSpaces(housenumber + " " + street + ", " + city);
        } else {
            return removeAllRedundantSpaces(street + " " + housenumber + ", " + city);
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

    public static String saveSetting(Activity activity, String key, List<String> values){
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



    public static String[] updateReportsForCity(Activity activity, String city, String value){
        List<String> mList = new ArrayList<String>();
        Collections.addAll(mList, getReportsForCity(activity, city));
        mList.add(value);
        saveSetting(activity, city, mList);
        return getReportsForCity(activity, city);
    }

    public static String[] getReportsForCity(Activity activity, String city){
        SharedPreferences settings = activity.getSharedPreferences(OPEN311_SETTINGS, 0);
        Gson gson = new Gson();
        String jsonText = settings.getString(city.replace(',', '_').replace(' ', '_').toLowerCase(), null);
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

}
