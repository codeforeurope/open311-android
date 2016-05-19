package org.open311.android.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.data.Attribute;
import org.codeforamerica.open311.facade.data.AttributeInfo;
import org.codeforamerica.open311.facade.data.POSTServiceRequestResponse;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.data.ServiceDefinition;
import org.codeforamerica.open311.facade.data.operations.POSTServiceRequestData;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.open311.android.Constants;
import org.open311.android.MainActivity;
import org.open311.android.R;
import org.open311.android.helpers.CustomButton;
import org.open311.android.helpers.Image;
import org.open311.android.helpers.MyReportsFile;
import org.open311.android.helpers.Utils;
import org.open311.android.network.MultipartHTTPNetworkManager;
import org.open311.android.network.POSTServiceRequestDataWrapper;
import org.open311.android.helpers.SingleValueAttributeWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Report {@link Fragment} subclass.
 *
 */
public class ReportFragment extends Fragment {

    private LinkedList<AttributeInfo> attrInfoList;
    private LinkedList<Attribute> attributes;
    private List<Service> services;
    private Uri imageUri;
    private Uri thumbnailUri;
    private ProgressDialog progress;
    private PostServiceRequestTask bgTask;

    private Float latitude;
    private Float longitude;

    public static final int BORDER_SIZE = 20; // border size of photo thumbnail

    public static final int CAMERA_REQUEST = 101;
    public static final int LOCATION_REQUEST = 102;

    private static final boolean ATTRIBUTES_ENABLED = false;

    private CustomButton photoButton;
    private String location;
    private String serviceName;
    private String serviceCode;
    private String installationId;

    public ReportFragment() {
        // Required empty public constructor
    }

    /**
     * Find a view by id - convenience method
     *
     * @param viewId
     * @return View
     */
    private View findViewById(int viewId) {
        return getActivity().findViewById(viewId);
    }

    private void updateServiceButton() {
        if (serviceName != null) {
            CustomButton btn = (CustomButton) findViewById(R.id.report_service_button);
            btn.setText(serviceName);
        }
    }

    private void updateLocationButton() {
        if (location != null) {
            CustomButton btn = (CustomButton) findViewById(R.id.report_location_button);
            btn.setText(location);
        }
    }

    public void updatePhotoButtonImage() {
        if (thumbnailUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), thumbnailUri);
                updatePhotoButtonImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePhotoButtonImage(Bitmap photo) {
        if (photoButton != null) {
            photoButton.setHint("");
            photoButton.setImageBitmap(addWhiteBorder(photo, BORDER_SIZE));
        }
    }

    public int getPhotoButtonWidth() {
        return photoButton.getWidth() - BORDER_SIZE * 2;
    }

    public void updateAttributeButtons() {
        if (attrInfoList != null) {
            addAttributesToForm();
        }
    }

    private void resetPhotoButton() {
        if (photoButton != null) {
            photoButton.clear();
        }
        imageUri = null;
        thumbnailUri = null;
    }

    private void resetServiceButton() {
        CustomButton btnService = (CustomButton) findViewById(R.id.report_service_button);
        btnService.clear();
        serviceName = null;
        serviceCode = null;
    }

    private void resetLocationButton() {
        CustomButton btnLocation = (CustomButton) findViewById(R.id.report_location_button);
        btnLocation.clear();
        latitude = null;
        longitude = null;
        location = null;
    }

    private void resetDescriptionTextbox() {
        EditText description = (EditText) findViewById(R.id.report_description_textbox);
        description.setText(null);
        description.setHintTextColor(Color.GRAY);
    }

    private void cleanUpForm() {
        resetPhotoButton();
        resetServiceButton();
        resetLocationButton();
        resetDescriptionTextbox();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);

        attributes = new LinkedList<Attribute>();
        attrInfoList = new LinkedList<AttributeInfo>();
        installationId = ((MainActivity) getActivity()).getInstallationId();

        new RetrieveServicesTask().execute(); // Load services-list in the background

        // Don't show the keyboard if it isn't already shown,
        // but if it was open when entering the activity, leave it open.
        // To always hide the keyboard when the activity starts: SOFT_INPUT_STATE_ALWAYS_HIDDEN
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        CustomButton btnPhoto = (CustomButton) view.findViewById(R.id.report_photo_button);
        CustomButton btnService = (CustomButton) view.findViewById(R.id.report_service_button);
        CustomButton btnLocation = (CustomButton) view.findViewById(R.id.report_location_button);

        Button btnSubmit = (Button) view.findViewById(R.id.report_next_button);

