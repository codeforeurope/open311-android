package org.open311.android.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Rect;
import android.media.MediaPlayer;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.codeforamerica.open311.facade.APIWrapper;
import org.codeforamerica.open311.facade.APIWrapperFactory;
import org.codeforamerica.open311.facade.EndpointType;
import org.codeforamerica.open311.facade.data.Attribute;
import org.codeforamerica.open311.facade.data.AttributeInfo;
import org.codeforamerica.open311.facade.data.POSTServiceRequestResponse;
import org.codeforamerica.open311.facade.data.Server;
import org.codeforamerica.open311.facade.data.Service;
import org.codeforamerica.open311.facade.data.ServiceDefinition;
import org.codeforamerica.open311.facade.data.operations.POSTServiceRequestData;
import org.codeforamerica.open311.facade.exceptions.APIWrapperException;
import org.codeforamerica.open311.internals.caching.AndroidCache;
import org.open311.android.MainActivity;
import org.open311.android.MapActivity;
import org.open311.android.R;
import org.open311.android.SoundRecorderActivity;

import org.open311.android.helpers.MyReportsFile;
import org.open311.android.helpers.Utils;
import org.open311.android.models.Attachment;
import org.open311.android.models.UploadResult;
import org.open311.android.network.POSTServiceRequestDataWrapper;
import org.open311.android.adapters.ServicesAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Intent.ACTION_EDIT;
import static org.open311.android.helpers.Utils.getSettings;
import static org.open311.android.helpers.Utils.hideKeyBoard;
import static org.open311.android.helpers.Utils.updateReports;

/**
 * Report {@link Fragment} subclass.
 */
public class ReportFragment extends Fragment {

    private static final String LOG_TAG = "ReportFragment";

    private LinkedList<AttributeInfo> attrInfoList;
    private LinkedList<Attribute> attributes;
    private LinkedList<Attachment> attachments;
    private List<Service> services;

    private ProgressDialog progress;
    private File photo;
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
    private FloatingActionButton mHideKeyboard;
    private FloatingActionButton mSubmitBtn;
    private EditText mDescriptionView;
    private int mPlayTime = 0;
    public static final int CAMERA_REQUEST = 101;
    public static final int LOCATION_REQUEST = 102;
    public static final int GALLERY_IMAGE_REQUEST = 103;
    public static final int READ_STORAGE_REQUEST = 104;
    public static final int RECORDER_REQUEST = 105;
    public static final int GALLERY_AUDIO_REQUEST = 107;

    private static final boolean ATTRIBUTES_ENABLED = false;
    private SharedPreferences settings;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        Log.d(LOG_TAG, "onCreateView");
        final View view = inflater.inflate(R.layout.fragment_report, container, false);

        LinearLayoutCompat btnPhoto = (LinearLayoutCompat) view.findViewById(R.id.photoButton);
        LinearLayoutCompat layoutPhoto = (LinearLayoutCompat) view.findViewById(R.id.photoLayout);
        LinearLayoutCompat btnAudio = (LinearLayoutCompat) view.findViewById(R.id.audioButton);
        LinearLayoutCompat layoutAudio = (LinearLayoutCompat) view.findViewById(R.id.audioLayout);
        LinearLayoutCompat btnService = (LinearLayoutCompat) view.findViewById(R.id.serviceButton);
        LinearLayoutCompat btnLocation = (LinearLayoutCompat) view.findViewById(R.id.locationButton);
        playBtn = (ImageView) view.findViewById(R.id.audioView2);
        mDescriptionView = (EditText) view.findViewById(R.id.report_description_textbox);
        mSubmitBtn = (FloatingActionButton) view.findViewById(R.id.report_submit);
        mHideKeyboard = (FloatingActionButton) view.findViewById(R.id.report_keyboard_close);
        photoviewSwitcher = (ViewSwitcher) view.findViewById(R.id.report_photoviewswitcher);
        audioviewSwitcher = (ViewSwitcher) view.findViewById(R.id.report_audioviewswitcher);
        ImageView photoPlaceholder = (ImageView) view.findViewById((R.id.photoPlaceholder));

