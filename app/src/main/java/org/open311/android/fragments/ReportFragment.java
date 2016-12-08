package org.open311.android.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
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
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.ViewSwitcher;

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
import org.codeforamerica.open311.facade.data.operations.POSTServiceRequestData;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.network.HTTPNetworkManager;
import org.open311.android.MainActivity;
import org.open311.android.MapActivity;
import org.open311.android.R;
import org.open311.android.SoundRecorderActivity;
import org.open311.android.helpers.MyReportsFile;
import org.open311.android.network.POSTServiceRequestDataWrapper;
import org.open311.android.adapters.ServicesAdapter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.content.Intent.ACTION_EDIT;
import static org.open311.android.helpers.Utils.hideKeyBoard;

/**
 * Report {@link Fragment} subclass.
 */
public class ReportFragment extends Fragment {

    private static final String LOG_TAG = "ReportFragment";

    private LinkedList<AttributeInfo> attrInfoList;
    private LinkedList<Attribute> attributes;
    private List<Service> services;
    private String imageUri;
    private Uri audioUri;
    private ProgressDialog progress;

    private String location;
    private String serviceName;
    private String serviceCode;
    private String installationId;
    private Float latitude;
    private Float longitude;
    private String source;
    private ImageView playBtn;
    private MediaPlayer mMediaPlayer;
    private ViewSwitcher photoviewSwitcher;
    private ViewSwitcher audioviewSwitcher;
    private AudioStatus mAudioStatus;
    private int mPlayTime = 0;

    public static final int CAMERA_REQUEST = 101;
    public static final int LOCATION_REQUEST = 102;
    public static final int GALLERY_IMAGE_REQUEST = 103;
    public static final int READ_STORAGE_REQUEST = 104;
    public static final int RECORDER_REQUEST = 105;
    public static final int GALLERY_AUDIO_REQUEST = 107;

    private static final boolean ATTRIBUTES_ENABLED = false;

    enum AudioStatus {
        PLAYING("Playing", 0),
        STOPPED("Stopped", 2);

        private String stringValue;
        private int intValue;

