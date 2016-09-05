package org.open311.android.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.EndpointType;
import org.codeforamerica.open311.facade.data.Attribute;
import org.codeforamerica.open311.facade.data.AttributeInfo;
import org.codeforamerica.open311.facade.data.POSTServiceRequestResponse;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.data.ServiceDefinition;
import org.codeforamerica.open311.facade.data.Value;
import org.codeforamerica.open311.facade.data.operations.POSTServiceRequestData;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.network.HTTPNetworkManager;
import org.open311.android.MainActivity;
import org.open311.android.MapActivity;
import org.open311.android.R;
import org.open311.android.helpers.MyReportsFile;
import org.open311.android.network.POSTServiceRequestDataWrapper;
import org.open311.android.helpers.SingleValueAttributeWrapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import io.tus.android.client.TusAndroidUpload;
import io.tus.android.client.TusPreferencesURLStore;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;


/**
 * Report {@link Fragment} subclass.
 */
public class ReportFragment extends Fragment {

    private static final String LOG_TAG = "ReportFragment";

    private LinkedList<AttributeInfo> attrInfoList;
    private LinkedList<Attribute> attributes;
    private List<Service> services;
    private String imageUri;
    private ProgressDialog progress;
    private TusClient client;

    private String location;
    private String serviceName;
    private String serviceCode;
    private String installationId;
    private Float latitude;
    private Float longitude;
    private String source;

    public static final int CAMERA_REQUEST = 101;
    public static final int LOCATION_REQUEST = 102;
    public static final int GALLERY_REQUEST = 103;
    public static final int READ_STORAGE_REQUEST = 104;

    private static final boolean ATTRIBUTES_ENABLED = false;


    public ReportFragment() {
        // Required empty public constructor
    }