        // Load services-list in the background
        new RetrieveServicesTask().execute();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    mHideKeyboard.setVisibility(View.VISIBLE);
                } else {
                    // keyboard is closed
                    mHideKeyboard.setVisibility(View.INVISIBLE);
                }
            }
        });
        mDescriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    v.clearFocus();
                    hideKeyBoard(getActivity());
                } else {
                    //Show a close keyboard Button
                    mDescriptionView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            validate();
                        }
                    });
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

        mSubmitBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard(getActivity());
                onSubmitButtonClicked();
            }
        });
        mHideKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDescriptionView.clearFocus();
                hideKeyBoard(getActivity());
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
        validate();
    }

    private void updateLocation() {
        try {
            TextView text = (TextView) getActivity().findViewById(R.id.location_text);
            if (location == null && source != null) {
                String coordText = String.format(Locale.getDefault(), "(%f, %f)", latitude, longitude);
                Log.d(LOG_TAG, "updateLocation - Coordinates: " + coordText);
                text.setText(coordText);
            }
            if (location != null && source != null) {
                Log.d(LOG_TAG, "updateLocation - Address: " + location);
                text.setText(location);
            } else {
                text.setText(R.string.report_hint_location);
            }
            validate(); //required, so validate
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void playAudio(Uri uri) {
        try {

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getContext(), uri);
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
            Log.e(LOG_TAG, "playAudio - Exception: " + e.getMessage());
            mAudioStatus = AudioStatus.STOPPED;
        }
    }

    public void updateAudio(final Uri uri) {
        if (uri != null) {
            attachments.add(new Attachment(Attachment.AttachmentType.AUDIO, uri));
            Log.d(LOG_TAG, "updateAudio - Uri:" + uri);
            TextView filename = (TextView) getActivity().findViewById(R.id.audio_text2);
            OnClickListener playClicked = new OnClickListener() {
                public void onClick(View v) {
                    if (mAudioStatus == AudioStatus.PLAYING) {
                        playBtn.setImageResource(R.drawable.ic_play_arrow);
                        mMediaPlayer.release();
                        mPlayTime = 0;
                        mAudioStatus = AudioStatus.STOPPED;
                    } else {
                        playAudio(uri);
                    }
                }
            };
            playBtn.setOnClickListener(playClicked);
            filename.setText(Utils.niceName(getContext(), uri));
            audioviewSwitcher.setDisplayedChild(1);
        } else {
            resetAudio();
        }
    }

    public void updatePhoto(Uri uri, Boolean broadcast) {
        if (uri != null) {
            attachments.add(new Attachment(Attachment.AttachmentType.IMAGE, uri));
            Log.d(LOG_TAG, "updatePhoto - Uri:" + uri);
            if (broadcast) {
                // Tell the media gallery the photo is created
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(uri);
                getContext().sendBroadcast(mediaScanIntent);
            }
            ImageView image = (ImageView) getActivity().findViewById(R.id.photoPlaceholder);
            Log.d(LOG_TAG, "updatePhoto - imageView: " + image.toString());
            Glide.with(getContext()).load(uri).asBitmap().into(image);
            Log.d(LOG_TAG, "updatePhoto - switching displayedChild");
            photoviewSwitcher.setDisplayedChild(1);
        } else {
            resetPhoto();
        }
    }

    private void resetAudio() {
        audioviewSwitcher.setDisplayedChild(0);
        TextView audioText = (TextView) getActivity().findViewById(R.id.audio_text);
        audioText.setText(R.string.report_hint_sound);
    }

    private void resetPhoto() {
        photoviewSwitcher.setDisplayedChild(0);
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
        //Clear attachments array
        attachments = new LinkedList<Attachment>();
        resetPhoto();
        resetAudio();
        resetService();
        resetLocation();
        resetDescription();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSettings(getActivity());
        // Retain this fragment across configuration changes
        setRetainInstance(true);
        attributes = new LinkedList<Attribute>();
        attrInfoList = new LinkedList<AttributeInfo>();
        attachments = new LinkedList<Attachment>();
        installationId = ((MainActivity) getActivity()).getInstallationId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
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

            if (savedInstanceState.containsKey("attachments")) {
                Serializable state = savedInstanceState.getSerializable("attachments");
                try {
                    attachments = (LinkedList<Attachment>) state;
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
        if (attachments != null) {
            savedInstanceState.putSerializable("attachments", attachments);
        }
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
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

        if (mDescriptionView.getText().length() == 0) {
            isValid = false;
        }
        if (serviceCode == null) {
            isValid = false;
        }

        if (latitude == null || longitude == null) {
            isValid = false;
        }


        if (attrInfoList.size() != 0) {
            isValid = (attrInfoList.size() == attributes.size());

        }
        if (!isValid) {
            mSubmitBtn.setVisibility(View.INVISIBLE);
        } else {
            mSubmitBtn.setVisibility(View.VISIBLE);
        }
        return isValid;
    }

    private Boolean checkAnonymous() {
        String email = settings.getString("profile_email", null);
        String phone = settings.getString("profile_phone", null);
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
                Log.d(LOG_TAG, "addAttributesToForm - Attribute info: " + attr.getDatatype());
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
        ServicesAdapter servicesAdapter = new ServicesAdapter(getActivity(), services);
        builder.setTitle(R.string.report_hint_service)
                .setCancelable(false)
                .setAdapter(servicesAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int index) {
                                serviceName = values[index];
                                serviceCode = codes[index];
                                Log.d(LOG_TAG, "onServiceButtonClicked - Selected: " + serviceName);
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
                        handleGallery(Attachment.AttachmentType.AUDIO);
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
                        handleGallery(Attachment.AttachmentType.IMAGE);
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
        Intent intent = new Intent(getActivity(), MapActivity.class);
        startActivityForResult(intent, LOCATION_REQUEST);
    }

    /**
     * User selected to use the Gallery as picture source
     * Because we check permissions for Android 6+, this function is also used to propagate
     * actions from the checker
     */
    private void handleGallery(Attachment.AttachmentType type) {
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
        intent.putExtra("GalleryType", type.toInt());
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
            File audio = createFile(Attachment.AttachmentType.AUDIO);
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

            try {
                photo = createFile(Attachment.AttachmentType.IMAGE);
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
        Log.d(LOG_TAG, "onSubmitButtonClicked");
        final String pattern = "[^\\w\\s\\.\\?\\(\\),!:;@]";
        // Check the form result and post the service request
        if (!validate()) {
            String result = getString(R.string.failure_posting_service);
            Snackbar.make(getView(), result, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }


        final TextView address = (TextView) getActivity().findViewById(R.id.location_text);

        final POSTServiceRequestDataWrapper data = new POSTServiceRequestDataWrapper(
                serviceCode,
                latitude,
                longitude,
                attributes);

        if (!checkAnonymous()) {
            String name = settings.getString("profile_name", null);
            String email = settings.getString("profile_email", null);
            String phone = settings.getString("profile_phone", null);

            if (name != null) data.setName(name);
            if (email != null) data.setEmail(email);
            if (phone != null) data.setPhone(phone);

            data.setDeviceId(installationId)
                    .setAddress(address.getText().toString())
                    .setDescription(
                            Normalizer.normalize(
                                    mDescriptionView.getText().toString(), Normalizer.Form.NFD)
                                    .replaceAll(pattern, ""));
            PostServiceRequestTask bgTask = new PostServiceRequestTask(data);
            bgTask.execute();
        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
            builder.setTitle(getString(R.string.post_anonymous))
                    .setMessage(getString(R.string.post_anonymous_description))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
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
                                    .setDescription(Normalizer.normalize(
                                            mDescriptionView.getText().toString(), Normalizer.Form.NFD)
                                            .replaceAll(pattern, ""));
                            PostServiceRequestTask bgTask = new PostServiceRequestTask(data);
                            bgTask.execute();
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
                updateAudio(data.getData());
            } else {
                resetAudio();
            }
        }
        if (requestCode == RECORDER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                updateAudio(data.getData());
            } else {
                resetAudio();
            }
        }
        if (requestCode == GALLERY_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                updatePhoto(data.getData(), false);
            } else {
                resetPhoto();
            }
        }

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.getData() == null) return;
                    updatePhoto(data.getData(), true);
                } else {
                    if (photo.length() == 0) return;
                    updatePhoto(Uri.fromFile(photo), true);
                }
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
        private boolean success = true;

        PostServiceRequestTask(POSTServiceRequestData data) {
            this.data = data;
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
                if (attachments.size() > 0) {
                    final OkHttpClient client = new OkHttpClient();


                    MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM);
                    //each
                    for (Attachment att : attachments) {
                        Uri uri = att.getUri();
                        String name = Utils.niceName(getContext(), uri);
                        if (att.getType() == Attachment.AttachmentType.AUDIO) {
                            name = name + ".3gp";
                        }
                        MediaType mediaType = Utils.getMediaType(getContext(), uri);
                        InputStream inputstream = getContext().getContentResolver().openInputStream(uri);
                        byte[] inputData = Utils.getBytes(inputstream);
                        requestBodyBuilder.addFormDataPart("att", name, RequestBody.create(mediaType, inputData));
                    }
                    RequestBody requestBody = requestBodyBuilder.build();
                    Request request = new Request.Builder()
                            .url("https://www.open311.io/api/upload")
                            .post(requestBody)
                            .build();

                    Response response1 = client.newCall(request).execute();
                    if (!response1.isSuccessful())
                        throw new IOException("Unexpected code " + response1);

                    final Gson gson = new Gson();
                    String responseBody = response1.body().string();
                    try {
                        Type uploadResultsType = new TypeToken<ArrayList<UploadResult>>() {
                        }.getType();

                        List<UploadResult> uploadResult = gson.fromJson(responseBody, uploadResultsType);
                        List<String> list = new ArrayList<String>();
                        Boolean mediaUrlSet = false;
                        for (UploadResult temp : uploadResult) {
                            if (!mediaUrlSet) {
                                // Get the item and parse it into the media_url for legacy compatibility
                                if (temp.getPath().startsWith("image")) {
                                    data.setMediaUrl(temp.getPath());
                                    mediaUrlSet = true;
                                }

                            }
                            list.add(temp.getPath());
                        }
                        String[] stringArray = list.toArray(new String[0]);

                        data.setMedia(stringArray);
                    } catch (Exception e) {
                        UploadResult uploadResult = gson.fromJson(responseBody, UploadResult.class);
                        List<String> list = new ArrayList<String>();
                        list.add(uploadResult.getPath());
                        String[] stringArray = list.toArray(new String[0]);
                        data.setMedia(stringArray);
                        // Add it as media url too for legacy compatibility
                        data.setMediaUrl(uploadResult.getPath());
                        e.printStackTrace();
                    }
                }
                APIWrapperFactory wrapperFactory = new APIWrapperFactory(((MainActivity) getActivity()).getCurrentServer(), EndpointType.PRODUCTION);
                wrapperFactory.setCache(AndroidCache.getInstance(getActivity().getApplicationContext()));
                wrapperFactory.setApiKey(((MainActivity) getActivity()).getCurrentServer().getApiKey());

                APIWrapper wrapper = wrapperFactory.build();
                PackageManager manager = getActivity().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
                wrapper.setHeader("User-Agent", "open311-android/" + info.versionName);
                wrapper.setHeader("open311-deviceid", ((MainActivity) getActivity()).getInstallationId());
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return e.getMessage();
            }
            return result;
        }

        private boolean saveServiceRequestId(String id) {
            MyReportsFile file = new MyReportsFile(getContext());
            MainActivity mActivity = (MainActivity) getActivity();
            Server mServer = mActivity.getCurrentServer();
            updateReports(mActivity, mServer.getName(), id);
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
            Log.d(LOG_TAG, "PostServiceRequestTask onPostExecute - Post result: " + result);

            if (success) resetAll();

            MyReportsFile file = new MyReportsFile(getContext());
            int reqs = file.getServiceRequestLength();
            Log.d(LOG_TAG, "PostServiceRequestTask onPostExecute - Requests for user: " + reqs);

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

                wrapper = new APIWrapperFactory(((MainActivity) getActivity()).getCurrentServer(), EndpointType.PRODUCTION)
                        .setCache(AndroidCache.getInstance(getActivity().getApplicationContext()))
                        .build();
                PackageManager manager = getActivity().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
                wrapper.setHeader("User-Agent", "open311-android/" + info.versionName);
                wrapper.setHeader("open311-deviceid", ((MainActivity) getActivity()).getInstallationId());
                definition = wrapper.getServiceDefinition(this.serviceCode);
                for (AttributeInfo o : definition.getAttributes()) {
                    attrInfoList.add(o);
                }
                count = attrInfoList.size();
                Log.d(LOG_TAG, "RetrieveAttributesTask doInBackground - Attribute count: " + count);

            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return count;
        }

        protected void onPostExecute(Integer count) {
            if (count == 0) {
                Log.d(LOG_TAG, "RetrieveAttributesTask onPostExecute - Service has no attributes");
            }
        }
    }

    private class RetrieveServicesTask extends AsyncTask<String, Void, List<Service>> {

        ProgressDialog progressDialog;

        @Override
        protected List<Service> doInBackground(String... params) {
            Log.d(LOG_TAG, "RetrieveServicesTask - doInBackground");
            services = null; //reset services
            APIWrapper wrapper;
            EndpointType endpointType;
            Server currentServer = ((MainActivity) getActivity()).getCurrentServer();
            // TODO Check if server has a base URL, then check if it has a test Url. determine the EndpointType by that.
            endpointType = EndpointType.PRODUCTION;

            try {
                wrapper = new APIWrapperFactory(currentServer, endpointType)
                        .setCache(AndroidCache.getInstance(getActivity().getApplicationContext()))
                        .build();
                PackageManager manager = getActivity().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
                wrapper.setHeader("User-Agent", "open311-android/" + info.versionName);
                wrapper.setHeader("open311-deviceid", ((MainActivity) getActivity()).getInstallationId());
                publishProgress();
                return wrapper.getServiceList();
            } catch (APIWrapperException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Service> result) {
            progressDialog.cancel();
            progressDialog.setCancelable(true);
            progressDialog.setMessage("All done!");
            Log.d(LOG_TAG, "RetrieveServicesTask onPostExecute - Result: " + result);
            if (result != null) {
                services = result;
                resetAll();
            } else {
                Log.w(LOG_TAG, "RetrieveServicesTask onPostExecute - Could not download services!");
            }
        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(
                    getActivity());

            progressDialog.setMessage(getString(R.string.contactingServer) + " " + ((MainActivity) getActivity()).getCurrentServer().getName());
            getActivity().setTitle(getString(R.string.app_name) + " " + ((MainActivity) getActivity()).getCurrentServer().getTitle());
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (services == null) {
                progressDialog.setMessage(getString(R.string.connectionEstablished));
            } else {
                if (services.size() > 0) {
                    progressDialog.setMessage(services.size() + " " + getString(R.string.servicesDownloaded));
                }
            }
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
            handleGallery(Attachment.AttachmentType.AUDIO);
        }
        if (requestCode == GALLERY_IMAGE_REQUEST
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handleGallery(Attachment.AttachmentType.IMAGE);
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

    private File createFile(Attachment.AttachmentType type) throws IOException {
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

        return File.createTempFile(
                fileName,
                extension,
                storageDir
        );
    }

}
