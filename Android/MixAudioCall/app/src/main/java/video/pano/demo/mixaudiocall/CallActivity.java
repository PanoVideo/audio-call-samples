package video.pano.demo.mixaudiocall;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.pano.rtc.api.Constants;
import com.pano.rtc.api.RtcAudioMixingConfig;
import com.pano.rtc.api.RtcChannelConfig;
import com.pano.rtc.api.RtcEngine;
import com.pano.rtc.api.RtcMediaStatsObserver;
import com.pano.rtc.api.model.stats.RtcAudioRecvStats;
import com.pano.rtc.api.model.stats.RtcAudioSendStats;
import com.pano.rtc.api.model.stats.RtcSystemStats;
import com.pano.rtc.api.model.stats.RtcVideoBweStats;
import com.pano.rtc.api.model.stats.RtcVideoRecvStats;
import com.pano.rtc.api.model.stats.RtcVideoSendStats;

import java.util.Locale;


public class CallActivity extends AppCompatActivity implements PanoEventHandler,
        RtcMediaStatsObserver {
    private static final String TAG = "AudioCall";
    private static final int CHOOSE_FILE_CODE = 217;

    private String mToken = "";
    private String mChannelId = "";
    private long mUserId = 0;
    private boolean mMode1v1 = false;
    private Constants.AudioAecType mAudioAecType = Constants.AudioAecType.Default;

    private RtcEngine mRtcEngine;
    private boolean mIsChannelJoined = false;

    private boolean mIsMixingStarted = false;
    private int mMixingTaskId = 42;
    private Dialog mMixingDialog;
    private String mMixingFile = "";

    private Switch mLoudspeaker;
    private Switch mMuteAudio;
    private TextView mTextLog;

    private String mMessage = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        configFromIntent(getIntent());

        PanoApplication app = (PanoApplication)getApplication();
        mRtcEngine = app.getPanoEngine();
        app.registerEventHandler(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = preferences.getBoolean("loudspeaker", true);
        mLoudspeaker = findViewById(R.id.switchLoudspeaker);
        mLoudspeaker.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("loudspeaker", mLoudspeaker.isChecked());
            editor.apply();
            mRtcEngine.setLoudspeakerStatus(mLoudspeaker.isChecked());
        });
        mLoudspeaker.setChecked(enabled);

        mMuteAudio = findViewById(R.id.switchMuteAudio);
        mMuteAudio.setOnClickListener(v -> {
            if (mMuteAudio.isChecked()) {
                mRtcEngine.muteAudio();
            } else {
                mRtcEngine.unmuteAudio();
            }
        });
        mMuteAudio.setChecked(false);

        findViewById(R.id.buttonLeaveChannel).setOnClickListener( v1 -> {
            onBackPressed();
        });
        findViewById(R.id.btnAudioMixing).setOnClickListener( v1 -> {
            audioMixing();
        });

        mTextLog = findViewById(R.id.textViewLog);

        mRtcEngine.setMediaStatsObserver(this);
        mRtcEngine.setLoudspeakerStatus(mLoudspeaker.isChecked());

        mMessage = "";
        joinChannel();
    }

    @Override
    public void onBackPressed() {
        leaveChannel();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        leaveChannel();
        PanoApplication app = (PanoApplication)getApplication();
        app.removeEventHandler(this);
        mRtcEngine = null;
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CHOOSE_FILE_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            mMixingFile = FileUtils.getFileFromUri(this, uri);
            if (mMixingDialog != null) {
                String filename;
                int idx = mMixingFile.lastIndexOf('/');
                if (idx >= 0) {
                    filename = mMixingFile.substring(idx+1);
                } else {
                    filename = mMixingFile;
                }
                EditText audioFile = mMixingDialog.findViewById(R.id.edit_audio_file);
                audioFile.setText(filename);
            }
        }
    }


    void configFromIntent(Intent intent) {
        mUserId = intent.getLongExtra("userId", 0);
        mChannelId = intent.getStringExtra("channelId");
        mToken = intent.getStringExtra("token");
        mMode1v1 = intent.getBooleanExtra("mode1v1", true);
    }

    private void joinChannel() {
        RtcChannelConfig config = new RtcChannelConfig();
        config.userName = "Android_" + mUserId;
        config.mode_1v1 = mMode1v1;
        // enable media service only
        config.serviceFlags = Constants.kChannelServiceMedia;
        config.subscribeAudioAll = true;
        mRtcEngine.joinChannel(mToken, mChannelId, mUserId, config);
    }

    private void leaveChannel() {
        if (mIsMixingStarted) {
            mRtcEngine.getAudioMixingMgr().stopAudioMixingTask(mMixingTaskId);
            mRtcEngine.getAudioMixingMgr().destroyAudioMixingTask(mMixingTaskId);
        }
        if (mIsChannelJoined) {
            mRtcEngine.leaveChannel();
        }
    }

    RtcAudioMixingConfig mMixingConfig = new RtcAudioMixingConfig();
    private void audioMixing() {
        if (mMixingDialog == null) {
            mMixingDialog = new Dialog(this, R.style.CustomDialogStyle);

            mMixingDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
            //dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.your_icon);
            mMixingDialog.setContentView(R.layout.dialog_mixing);

            Switch enablePublish = mMixingDialog.findViewById(R.id.switch_enable_publish);
            enablePublish.setChecked(mMixingConfig.enablePublish);
            enablePublish.setOnClickListener(view -> {
                mMixingConfig.enablePublish = enablePublish.isChecked();
            });
            EditText editPubVol = mMixingDialog.findViewById(R.id.edit_publish_volume);
            editPubVol.setText(String.valueOf(mMixingConfig.publishVolume));
            editPubVol.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    if(s.length() != 0) {
                        mMixingConfig.publishVolume = Integer.parseInt(s.toString());
                    } else {
                        mMixingConfig.publishVolume = 0;
                    }
                }
            });
            Switch enableLoopback = mMixingDialog.findViewById(R.id.switch_enable_loopback);
            enableLoopback.setChecked(mMixingConfig.enableLoopback);
            enableLoopback.setOnClickListener(view -> {
                mMixingConfig.enableLoopback = enableLoopback.isChecked();
            });
            EditText editLoopVol = mMixingDialog.findViewById(R.id.edit_loopback_volume);
            editLoopVol.setText(String.valueOf(mMixingConfig.loopbackVolume));
            editLoopVol.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    if(s.length() != 0) {
                        mMixingConfig.loopbackVolume = Integer.parseInt(s.toString());
                    } else {
                        mMixingConfig.loopbackVolume = 0;
                    }
                }
            });
            EditText editCycle = mMixingDialog.findViewById(R.id.edit_mixing_cycle);
            editCycle.setText(String.valueOf(mMixingConfig.cycle));
            editCycle.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    if(s.length() != 0) {
                        mMixingConfig.cycle = Integer.parseInt(s.toString());
                    } else {
                        mMixingConfig.cycle = 0;
                    }
                }
            });
            Switch replaceMic = mMixingDialog.findViewById(R.id.switch_replace_mic);
            replaceMic.setChecked(mMixingConfig.replaceMicrophone);
            replaceMic.setOnClickListener(view -> {
                mMixingConfig.replaceMicrophone = replaceMic.isChecked();
            });

            mMixingDialog.findViewById(R.id.btn_pick_file).setOnClickListener(v -> {
                pickAudioFile();
            });
            mMixingDialog.findViewById(R.id.btn_start_mixing).setOnClickListener(v -> {
                mMixingDialog.dismiss();
                Button btn = (Button)v;
                if (mIsMixingStarted) {
                    stopAudioMixing();
                    btn.setText("Start Mixing");
                } else {
                    btn.setText("Stop Mixing");
                    startAudioMixing(mMixingFile);
                }
            });
            mMixingDialog.findViewById(R.id.btn_update_config).setOnClickListener(v -> {
                updateMixingConfig();
            });
            //dialog.setCancelable(true);
            mMixingDialog.setCanceledOnTouchOutside(true);
        }
        mMixingDialog.show();
    }

    private void pickAudioFile() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");
        intent = Intent.createChooser(chooseFile, "Choose an audio file");
        try {
            this.startActivityForResult(intent, CHOOSE_FILE_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "found no file manager");
        }
    }

    private void startAudioMixing(String filename) {
        if (mMixingFile == null || mMixingFile.isEmpty()) {
            appendMessage("must select an audio file");
            return;
        }
        Log.i(TAG, "pick file name: " + filename);
        appendMessage("audio file: " + mMixingFile);
        mRtcEngine.getAudioMixingMgr().createAudioMixingTask(mMixingTaskId, filename);
        Constants.QResult ret = mRtcEngine.getAudioMixingMgr().startAudioMixingTask(mMixingTaskId, mMixingConfig);
        if (ret != Constants.QResult.OK) {
            mIsMixingStarted = false;
            mRtcEngine.getAudioMixingMgr().destroyAudioMixingTask(mMixingTaskId);
        } else {
            mIsMixingStarted = true;
        }
        appendMessage("audio mixing, result: " + ret);
    }

    private void stopAudioMixing() {
        mRtcEngine.getAudioMixingMgr().stopAudioMixingTask(mMixingTaskId);
        mRtcEngine.getAudioMixingMgr().destroyAudioMixingTask(mMixingTaskId);
        mIsMixingStarted = false;
    }
    private void updateMixingConfig() {
        if (mIsMixingStarted) {
            mRtcEngine.getAudioMixingMgr().updateAudioMixingTask(mMixingTaskId, mMixingConfig);
        }
    }

    private void appendMessage(String str) {
        mMessage = str + "\n" + mMessage;
        mTextLog.setText(mMessage);
    }


    // -------------------------- RTC Engine Callbacks --------------------------
    public void onChannelJoinConfirm(Constants.QResult result) {
        Log.i(TAG, "onChannelJoinConfirm, result="+result);
        if (result == Constants.QResult.OK) {
            mIsChannelJoined = true;
            Toast.makeText(CallActivity.this, "onChannelJoinConfirm success", Toast.LENGTH_LONG).show();
            appendMessage("join channel success");
            mRtcEngine.startAudio();
        } else {
            mIsChannelJoined = false;
            appendMessage("join channel failed, result=" + result );
            Toast.makeText(CallActivity.this, "onChannelJoinConfirm result=" + result, Toast.LENGTH_LONG).show();
        }
    }

    public void onChannelLeaveIndication(Constants.QResult result) {
        Log.i(TAG, "onChannelLeaveIndication, result="+result);
        mIsChannelJoined = false;
        appendMessage("channel left, result=" + result);
        Toast.makeText(CallActivity.this, "onChannelLeaveIndication result=" + result, Toast.LENGTH_LONG).show();
        finish();
    }
    public void onChannelCountDown(long remain) {
        Log.i(TAG, "onChannelCountDown, remain="+remain);
    }
    public void onUserJoinIndication(long userId, String userName) {
        Log.i(TAG, "onUserJoinIndication, userId="+userId+", userName="+userName);
        appendMessage("user joined, userId=" + userId + ", userName=" + userName);
        Toast.makeText(CallActivity.this, "onUserJoinIndication userId=" + userId, Toast.LENGTH_LONG).show();
    }
    public void onUserLeaveIndication(long userId, Constants.UserLeaveReason reason) {
        Log.i(TAG, "onUserLeaveIndication, userId="+userId);
        appendMessage("user left, userId=" + userId + ", reason=" + reason);
        Toast.makeText(CallActivity.this, "onUserLeaveIndication userId=" + userId + ", reason=" + reason, Toast.LENGTH_LONG).show();
    }
    public void onUserAudioStart(long userId) {
        Log.i(TAG, "onUserAudioStart, userId="+userId);
        appendMessage("user audio is started, userId=" + userId);
    }
    public void onUserAudioStop(long userId) {
        Log.i(TAG, "onUserAudioStop, userId="+userId);
        appendMessage("user audio is stopped, userId=" + userId);
    }
    public void onUserAudioSubscribe(long userId, Constants.MediaSubscribeResult result) {
        Log.i(TAG, "onUserAudioSubscribe, userId="+userId + ", result=" + result);
    }
    public void onUserAudioMute(long userId) {
        appendMessage("user audio is muted, userId=" + userId);
    }
    public void onUserAudioUnmute(long userId) {
        appendMessage("user audio is unmuted, userId=" + userId);
    }
    @Override
    public void onFirstAudioDataReceived(long userId) {
        Log.i(TAG, "+++++ onFirstAudioDataReceived: userId="+userId);
        appendMessage("first audio data is received, userId=" + userId);
    }
    @Override
    public void onAudioDeviceStateChanged(String deviceId,
                                          Constants.AudioDeviceType deviceType,
                                          Constants.AudioDeviceState deviceState) {
        Log.i(TAG, "+++++ onAudioDeviceStateChanged: "+deviceId);
        appendMessage("audio device state is changed, deviceId=" + deviceId);
    }
    @Override
    public void onChannelFailover(Constants.FailoverState state) {
        Log.i(TAG, "+++++ onChannelFailover: state="+state.getValue());
        appendMessage("channel failover state: " + state);
    }


    // -------------------------- RTC Media Stats Callbacks --------------------------
    @Override
    public void onVideoSendStats(RtcVideoSendStats stats) {}

    @Override
    public void onVideoRecvStats(RtcVideoRecvStats stats) {}

    @Override
    public void onAudioSendStats(RtcAudioSendStats stats) {
        runOnUiThread(()-> {
            /*if (mAtxStats != null) {
                String str = statsToString(stats);
                mAtxStats.setText(str);
            }*/
        });
    }

    @Override
    public void onAudioRecvStats(RtcAudioRecvStats stats) {
        runOnUiThread(()-> {
            /*if (mArxStats != null) {
                String str = statsToString(stats);
                mArxStats.setText(str);
            }*/
        });
    }

    @Override
    public void onScreenSendStats(RtcVideoSendStats stats) {}

    @Override
    public void onScreenRecvStats(RtcVideoRecvStats stats) {}

    @Override
    public void onVideoBweStats(RtcVideoBweStats stats) {}

    @Override
    public void onSystemStats(RtcSystemStats stats) {}


    private String statsToString(RtcAudioSendStats stats) {
        return String.format(Locale.CHINA,
                "===== audio tx =====\nbytes:\t\t\t%d\nbr:\t\t\t\t\t%d"
                        + "\nlosscnt:\t%d\nloss:\t\t\t\t%.3f"
                        + "\nrtt:\t\t\t\t\t%d\nlevel:\t\t\t%d",
                stats.bytesSent, stats.bitrate, stats.packetsLost,
                stats.lossRatio,stats.rtt, stats.inputLevel);
    }

    private String statsToString(RtcAudioRecvStats stats) {
        return String.format(Locale.CHINA,
                "===== audio rx =====\nbytes:\t\t\t%d\nbr:\t\t\t\t\t%d"
                        + "\nlosscnt:\t%d\nloss:\t\t\t\t%.3f"
                        + "\nlevel:\t\t\t%d",
                stats.bytesReceived, stats.bitrate, stats.packetsLost,
                stats.lossRatio,stats.outputLevel);
    }
}