        AudioStatus(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    enum GalleryType {
        IMAGE("Image", 0),
        AUDIO("Audio", 1);

        private String stringValue;
        private int intValue;

        GalleryType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    public ReportFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        Log.d(LOG_TAG, "onCreateView");
        if (state != null)
            Log.d(LOG_TAG, state.toString());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        LinearLayoutCompat btnPhoto = (LinearLayoutCompat) view.findViewById(R.id.photoButton);
        LinearLayoutCompat layoutPhoto = (LinearLayoutCompat) view.findViewById(R.id.photoLayout);
        LinearLayoutCompat btnAudio = (LinearLayoutCompat) view.findViewById(R.id.audioButton);
        LinearLayoutCompat layoutAudio = (LinearLayoutCompat) view.findViewById(R.id.audioLayout);
        LinearLayoutCompat btnService = (LinearLayoutCompat) view.findViewById(R.id.serviceButton);
        LinearLayoutCompat btnLocation = (LinearLayoutCompat) view.findViewById(R.id.locationButton);
        playBtn = (ImageView) view.findViewById(R.id.audioView2);
        View descriptionView = view.findViewById(R.id.report_description_textbox);
        FloatingActionButton btnSubmit = (FloatingActionButton) view.findViewById(R.id.report_submit);
        photoviewSwitcher = (ViewSwitcher) view.findViewById(R.id.report_photoviewswitcher);
        audioviewSwitcher = (ViewSwitcher) view.findViewById(R.id.report_audioviewswitcher);
        ImageView photoPlaceholder = (ImageView) view.findViewById((R.id.photoPlaceholder));

        //Hide the keyboard unless the descriptionView is selected
        descriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    v.clearFocus();
                    hideKeyBoard(getActivity());
                }
            }
        });
        photoPlaceholder.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onPhotoButtonClicked();
            }
        });
        btnPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onPhotoButtonClicked();
            }
        });
        layoutPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onPhotoButtonClicked();
            }
        });
        btnAudio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onAudioButtonClicked();
            }
        });
        layoutAudio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onAudioButtonClicked();
            }
        });


        btnLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onLocationButtonClicked();
            }
        });

        btnService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onServiceButtonClicked();
            }
        });

        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onSubmitButtonClicked();
            }
        });

        return view;
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

    public void playAudio() {
        try {

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getContext(), audioUri);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mPlayTime > 0) {
                        mMediaPlayer.seekTo(mPlayTime);
                    }

                    playBtn.setImageResource(R.drawable.ic_stop);
                    mMediaPlayer.start();
                    mAudioStatus = AudioStatus.PLAYING;
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playBtn.setImageResource(R.drawable.ic_play_arrow);
                    mMediaPlayer.release();
                    mPlayTime = 0;
                    mAudioStatus = AudioStatus.STOPPED;
                }
            });

        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
            mAudioStatus = AudioStatus.STOPPED;
        }
    }

    public void updateAudio() {
        if (audioUri != null) {
            Log.d(LOG_TAG, "updateAudio " + audioUri);
            TextView filename = (TextView) getActivity().findViewById(R.id.audio_text2);
            OnClickListener playClicked = new OnClickListener() {
                public void onClick(View v) {
                    if (mAudioStatus == AudioStatus.PLAYING) {
                        playBtn.setImageResource(R.drawable.ic_play_arrow);
                        mMediaPlayer.release();
                        mPlayTime = 0;
                        mAudioStatus = AudioStatus.STOPPED;
                    } else {
                        playAudio();
                    }
                }
            };
            playBtn.setOnClickListener(playClicked);
            filename.setText(niceName(audioUri));
            audioviewSwitcher.setDisplayedChild(1);
        } else {
            resetPhoto();
        }
    }

    private String niceName(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return uri.getLastPathSegment();
        } else if (scheme.equals("content")) {
            Cursor returnCursor = getContext().getContentResolver().query(uri, null, null, null, null);
            int nameIndex = 0;
            if (returnCursor != null) {
                nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                return returnCursor.getString(nameIndex);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void updatePhoto(Boolean broadcast) {
        if (imageUri != null) {
            Log.d(LOG_TAG, "updatePhoto " + imageUri);
            if (broadcast) {
                // Tell the media gallery the photo is created
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(imageUri);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                getContext().sendBroadcast(mediaScanIntent);
            }
            ImageView image = (ImageView) getActivity().findViewById(R.id.photoPlaceholder);
            Log.d(LOG_TAG, "imageView " + image.toString());
            Glide.with(getContext()).load(imageUri).asBitmap().into(image);
            Log.d(LOG_TAG, "gonna switch!");
            photoviewSwitcher.setDisplayedChild(1);
        } else {
            resetPhoto();
        }
    }

    private void resetAudio() {
        audioviewSwitcher.setDisplayedChild(0);
        audioUri = null;
        TextView audioText = (TextView) getActivity().findViewById(R.id.audio_text);
        audioText.setText(R.string.report_hint_sound);
    }

    private void resetPhoto() {
        photoviewSwitcher.setDisplayedChild(0);
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
        resetAudio();
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
    @SuppressWarnings("unchecked")
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
        Log.d(LOG_TAG, "onResume");
        super.onResume();
        updatePhoto(true);
        updateAudio();
        updateService();
        updateLocation();
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
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
    private boolean validate() {

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
            String result = getString(R.string.failure_posting_service);
            Snackbar.make(getView(), result, Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            showFab();
        }

        return isValid;
    }

    private void showFab() {
        View fab = getActivity().findViewById(R.id.report_submit);
        fab.setVisibility(View.VISIBLE);
    }

    private Boolean checkAnonymous() {
        SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
        String email = settings.getString("email", null);
        String phone = settings.getString("phone", null);
        Boolean anonymous = true;
        if (email != null) {
            anonymous = false;
        }

        if (phone != null) {
            anonymous = false;
        }
        return anonymous;
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
        LinearLayoutCompat layout = (LinearLayoutCompat) getActivity().findViewById(R.id.report_attributes);
        layout.removeAllViews(); // Make sure the ViewGroup is empty (i.e. has no child views)
        while (iterator.hasNext()) {
            final AttributeInfo attr = iterator.next();
            if (attr.getDatatype() != AttributeInfo.Datatype.SINGLEVALUELIST) {
                Log.d(LOG_TAG, "ATTR-INFO: " + attr.getDatatype());
            }
        }
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
        builder.setTitle(R.string.report_hint_service)
                .setCancelable(false)
                .setAdapter(new ServicesAdapter(getActivity(), services),
                        new DialogInterface.OnClickListener() {
                            @Override
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
     * User clicked the Button to add audio to the request.
     * We present the user with a dialog to select audio from storage, or use the recorder.
     */
    private void onAudioButtonClicked() {
        final CharSequence[] items = {getString(R.string.recorder), getString(R.string.gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        builder.setTitle(getString(R.string.choose_audio_source));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getString(R.string.recorder))) {
                    String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!hasPermissions(getContext().getApplicationContext(), PERMISSIONS)) {
                        requestPermissions(PERMISSIONS, RECORDER_REQUEST);
                    } else {
                        handleRecorder();
                    }
                } else if (items[item].equals(getString(R.string.gallery))) {
                    if (ContextCompat.checkSelfPermission(getContext().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_STORAGE_REQUEST);
                    } else {
                        handleGallery(GalleryType.AUDIO);
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
                        handleGallery(GalleryType.IMAGE);
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
    private void handleGallery(GalleryType type) {
        Log.d(LOG_TAG, "HandleGallery");
        View v = getActivity().findViewById(R.id.report_submit);
        if (!isExternalStorageWritable()) {
            String msg = getString(R.string.storageNotWritable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        // Pass the GalleryType to the intent
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.putExtra("GalleryType", type.intValue);
        intent.putExtra("return_data", true);
        // Initiate the correct type of Gallery
        switch (type) {
            case AUDIO:
                intent.setType("audio/*");
                startActivityForResult(intent, GALLERY_AUDIO_REQUEST);
                break;
            case IMAGE:
            default:
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_IMAGE_REQUEST);
        }


    }

    private void handleRecorder() {
        Log.d(LOG_TAG, "HandleRecorder");
        View v = getActivity().findViewById(R.id.report_submit);
        if (!isExternalStorageWritable()) {
            String msg = getString(R.string.storageNotWritable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        Intent audioIntent = new Intent(getActivity(), SoundRecorderActivity.class);
        try {
            File audio = createFile(GalleryType.AUDIO);
            audioIntent.setAction(ACTION_EDIT);
            audioIntent.setData(Uri.fromFile(audio));
            startActivityForResult(audioIntent, RECORDER_REQUEST);

        } catch (IOException ex) {
            String msg = getString(R.string.storageNotWritable);
            Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                    .show();
        }
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
            File photo;
            try {
                photo = createFile(GalleryType.IMAGE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

            } catch (IOException ex) {
                String msg = getString(R.string.storageNotWritable);
                Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
                        .show();
            }
        }


    }

    private void onSubmitButtonClicked() {
        Log.d(LOG_TAG, "Submit Button was clicked.");

        // Check the form result and post the service request
        if (!validate()) {
            return;
        }

        final TextView address = (TextView) getActivity().findViewById(R.id.location_text);
        final EditText description = (EditText) getActivity().findViewById(R.id.report_description_textbox);

        final POSTServiceRequestDataWrapper data = new POSTServiceRequestDataWrapper(
                serviceCode,
                latitude,
                longitude,
                attributes);

        if (!checkAnonymous()) {
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
        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
            builder.setTitle(getString(R.string.post_anonymous))
                    .setMessage(getString(R.string.post_anonymous_description))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO redirect to profile
                            TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
                            TabLayout.Tab tab = tabLayout.getTabAt(2);
                            if (tab != null) {
                                tab.select();
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.no_anonymous), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
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
                    })
                    .show();
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        if (requestCode == GALLERY_AUDIO_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                audioUri = data.getData();
                updateAudio();
            } else {
                resetAudio();
            }
        }
        if (requestCode == RECORDER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                audioUri = data.getData();
                updateAudio();
            } else {
                resetAudio();
            }
        }
        if (requestCode == GALLERY_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                imageUri = data.getData().toString();
                updatePhoto(false);
            } else {
                resetPhoto();
            }
        }

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (imageUri == null) return;
                updatePhoto(true);
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

        PostServiceRequestTask(POSTServiceRequestData data, Bitmap bitmap) {
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

        RetrieveAttributesTask(String serviceCode) {
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
                for (AttributeInfo o : definition.getAttributes()) {
                    attrInfoList.add(o);
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
        if (requestCode == GALLERY_AUDIO_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleGallery(GalleryType.AUDIO);
        }
        if (requestCode == GALLERY_IMAGE_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleGallery(GalleryType.IMAGE);
        }
        if (requestCode == LOCATION_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleLocation();
        }
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

    private File createFile(GalleryType type) throws IOException {
        String prefix;
        String extension;
        File storageDir;
        // Initiate the correct type of Gallery
        switch (type) {
            case AUDIO:
                prefix = "REC311_";
                extension = ".3gp";
                storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                break;
            case IMAGE:
            default:
                prefix = "IMG311_";
                extension = ".jpg";
                storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        }

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = prefix + timeStamp + "_";

        File file = File.createTempFile(
                fileName,  /* prefix */
                extension,         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        switch (type) {
            case AUDIO:
                audioUri = Uri.fromFile(file);
                break;
            case IMAGE:
            default:
                imageUri = Uri.fromFile(file).getPath();
        }
        return file;
    }

}
