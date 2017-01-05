package org.open311.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SoundRecorderActivity extends Activity {

    private static final String LOG_TAG = "SoundRecorderFragment";
    private static final String FILE_DESCRIPTOR_MODE = "rwt";

    private static final int MAX_DURATION_MS = 3 * 60 * 1000; // 3 minutes

    Uri mOutputUri;
    FileDescriptor mOutputFileDescriptor;

    // Views for recording
    ProgressBar mRecordAudioProgressBar;
    // Views for playback
    Chronometer mAudioSeekBar;

    // Views for both
    FloatingActionButton mRecordAudioControlButton;
    FloatingActionButton mRecordSubmitButton;
    TextView mRecordingStatus;
    Timer mProgressTimer;

    // Recorder stuff
    MediaRecorder mMediaRecorder;
    RecordingState mCurrentRecordingState;

    // Stuff for both
    View.OnClickListener mOnClickListener;

    enum RecordingState {
        NOT_STARTED,
        STARTED,
        STOPPED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sound_recorder);

        Intent recordIntent = getIntent();

        Uri outputUri;

        if (recordIntent == null) {
            Log.e(LOG_TAG, "Intent is null");
            finish();
            return;
        }

        outputUri = recordIntent.getData();

        if (outputUri == null) {
            Log.e(LOG_TAG, "Output URI is null");
            finish();
            return;
        } else {
            Log.e(LOG_TAG, "Output URI is " + outputUri);
        }

        mOutputUri = outputUri;

        FileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = getContentResolver().openFileDescriptor(mOutputUri, FILE_DESCRIPTOR_MODE).getFileDescriptor();
        } catch (FileNotFoundException exception) {
            Log.e(LOG_TAG, "Output URI is an invalid file", exception);
        }

        if (fileDescriptor == null || !fileDescriptor.valid()) {
            finish();
            return;
        }

        mOutputFileDescriptor = fileDescriptor;

        setup();

        setFinishOnTouchOutside(false);
    }

    private void setup() {
        setupViews();
        setupRecorder();
    }

    private void setupViews() {
        Log.d(LOG_TAG, "setupViews");

        mRecordAudioProgressBar = (ProgressBar) findViewById(R.id.record_audio_progress);
        mRecordAudioProgressBar.setMax(MAX_DURATION_MS);
        mAudioSeekBar = (Chronometer) findViewById(R.id.record_audio_seekbar);
        mRecordAudioControlButton = (FloatingActionButton) findViewById(R.id.record_audio_control_button);
        mRecordSubmitButton = (FloatingActionButton) findViewById(R.id.record_submit);
        mRecordingStatus = (TextView) findViewById((R.id.recording_status_text));

        mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.record_audio_control_button:
                        switch (mCurrentRecordingState) {
                            case NOT_STARTED:
                                startRecording();
                                break;
                            case STARTED:
                                tearDownRecording(false);
                                break;
                            case STOPPED:
                                setupRecorder();
                                startRecording();
                                break;
                            default:
                                completeRecording();
                                break;
                        }
                        break;
                }
            }
        };
        mRecordSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeRecording();
            }
        });

        mRecordingStatus.setText(getString(R.string.record_start));
        mRecordAudioControlButton.setOnClickListener(mOnClickListener);

        mAudioSeekBar.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long t = SystemClock.elapsedRealtime() - chronometer.getBase();

                mRecordAudioProgressBar.setProgress((int) t);
                chronometer.setText(DateFormat.format("mm:ss", t));
            }
        });

    }

    private void setupRecorder() {
        Log.d(LOG_TAG, "setupRecorder");
        // Credit to wernerd :-)
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setAudioEncodingBitRate(16);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setMaxDuration(MAX_DURATION_MS);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile(mOutputFileDescriptor);
        mAudioSeekBar.setBase(SystemClock.elapsedRealtime());
        try {
            mMediaRecorder.prepare();
            mCurrentRecordingState = RecordingState.NOT_STARTED;
        } catch (IOException exception) {
            Log.e(LOG_TAG, "Recording preparation failed", exception);
            finish();
        }
    }

    private void startRecording() {
        try {
            mMediaRecorder.start();
            mCurrentRecordingState = RecordingState.STARTED;
            mRecordSubmitButton.setVisibility(View.INVISIBLE);
            mRecordAudioControlButton.setImageDrawable(ContextCompat.getDrawable(SoundRecorderActivity.this.getApplication().getBaseContext(), R.drawable.ic_stop));
            mAudioSeekBar.start();
            mRecordingStatus.setText(getString(R.string.record_in_progress));
            mRecordAudioProgressBar.setVisibility(View.VISIBLE);
        } catch (IllegalStateException exception) {
            Log.e(LOG_TAG, "Bad state when starting recording", exception);
        }
    }

    private void stopRecording() {
        try {
            mMediaRecorder.stop();
            mAudioSeekBar.stop();

        } catch (IllegalStateException exception) {
            Log.e(LOG_TAG, "Bad state when stopping recording (ignoring)", exception);
        } catch (RuntimeException exception) {
            Log.e(LOG_TAG, "Exception when stopping recording (ignoring)", exception);
        }

        Log.i(LOG_TAG, "Recording stopped");
        mCurrentRecordingState = RecordingState.STOPPED;
        mRecordAudioControlButton.setImageDrawable(ContextCompat.getDrawable(SoundRecorderActivity.this.getApplication().getBaseContext(), R.drawable.ic_mic_white));
        mRecordSubmitButton.setVisibility(View.VISIBLE);
    }


    private void completeRecording() {
        Log.d(LOG_TAG, "completeRecording");
        tearDownRecording(true);
        Intent returnIntent = new Intent();
        returnIntent.setData(mOutputUri);
        setResult(RESULT_OK, returnIntent);

        finish();
    }

    private void tearDownRecording(boolean fromCancellation) {
        Log.i(LOG_TAG, "tearDownRecording");

        if (mCurrentRecordingState == RecordingState.STARTED) {
            stopRecording();
        }

        if (mMediaRecorder != null) {

            mMediaRecorder.release();
            //Now the file is there!

            mMediaRecorder = null;

        }

        if (!fromCancellation) {
            mRecordingStatus.setText(R.string.record_ready);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        tearDownRecording(true);
        finish();
    }
}
