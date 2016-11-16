package org.open311.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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

    LinearLayout mSoundRecorderLayout;

    // Views for recording
    ProgressBar mRecordAudioProgressBar;
    ImageButton mPlaybackPlayPauseButton;

    // Views for playback
    SeekBar mAudioSeekBar;
    Button mRecordAudioCancelButton;
    ImageButton mRecordAudioPlayButton;

    // Views for both
    Button mRecordAudioControlButton;
    TextView mCurrentRecordTimeCurrentTextView;
    TextView mCurrentRecordTimeMaxTextView;
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
        mSoundRecorderLayout = (LinearLayout) findViewById(R.id.sound_recorder_layout);

        mRecordAudioProgressBar = (ProgressBar) findViewById(R.id.record_audio_progress);
        mAudioSeekBar = (SeekBar) findViewById(R.id.audio_seekbar);

        mRecordAudioControlButton = (Button) findViewById(R.id.record_audio_control_button);
        mRecordAudioCancelButton = (Button) findViewById(R.id.record_audio_cancel_button);

        mCurrentRecordTimeCurrentTextView = (TextView) findViewById(R.id.record_audio_time_current);
        mCurrentRecordTimeMaxTextView = (TextView) findViewById(R.id.record_audio_time_max);

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
                                completeRecording();
                                break;
                        }
                        break;

                    case R.id.record_audio_cancel_button:
                        onCancel();
                        break;
                }
            }
        };

        mRecordAudioControlButton.setOnClickListener(mOnClickListener);
        mRecordAudioCancelButton.setOnClickListener(mOnClickListener);

        mAudioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentRecordTimeCurrentTextView.setText(getTimeString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        mRecordAudioProgressBar.setMax(MAX_DURATION_MS);
    }

    private void setCurrentRecordingState(RecordingState recordingState) {
        Log.i(LOG_TAG, "setCurrentRecordingState: " + recordingState);

        mCurrentRecordingState = recordingState;
    }

    private void setupProgressTimer() {
        mProgressTimer = new Timer();
    }

    private void startProgressTimer() {
        if (mProgressTimer == null) {
            setupProgressTimer();
        }

        mProgressTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int nextMilliSeconds;

                        if (mCurrentRecordingState == RecordingState.STARTED) {
                            if (mRecordAudioProgressBar.getProgress() < MAX_DURATION_MS) {
                                nextMilliSeconds = mRecordAudioProgressBar.getProgress() + getProgressIncrement(mRecordAudioProgressBar.getProgress());

                                Log.i(LOG_TAG, "Recording progress " + nextMilliSeconds);

                                mRecordAudioProgressBar.setProgress(nextMilliSeconds);
                                mCurrentRecordTimeCurrentTextView.setText(getTimeString(nextMilliSeconds));
                            } else {
                                //Reached maximum duration. Stopping.
                                tearDownRecording(false);
                            }
                        } else if (mCurrentRecordingState == RecordingState.STOPPED) {
                            if (mAudioSeekBar.getProgress() < MAX_DURATION_MS) {
                                nextMilliSeconds = mAudioSeekBar.getProgress() + getProgressIncrement(mAudioSeekBar.getProgress());

                                Log.i(LOG_TAG, "Recording progress " + nextMilliSeconds);

                                mAudioSeekBar.setProgress(nextMilliSeconds);
                                mCurrentRecordTimeCurrentTextView.setText(getTimeString(nextMilliSeconds));
                            }
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopProgressTimer() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
            mProgressTimer.purge();
        }

        mProgressTimer = null;
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


        try {
            mMediaRecorder.prepare();

            onSetupRecorder();
        } catch (IOException exception) {
            Log.e(LOG_TAG, "Recording preparation failed", exception);
            finish();
        }
    }

    private void onSetupRecorder() {
        setCurrentRecordingState(RecordingState.NOT_STARTED);
    }

    private void startRecording() {
        try {
            mMediaRecorder.start();

            onStartRecording();
        } catch (IllegalStateException exception) {
            Log.e(LOG_TAG, "Bad state when starting recording", exception);
        }
    }

    private void onStartRecording() {
        Log.i(LOG_TAG, "Recording started");

        setCurrentRecordingState(RecordingState.STARTED);

        mRecordAudioControlButton.setText(R.string.stop_dialog);
        mRecordAudioProgressBar.setVisibility(View.VISIBLE);
        startProgressTimer();
    }

    private void stopRecording() {
        try {
            mMediaRecorder.stop();

        } catch (IllegalStateException exception) {
            Log.e(LOG_TAG, "Bad state when stopping recording (ignoring)", exception);
        } catch (RuntimeException exception) {
            Log.e(LOG_TAG, "Exception when stopping recording (ignoring)", exception);
        }

        onStopRecording();
    }

    private void onStopRecording() {
        Log.i(LOG_TAG, "Recording stopped");

        setCurrentRecordingState(RecordingState.STOPPED);

        stopProgressTimer();
    }

    private void completeRecording() {
        Log.d(LOG_TAG, "completeRecording");
        tearDownRecording(true);
        onCompleteRecording();
    }

    private void onCompleteRecording() {
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
            mRecordAudioControlButton.setText(R.string.okay);
            mRecordAudioProgressBar.setVisibility(View.INVISIBLE);
            mAudioSeekBar.setVisibility(View.VISIBLE);
            mAudioSeekBar.setProgress(0);
        }
    }

    private void onCancel() {
        tearDownRecording(true);
        finish();
    }

    private String getTimeString(int miliSeconds) {
        int minutes = (int) Math.floor((miliSeconds / 1000.0) / 60);
        int seconds = miliSeconds / 1000 - minutes * 60;

        return String.format("%01d:%02d", minutes, seconds);
    }

    private int getNumIncrements(int durationMiliSeconds) {
        int numIncrements = (int) Math.ceil(durationMiliSeconds / 1000.0);

        if (numIncrements == 0) {
            return 1;
        }

        return numIncrements;
    }

    private int getMaxDuration(int durationMiliSeconds) {
        int numIncrements = getNumIncrements(durationMiliSeconds) * 1000;

        if (numIncrements > MAX_DURATION_MS) {
            numIncrements = MAX_DURATION_MS;
        }

        return numIncrements;
    }

    private int getProgressIncrement(int durationMiliSeconds) {
        int numIncrements = getNumIncrements(durationMiliSeconds);

        if (numIncrements == 1) {
            return 1000;
        }

        return Math.round(durationMiliSeconds / numIncrements);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancel();
    }
}