        btnPhoto.setIcon(R.drawable.ic_photo_camera_black_24dp);
        btnPhoto.setHint(getString(R.string.report_hint_photo));
        btnPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPhotoButtonClicked();
            }
        });

        btnLocation.setIcon(R.drawable.ic_location_on_black_24dp);
        btnLocation.setHint(getString(R.string.report_hint_location));
        btnLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onLocationButtonClicked();
            }
        });

        btnService.setIcon(R.drawable.ic_subject_black_24dp);
        btnService.setHint(R.string.report_hint_service);
        btnService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onServiceButtonClicked();
            }
        });

        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClicked();
            }
        });

        photoButton = btnPhoto;

        return view;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("imageUri")) {
                imageUri = Uri.parse(savedInstanceState.getString("imageUri"));
            }
            if (savedInstanceState.containsKey("thumbnailUri")) {
                thumbnailUri = Uri.parse(savedInstanceState.getString("thumbnailUri"));
            }
            if (savedInstanceState.containsKey("location")) {
                location = savedInstanceState.getString("location");
            }
            if (savedInstanceState.containsKey("latitude")) {
                latitude = savedInstanceState.getFloat("latitude");
            }
            if (savedInstanceState.containsKey("longitude")) {
                longitude = savedInstanceState.getFloat("longitude");
            }
            if (savedInstanceState.containsKey("serviceName")) {
                serviceName = savedInstanceState.getString("serviceName");
            }
            if (savedInstanceState.containsKey("serviceCode")) {
                serviceCode = savedInstanceState.getString("serviceCode");
            }
            if (savedInstanceState.containsKey("attributes")) {
                Serializable state = savedInstanceState.getSerializable("attributes");
                try {
                    attributes = (LinkedList<Attribute>) state;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
            if (savedInstanceState.containsKey("attrInfoList")) {
                Serializable state = savedInstanceState.getSerializable("attrInfoList");
                try {
                    attrInfoList = (LinkedList<AttributeInfo>) state;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (imageUri != null) {
            savedInstanceState.putString("imageUri", imageUri.toString());
        }
        if (thumbnailUri != null) {
            savedInstanceState.putString("thumbnailUri", thumbnailUri.toString());
        }
        if (location != null) {
            savedInstanceState.putString("location", location);
        }
        if (latitude != null) {
            savedInstanceState.putFloat("latitude", latitude);
        }
        if (longitude != null) {
            savedInstanceState.putFloat("longitude", longitude);
        }
        if (serviceName != null) {
            savedInstanceState.putString("serviceName", serviceName);
        }
        if (serviceCode != null) {
            savedInstanceState.putString("serviceCode", serviceCode);
        }
        if (attributes != null) {
            savedInstanceState.putSerializable("attributes", attributes);
        }
        if (attrInfoList != null) {
            savedInstanceState.putSerializable("attrInfoList", attrInfoList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePhotoButtonImage();
        updateServiceButton();
        updateLocationButton();
        //updateAttributeButtons();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        progress = null;
    }

    /**
     * Create a temporary file
     *
     * @param name
     * @param ext
     * @return File
     * @throws Exception
     */
    private File createTemporaryFile(String name, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (! tempDir.exists()) {
            tempDir.mkdir();
        }
        File file = new File(tempDir, name + ext);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        return File.createTempFile(name, ext, tempDir);
    }

    /**
     * Checks if form values are valid
     *
     * @return boolean
     */
    private boolean isValidFormContent() {

        boolean isValid = true;

        CustomButton service = (CustomButton) findViewById(R.id.report_service_button);
        CustomButton location = (CustomButton) findViewById(R.id.report_location_button);
        EditText description = (EditText) findViewById(R.id.report_description_textbox);

        if (serviceCode == null) {
            isValid = false;
            service.setError();
        }

        if (latitude == null || longitude == null) {
            isValid = false;
            location.setError();
        }

        if (description.getText().length() == 0) {
            isValid = false;
            description.setHintTextColor(Color.RED);
        }

        if (attrInfoList.size() != 0) {
            isValid = (attrInfoList.size() == attributes.size());
            // TODO: set error for each missing attribute
        }

        return isValid;
    }

    /**
     * Checks if external storage is available for read and write
     *
     * @return boolean
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void addAttributesToForm() {
        Iterator<AttributeInfo> iterator = attrInfoList.iterator();
        LinearLayout layout = (LinearLayout) findViewById(R.id.report_attributes);
        layout.removeAllViews(); // Make sure the ViewGroup is empty (i.e. has no child views)
        while (iterator.hasNext()) {
            final AttributeInfo attr = iterator.next();
            if (attr.getDatatype() != AttributeInfo.Datatype.SINGLEVALUELIST) {
                System.out.println("ATTR-INFO: " + attr.getDatatype());
                // For now we'll accept attribute type SingleValueList.
                // In a later version we'll also support the types:
                // STRING, NUMBER, DATETIME, TEXT, MULTIVALUELIST
                continue;
            }

            CustomButton button = new CustomButton(getActivity());
            button.setId(attr.hashCode());
            button.setText(attr.getDescription());
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAttributeButtonClicked(attr);
                }
            });

            layout.addView(button);
        }
    }

    private int indexOfAttribute(SingleValueAttributeWrapper a) {
        SingleValueAttributeWrapper attribute;
        boolean found = false;
        int n = attributes.size();
        int k = 0;
        int index = -1;
        while (!found && k < n) {
            attribute = (SingleValueAttributeWrapper) attributes.get(k);
            found = attribute.hasCode(a.getCode());
            if (found) index = k;
            k++;
        }
        return index;
    }

    private void onAttributeButtonClicked(AttributeInfo attribute) {
        String title = attribute.getDescription();
        final String code = attribute.getCode();
        Map<String, String> map = attribute.getValues();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] keys = new String[map.size()];
        final String[] values = new String[map.size()];
        int index = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            keys[index] = entry.getKey();
            values[index] = entry.getValue();
            index++;
        }
        builder.setTitle(title).setItems(values, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                System.out.println("SELECTED ATTRIBUTE: " + values[index]);
                updateLocationButton();
                SingleValueAttributeWrapper sva = new SingleValueAttributeWrapper(code, keys[index]);
                int aIndex = indexOfAttribute(sva);
                if (aIndex == -1) {
                    attributes.add(sva);
                } else {
                    attributes.set(aIndex, sva);
                }
            }
        });
        builder.create().show();
    }

    private void onServiceButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (services == null) {
            String msg = "Service list is unavailable";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] codes = new String[services.size()];
        final String[] values = new String[services.size()];
        int index = 0;
        for (Service item : services) {
            codes[index] = item.getServiceCode();
            values[index] = item.getServiceName();
            index++;
        }
        builder.setTitle(R.string.report_service).setItems(values, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                serviceName = values[index];
                serviceCode = codes[index];
                System.out.println("SELECTED SERVICE: " + serviceName);
                updateServiceButton();
                if (ATTRIBUTES_ENABLED) {
                    new RetrieveAttributesTask(codes[index]).execute();
                }
            }
        });
        builder.create().show();
    }

    private void onPhotoButtonClicked() {

        if (! isExternalStorageWritable()) {
            String msg = "External storage is not writable";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo;
        try {
            photo = createTemporaryFile("picture", ".bmp");
            photo.delete();
            imageUri = Uri.fromFile(photo);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        } catch (Exception e) {
            e.printStackTrace();
        }

        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void onLocationButtonClicked() {
        progress = new ProgressDialog(getContext(), R.style.CustomDialogTheme);
        progress.show();
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(getActivity()), LOCATION_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void onSubmitButtonClicked() {
        Log.d("open311", "Submit Button was clicked.");

        // Check the form result and post the service request
        if (! isValidFormContent()) {
            return;
        }

        CustomButton address = (CustomButton) findViewById(R.id.report_location_button);
        EditText description = (EditText) findViewById(R.id.report_description_textbox);

        POSTServiceRequestDataWrapper data = new POSTServiceRequestDataWrapper(
            serviceCode,
            latitude,
            longitude,
            attributes);

        SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
        String name  = settings.getString("name", null);
        String email = settings.getString("email", null);
        String phone = settings.getString("phone", null);

        if (name  != null) data.setName(name);
        if (email != null) data.setEmail(email);
        if (phone != null) data.setPhone(phone);

        data.setDeviceId(installationId)
            .setAddress(address.getText().toString())
            .setDescription(description.getText().toString());

        bgTask = new PostServiceRequestTask(Constants.ENDPOINT, data, imageUri);
        bgTask.execute();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                if (imageUri == null) return;
                try {
                    // int width = getPhotoButtonWidth();
                    // int height = Math.round((3 / (float) 4) * width); // 4:3 aspect ratio
                    // Bitmap bitmap = Image.decodeSampledBitmap(imageUri.getPath(), width, height);
                    Bitmap bitmap = Image.decodeSampledBitmap(imageUri.getPath(), 320, 240);
                    File file = createTemporaryFile("thumb", ".bmp");
                    FileOutputStream out = new FileOutputStream(file);
                    // PNG is a loss-less format, the compression factor (100) is ignored
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    thumbnailUri = Uri.fromFile(file);
                    updatePhotoButtonImage(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                imageUri = null;
            }
        }

        if (requestCode == LOCATION_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                Place place = PlacePicker.getPlace(data, getActivity());
                LatLng position = place.getLatLng();
                location = place.getName().toString();
                latitude = new Float(position.latitude);
                longitude = new Float(position.longitude);
                CustomButton location = (CustomButton) getActivity()
                        .findViewById(R.id.report_location_button);
                location.setText(place.getName());
            } else {
                System.out.println("LOCATION REQUEST RESULT CODE: " + resultCode);
            }
        }
    }

    private class PostServiceRequestTask extends AsyncTask<Void, Void, String> {

        private POSTServiceRequestData data;
        private String url;
        private Uri imageUri;
        private boolean success = true;

        public PostServiceRequestTask(String url, POSTServiceRequestData data, Uri imageUri) {
            this.url = url;
            this.data = data;
            this.imageUri = imageUri;
        }

        /**
         * Post service request in the background.
         *
         * @param ignore the parameters of the task
         */
        @Override
        protected String doInBackground(Void... ignore) {
            String result;
            try {
                APIWrapperFactory wrapperFactory = new APIWrapperFactory(url);
                if (imageUri != null) {
                    MultipartHTTPNetworkManager networkManager = new MultipartHTTPNetworkManager(imageUri);
                    wrapperFactory.setNetworkManager(networkManager);
                }
                wrapperFactory.setApiKey(Constants.API_KEY);

                APIWrapper wrapper = wrapperFactory.build();
                POSTServiceRequestResponse response = wrapper.postServiceRequest(data);

                if (response != null) {
                    success = true;
                    result = getString(R.string.report_success_message);
                    saveServiceRequestId(response.getServiceRequestId());
                    saveHasReportsSetting(true);
                    System.out.println("SERVICE REQUEST ID: " + response.getServiceRequestId());
                } else {
                    success = false;
                    result = getString(R.string.report_failure_message);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                result = e.getMessage();
            } catch (APIWrapperException e) {
                e.printStackTrace();
                result = e.getMessage();
            }
            return result;
        }

        private boolean saveHasReportsSetting(boolean flag) {
            SharedPreferences settings = Utils.getSettings(getActivity());
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("has_reports", flag);
            return editor.commit();
        }

        private boolean saveServiceRequestId(String id) {
            MyReportsFile file = new MyReportsFile(getContext());
            try {
                return file.addServiceRequestId(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onPreExecute() {
            progress = new ProgressDialog(getContext());
            progress.setTitle(getString(R.string.report_dialog_title));
            progress.setMessage(getString(R.string.report_dialog_message) + "...");
            progress.show();
        }

        protected void onPostExecute(String result) {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
            System.out.println("POST RESULT: " + result);
            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
            if (success) cleanUpForm();
        }
    }

    private class RetrieveAttributesTask extends AsyncTask<Void, Void, Integer> {

        private String serviceCode;

        public RetrieveAttributesTask(String serviceCode) {
            this.serviceCode = serviceCode;
        }

        /**
         * Get service attributes in the background.
         *
         * @param ignore the parameters of the task
         */
        @Override
        protected Integer doInBackground(Void... ignore) {
            String url = Constants.ENDPOINT + "/services/" + this.serviceCode + ".xml";
            APIWrapper wrapper;
            ServiceDefinition definition;
            Integer count = 0;
            attrInfoList.clear(); // Start with an empty list
            try {
                wrapper = new APIWrapperFactory(url).build();
                definition = wrapper.getServiceDefinition(this.serviceCode);
                Iterator iterator = definition.getAttributes().iterator();
                while (iterator.hasNext()) {
                    attrInfoList.add((AttributeInfo) iterator.next());
                }
                count = attrInfoList.size();
                System.out.println("ATTRIBUTE COUNT: " + count);

            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return count;
        }

        protected void onPostExecute(Integer count) {
            if (count == 0) {
                System.out.println("THE SELECTED SERVICE HAS NO ATTRIBUTES");
            }
            addAttributesToForm();
        }
    }

    private class RetrieveServicesTask extends AsyncTask<Void, Void, Void> {
        /**
         * Get services in the background.
         *
         * @param ignore the parameters of the task
         */
        @Override
        protected Void doInBackground(Void... ignore) {
            if (services != null) return null;
            APIWrapper wrapper;
            try {
                wrapper = new APIWrapperFactory(Constants.ENDPOINT).build();
                services = wrapper.getServiceList();

            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Add a white border to a bitmap image
     *
     * @param bmp Bitmap
     * @param borderSize int
     * @return Bitmap
     */
    private Bitmap addWhiteBorder(Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(
                bmp.getWidth() + borderSize * 2,
                bmp.getHeight() + borderSize * 2,
                bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }
}
