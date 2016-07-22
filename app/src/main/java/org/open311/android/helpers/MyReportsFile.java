package org.open311.android.helpers;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MyReportsFile {
    private static final String LOG_TAG = "MyReportsFile";
    private static final String FILENAME = "my_reports";
    private Context context;

    public MyReportsFile(Context context) {
        this.context = context;
    }

    public boolean addServiceRequestId(String serviceRequestId) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_APPEND);
            fos.write(("," + serviceRequestId).getBytes());
            return true;
        } catch (FileNotFoundException e) {
            // TODO Create the file if it cannot be found and try a write.
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) fos.close();
        }
        return false;
    }

    public String getServiceRequestIds() {
        FileInputStream fis = null;
        String line;
        String identifiers = "";
        try {
            fis = context.openFileInput(FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                identifiers += line;
            }
            if (fis != null) fis.close();
            identifiers = identifiers.startsWith(",") ? identifiers.substring(1) : identifiers;
        } catch (FileNotFoundException e) {
            // todo tell the app that the file does not exist
            Log.d(LOG_TAG, "File '" + FILENAME + "' not found");
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException on " + FILENAME);
        } finally {
            return identifiers;
        }

    }

    public int getServiceRequestLength()  {
        String temp = getServiceRequestIds();
        String[] array = temp.split("\\|", -1);
        return array.length;
    }
}