    private void updateService() {
        if (serviceName != null) {
            TextView serviceTextView = (TextView) getActivity().findViewById(R.id.service_text);
            serviceTextView.setText(serviceName);
            ImageView icon = (ImageView) getActivity().findViewById(R.id.serviceView);
            icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
    }

    private void updateLocation() {
        try {
            TextView text = (TextView) getActivity().findViewById(R.id.location_text);
            if (location == null && source != null) {
                String coordText = String.format(Locale.getDefault(), "(%f, %f)", latitude, longitude);
                Log.d(LOG_TAG, "Coordinates: " + coordText);
                text.setText(coordText);
            }
            if (location != null && source != null) {
                Log.d(LOG_TAG, "Address: " + location);
                text.setText(location);
            } else {
                text.setText(R.string.report_hint_location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * When we get a Return from the Camera, the image is in a temp file, transform it to string with getPath()
     *
     * @param imageFromTemp temporary image, this gets created when using the camera
     */
    public void updatePhoto(Uri imageFromTemp) {
        imageUri = imageFromTemp.getPath();
        updatePhoto();
    }

    public void updatePhoto() {
        if (imageUri != null) {
            Log.d(LOG_TAG, "updatePhoto " + imageUri);
            // Tell the media gallery the photo is created
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(imageUri);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            getContext().sendBroadcast(mediaScanIntent);

            RelativeLayout layout = (RelativeLayout) getActivity().findViewById(R.id.photoLayout);
            ImageView image = (ImageView) getActivity().findViewById(R.id.photoPlaceholder);
            Glide.with(getContext()).load(imageUri).asBitmap().into(image);
            layout.setVisibility(View.VISIBLE);
        } else {
            resetPhoto();
        }
    }

    private void resetPhoto() {
        RelativeLayout layout = (RelativeLayout) getActivity().findViewById(R.id.photoLayout);
        layout.setVisibility(View.INVISIBLE);
        imageUri = null;
        TextView photoText = (TextView) getActivity().findViewById(R.id.photo_text);
        photoText.setText(R.string.report_hint_photo);
    }

    private void resetService() {
        serviceName = null;
        serviceCode = null;
        TextView serviceText = (TextView) getActivity().findViewById(R.id.service_text);
        serviceText.setText(R.string.report_hint_service);
    }

    private void resetLocation() {
        latitude = null;
        longitude = null;
        location = null;
        TextView locationText = (TextView) getActivity().findViewById(R.id.location_text);
        locationText.setText(R.string.report_hint_location);
    }

    private void resetDescription() {
        EditText description = (EditText) getActivity().findViewById(R.id.report_description_textbox);
        description.setText(null);

    }

    private void resetAll() {
        resetPhoto();
        resetService();
        resetLocation();
        resetDescription();
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
        Log.d(LOG_TAG, "onCreateView");
        if (state != null)
            Log.d(LOG_TAG, state.toString());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        RelativeLayout btnPhoto = (RelativeLayout) view.findViewById(R.id.photoButton);
        RelativeLayout btnService = (RelativeLayout) view.findViewById(R.id.serviceButton);
        RelativeLayout btnLocation = (RelativeLayout) view.findViewById(R.id.locationButton);
        View descriptionView = view.findViewById(R.id.report_description_textbox);
        FloatingActionButton btnSubmit = (FloatingActionButton) view.findViewById(R.id.report_submit);

        //Hide the keyboard unless the descriptionView is selected
        descriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    v.clearFocus();
                    hideKeyBoard(v);
                }
            }
        });
        btnPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPhotoButtonClicked();
            }
        });

        btnLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onLocationButtonClicked();
            }
        });

        btnService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onServiceButtonClicked();
            }
        });

        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClicked(v);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("imageUri")) {
                imageUri = savedInstanceState.getString("imageUri");
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
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
        if (imageUri != null) {
            savedInstanceState.putString("imageUri", imageUri);
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
        updatePhoto();
        updateService();
        updateLocation();
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
     * Checks if form values are valid
     *
     * @return boolean
     */
    private boolean isValidFormContent(View v) {

        boolean isValid = true;

        EditText description = (EditText) getActivity().findViewById(R.id.report_description_textbox);

        String descText = description.getText().toString();

        // Filter description text. Replace everything that is not a word character,
        // whitespace or one of the symbols .?(),!:;@
        String pattern = "[^\\w\\s\\.\\?\\(\\),!:;@]";
        description.setText(
                Normalizer.normalize(descText, Normalizer.Form.NFD).replaceAll(pattern, "")
        );

        if (serviceCode == null) {
            isValid = false;
            resetLocation();
        }

        if (latitude == null || longitude == null) {
            isValid = false;
            resetLocation();
        }

        if (description.getText().length() == 0) {
            isValid = false;
            description.setHintTextColor(Color.RED);
        }

        if (attrInfoList.size() != 0) {
            isValid = (attrInfoList.size() == attributes.size());

        }
        if (!isValid) {
            // TODO: Construct a snackbar with text if invalid
            String result = getString(R.string.failure_posting_service);
            Snackbar.make(v, result, Snackbar.LENGTH_SHORT)
                    .show();
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
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void addAttributesToForm() {
        // TODO process attributes
        Iterator<AttributeInfo> iterator = attrInfoList.iterator();
        LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.report_attributes);
        layout.removeAllViews(); // Make sure the ViewGroup is empty (i.e. has no child views)
        while (iterator.hasNext()) {
            final AttributeInfo attr = iterator.next();
            if (attr.getDatatype() != AttributeInfo.Datatype.SINGLEVALUELIST) {
                Log.d(LOG_TAG, "ATTR-INFO: " + attr.getDatatype());
                continue;
            }

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
        Value[] map = attribute.getValues();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] keys = new String[map.length];
        final String[] values = new String[map.length];
        for (int index = 0; index < map.length; ++index) {
            keys[index] = map[index].getKey();
            values[index] = map[index].getName();
            index++;
        }
        builder.setTitle(title).setItems(values, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "SELECTED ATTRIBUTE: " + values[index]);
                updateLocation();
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
            View v = getActivity().findViewById(R.id.report_submit);
            String msg = getString(R.string.serviceListUnavailable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
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
                Log.d(LOG_TAG, "SELECTED SERVICE: " + serviceName);
                updateService();
                if (ATTRIBUTES_ENABLED) {
                    new RetrieveAttributesTask(codes[index]).execute();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });
        builder.show();
    }


    /**
     * User clicked the Button to add a Picture to the request.
     * We present the user with a dialog to select a picture from the
     * gallery, or use the camera to pick one.
     */
    private void onPhotoButtonClicked() {
        final CharSequence[] items = {getString(R.string.camera), getString(R.string.gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        builder.setTitle(getString(R.string.choose_media_source));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getString(R.string.camera))) {
                    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!hasPermissions(getContext().getApplicationContext(), PERMISSIONS)) {
                        requestPermissions(PERMISSIONS, CAMERA_REQUEST);
                    } else {
                        handleCamera();
                    }
                } else if (items[item].equals(getString(R.string.gallery))) {
                    if (ContextCompat.checkSelfPermission(getContext().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_STORAGE_REQUEST);
                    } else {
                        handleGallery();
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        builder.show();

    }

    private void onLocationButtonClicked() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission_group.LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
        } else {
            handleLocation();
        }


    }

    private void handleLocation() {
        // todo, save to the backstack so that when we get back, we remain where we were.
        Intent intent = new Intent(getActivity(), MapActivity.class);
        startActivityForResult(intent, LOCATION_REQUEST);
    }

    /**
     * User selected to use the Gallery as picture source
     * Because we check permissions for Android 6+, this function is also used to propagate
     * actions from the checker
     */
    private void handleGallery() {
        Log.d(LOG_TAG, "HandleGallery");
        View v = getActivity().findViewById(R.id.report_submit);
        if (!isExternalStorageWritable()) {
            String msg = getString(R.string.storageNotWritable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra("return_data", true);
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    /**
     * User selected to use the Camera as picture source.
     * Because we check permissions for Android 6+, this function is also used to propagate
     * actions from the checker
     */
    private void handleCamera() {
        Log.d(LOG_TAG, "HandleCamera");
        View v = getActivity().findViewById(R.id.report_submit);
        if (!isExternalStorageWritable()) {
            String msg = getString(R.string.storageNotWritable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getContext().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photo = null;
            try {
                photo = createImageFile();
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

            } catch (IOException ex) {
                String msg = getString(R.string.storageNotWritable);
                Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                        .show();
            }
        }


    }

    private void onSubmitButtonClicked(View v) {
        Log.d(LOG_TAG, "Submit Button was clicked.");

        // Check the form result and post the service request
        if (!isValidFormContent(v)) {
            return;
        }

        TextView address = (TextView) getActivity().findViewById(R.id.location_text);
        EditText description = (EditText) getActivity().findViewById(R.id.report_description_textbox);

        final POSTServiceRequestDataWrapper data = new POSTServiceRequestDataWrapper(
                serviceCode,
                latitude,
                longitude,
                attributes);

        SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
        String name = settings.getString("name", null);
        String email = settings.getString("email", null);
        String phone = settings.getString("phone", null);

        if (name != null) data.setName(name);
        if (email != null) data.setEmail(email);
        if (phone != null) data.setPhone(phone);

        data.setDeviceId(installationId)
                .setAddress(address.getText().toString())
                .setDescription(description.getText().toString());

        if (imageUri != null) {
            Glide.with(getActivity().getApplicationContext())
                    .load(imageUri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            PostServiceRequestTask bgTask = new PostServiceRequestTask(data, bitmap);
                            bgTask.execute();
                        }
                    });
        } else {
            PostServiceRequestTask bgTask = new PostServiceRequestTask(data, null);
            bgTask.execute();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }

        if (requestCode == GALLERY_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                imageUri = data.getData().toString();
                updatePhoto();
            } else {
                resetPhoto();
            }

        }
        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (imageUri == null) return;
                updatePhoto();
            } else {
                resetPhoto();
            }
        }

        if (requestCode == LOCATION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle tempBundle = data.getExtras();
                location = tempBundle.getString("address_string");
                latitude = tempBundle.getFloat("latitude");
                longitude = tempBundle.getFloat("longitude");
                source = tempBundle.getString("source");
                updateLocation();
            }
        }
    }

    private class PostServiceRequestTask extends AsyncTask<Void, Void, String> {

        private POSTServiceRequestData data;
        private Bitmap bitmap;
        private boolean success = true;

        public PostServiceRequestTask(POSTServiceRequestData data, Bitmap bitmap) {
            this.data = data;
            this.bitmap = bitmap;
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
                APIWrapperFactory wrapperFactory = new APIWrapperFactory(((MainActivity) getActivity()).getCurrentCity(), EndpointType.PRODUCTION);
                if (imageUri != null) {
                    HTTPNetworkManager networkManager = new HTTPNetworkManager(bitmap);
                    wrapperFactory.setNetworkManager(networkManager);
                }
                wrapperFactory.setApiKey(((MainActivity) getActivity()).getCurrentCity().getApiKey());

                APIWrapper wrapper = wrapperFactory.build();
                POSTServiceRequestResponse response = wrapper.postServiceRequest(data);

                if (response != null) {
                    success = true;
                    result = response.getServiceNotice();
                    saveServiceRequestId(response.getServiceRequestId());
                    Log.d(LOG_TAG, "SERVICE REQUEST ID: " + response.getServiceRequestId());
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
            Log.d(LOG_TAG, "POST RESULT: " + result);

            if (success) resetAll();

            MyReportsFile file = new MyReportsFile(getContext());
            int reqs = file.getServiceRequestLength();
            Log.d(LOG_TAG, "requests for user: " + reqs);

            new AlertDialog.Builder(getContext())
                    .setTitle(getString(R.string.report_dialog_title))
                    .setMessage(result)
                    .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .show();
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
            APIWrapper wrapper;
            ServiceDefinition definition;
            Integer count = 0;
            attrInfoList.clear(); // Start with an empty list
            try {

                wrapper = new APIWrapperFactory(((MainActivity) getActivity()).getCurrentCity(), EndpointType.PRODUCTION).build();
                definition = wrapper.getServiceDefinition(this.serviceCode);
                Iterator iterator = definition.getAttributes().iterator();
                while (iterator.hasNext()) {
                    attrInfoList.add((AttributeInfo) iterator.next());
                }
                count = attrInfoList.size();
                Log.d(LOG_TAG, "ATTRIBUTE COUNT: " + count);

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
                Log.d(LOG_TAG, "THE SELECTED SERVICE HAS NO ATTRIBUTES");
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
            services = ((MainActivity) getActivity()).getServices();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionResult");
        if (requestCode == CAMERA_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleCamera();
        }
        if (requestCode == GALLERY_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleGallery();
        }
        if (requestCode == LOCATION_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleLocation();
        }
    }

    /**
     * Create a temporary file
     *
     * @param name Filename
     * @param ext  File Extension
     * @return File
     * @throws Exception
     */
    private File createTemporaryFile(String name, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File file = new File(tempDir, name + ext);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        return File.createTempFile(name, ext, tempDir);
    }

    /**
     * Helper function to check multiple permissions, even with multiple.
     *
     * @param context     the context from which to check permissions
     * @param permissions the permissions to be checked
     * @return if permissions are granted or not
     */

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void hideKeyBoard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private class UploadTask extends AsyncTask<Void, Long, URL> {
        private TusClient client;
        private TusUpload upload;
        private Exception exception;

        public UploadTask(TusClient client, TusUpload upload) {
            this.client = client;
            this.upload = upload;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getContext());
            progress.setTitle(getString(R.string.report_upload_title));
            progress.setMessage(getString(R.string.report_upload_started) + "...");
            progress.show();
            //activity.setPauseButtonEnabled(true);
        }

        @Override
        protected void onPostExecute(URL uploadURL) {
            //activity.setStatus("Upload finished!\n" + uploadURL.toString());
            //activity.setPauseButtonEnabled(false);
        }

        @Override
        protected void onCancelled() {
            //if(exception != null) {
            //    activity.showError(exception);
            //}

            //activity.setPauseButtonEnabled(false);
        }

        @Override
        protected void onProgressUpdate(Long... updates) {

            long uploadedBytes = updates[0];
            long totalBytes = updates[1];
            progress.setMessage(String.format(
                    getString(R.string.report_upload_progress), uploadedBytes, totalBytes)
            );
            //activity.setStatus(String.format("Uploaded %d/%d.", uploadedBytes, totalBytes));
            //activity.setUploadProgress((int) ((double) uploadedBytes / totalBytes * 100));
        }

        @Override
        protected URL doInBackground(Void... params) {
            try {
                TusUploader uploader = client.resumeOrCreateUpload(upload);
                long totalBytes = upload.getSize();
                long uploadedBytes;

                // Upload file in 10KB chunks
                uploader.setChunkSize(10 * 1024);

                while (!isCancelled() && uploader.uploadChunk() > 0) {
                    uploadedBytes = uploader.getOffset();
                    publishProgress(uploadedBytes, totalBytes);
                }

                uploader.finish();
                return uploader.getUploadURL();

            } catch (Exception e) {
                exception = e;
                cancel(true);
            }
            return null;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        imageUri = Uri.fromFile(image).getPath();
        return image;
    }
}
