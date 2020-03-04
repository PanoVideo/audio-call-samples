package com.pano.rtc.demo.basicaudiocall;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.pano.rtc.api.Constants;
import com.pano.rtc.api.RtcChannelConfig;
import com.pano.rtc.api.RtcEngine;
import com.pano.rtc.api.RtcEngineCallback;
import com.pano.rtc.api.RtcEngineConfig;
import com.pano.rtc.api.RtcMediaStatsObserver;
import com.pano.rtc.api.model.stats.RtcAudioRecvStats;
import com.pano.rtc.api.model.stats.RtcAudioSendStats;
import com.pano.rtc.api.model.stats.RtcSystemStats;
import com.pano.rtc.api.model.stats.RtcVideoBweStats;
import com.pano.rtc.api.model.stats.RtcVideoRecvStats;
import com.pano.rtc.api.model.stats.RtcVideoSendStats;

import java.util.Locale;


public class CallActivity extends AppCompatActivity implements RtcEngineCallback,
        RtcMediaStatsObserver {
    private static final String TAG = "AudioCall";

    private String mPanoServer = "api.pano.video";
    private String mToken = "";
    private String mChannelId = "";
    private long mUserId = 0;
    private boolean mMode1v1 = false;
    private Constants.AudioAecType mAudioAecType = Constants.AudioAecType.Default;

    private RtcEngine mRtcEngine = null;
    private boolean mIsChannelJoined = false;

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

        mTextLog = findViewById(R.id.textViewLog);


        RtcEngineConfig engineConfig = new RtcEngineConfig();
        engineConfig.appId = MainActivity.APPID;
        engineConfig.server = mPanoServer;
        engineConfig.context = getApplicationContext();
        engineConfig.callback = this;
        engineConfig.audioAecType = mAudioAecType;

        try {
            mRtcEngine = RtcEngine.create(engineConfig);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }
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
        RtcEngine.destroy();
        mRtcEngine = null;
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed();
        return true;
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
        config.subscribeAudioAll = true;
        mRtcEngine.joinChannel(mToken, mChannelId, mUserId, config);
    }

    private void leaveChannel() {
        if (mIsChannelJoined) {
            mRtcEngine.leaveChannel();
        }
    }

    void appendMessage(String str) {
        mMessage = str + "\n" + mMessage;
        mTextLog.setText(mMessage);
    }


    // -------------------------- RTC Engine Callbacks --------------------------
    public void onChannelJoinConfirm(Constants.QResult result) {
        Log.i(TAG, "onChannelJoinConfirm, result="+result);
        runOnUiThread(()->{
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
        });
    }

    public void onChannelLeaveIndication(Constants.QResult result) {
        Log.i(TAG, "onChannelLeaveIndication, result="+result);
        runOnUiThread(()-> {
            mIsChannelJoined = false;
            appendMessage("channel left, result=" + result);
            Toast.makeText(CallActivity.this, "onChannelLeaveIndication result=" + result, Toast.LENGTH_LONG).show();
            finish();
        });
    }
    public void onChannelCountDown(long remain) {
        Log.i(TAG, "onChannelCountDown, remain="+remain);
    }
    public void onUserJoinIndication(long userId, String userName) {
        Log.i(TAG, "onUserJoinIndication, userId="+userId+", userName="+userName);
        runOnUiThread(()-> {
            appendMessage("user joined, userId=" + userId + ", userName=" + userName);
            Toast.makeText(CallActivity.this, "onUserJoinIndication userId=" + userId, Toast.LENGTH_LONG).show();
        });
    }
    public void onUserLeaveIndication(long userId, Constants.UserLeaveReason reason) {
        Log.i(TAG, "onUserLeaveIndication, userId="+userId);
        runOnUiThread(()-> {
            appendMessage("user left, userId=" + userId + ", reason=" + reason);
            Toast.makeText(CallActivity.this, "onUserLeaveIndication userId=" + userId + ", reason=" + reason, Toast.LENGTH_LONG).show();
        });
    }
    public void onUserAudioStart(long userId) {
        Log.i(TAG, "onUserAudioStart, userId="+userId);
        runOnUiThread(()-> {
            appendMessage("user audio is started, userId=" + userId);
        });
    }
    public void onUserAudioStop(long userId) {
        Log.i(TAG, "onUserAudioStop, userId="+userId);
        runOnUiThread(()-> {
            appendMessage("user audio is stopped, userId=" + userId);
        });
    }
    public void onUserAudioSubscribe(long userId, Constants.MediaSubscribeResult result) {
        Log.i(TAG, "onUserAudioSubscribe, userId="+userId + ", result=" + result);
    }
    public void onUserVideoStart(long userId, Constants.VideoProfileType maxProfile) {
        runOnUiThread(()-> {
            appendMessage("user video is stopped, userId=" + userId + ", max=" + maxProfile);
        });
    }
    public void onUserVideoStop(long userId) {
        runOnUiThread(()-> {
            appendMessage("user video is stopped, userId=" + userId);
        });
    }
    public void onUserVideoSubscribe(long userId, Constants.MediaSubscribeResult result) {
        Log.i(TAG, "onUserVideoSubscribe, userId="+userId + ", result=" + result);
    }
    public void onUserAudioMute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user audio is muted, userId=" + userId);
        });
    }
    public void onUserAudioUnmute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user audio is unmuted, userId=" + userId);
        });
    }
    public void onUserVideoMute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user video is muted, userId=" + userId);
        });
    }
    public void onUserVideoUnmute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user video is unmuted, userId=" + userId);
        });
    }

    public void onUserScreenStart(long userId) {
        runOnUiThread(()-> {
            appendMessage("user screen is started, userId=" + userId);
        });
    }
    public void onUserScreenStop(long userId) {
        runOnUiThread(()-> {
            appendMessage("user screen is stopped, userId=" + userId);
        });
    }
    public void onUserScreenSubscribe(long userId, Constants.MediaSubscribeResult result) {
        Log.i(TAG, "onUserScreenSubscribe, userId="+userId + ", result=" + result);
    }
    public void onUserScreenMute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user screen is muted, userId=" + userId);
        });
    }
    public void onUserScreenUnmute(long userId) {
        runOnUiThread(()-> {
            appendMessage("user screen is unmuted, userId=" + userId);
        });
    }

    @Override
    public void onWhiteboardAvailable() {
        runOnUiThread(()-> {
            appendMessage("whiteboard is available");
        });
    }

    @Override
    public void onWhiteboardUnavailable() {
        runOnUiThread(()-> {
            appendMessage("whiteboard is unavailable");
        });
    }

    @Override
    public void onWhiteboardStart() {
        runOnUiThread(()-> {
            appendMessage("whiteboard is started");
        });
    }

    @Override
    public void onWhiteboardStop() {
        runOnUiThread(()-> {
            appendMessage("whiteboard is stopped");
        });
    }

    @Override
    public void onFirstAudioDataReceived(long userId) {
        Log.i(TAG, "+++++ onFirstAudioDataReceived: userId="+userId);
        runOnUiThread(()-> {
            appendMessage("first audio data is received, userId=" + userId);
        });
    }

    @Override
    public void onFirstVideoDataReceived(long userId) {
        Log.i(TAG, "+++++ onFirstVideoDataReceived: userId="+userId);
        runOnUiThread(()-> {
            appendMessage("first video state is received, userId=" + userId);
        });
    }

    @Override
    public void onFirstScreenDataReceived(long userId) {
        Log.i(TAG, "+++++ onFirstScreenDataReceived: userId="+userId);
        runOnUiThread(()-> {
            appendMessage("first screen data is received, userId=" + userId);
        });
    }

    @Override
    public void onAudioDeviceStateChanged(String deviceId,
                                          Constants.AudioDeviceType deviceType,
                                          Constants.AudioDeviceState deviceState) {
        Log.i(TAG, "+++++ onAudioDeviceStateChanged: "+deviceId);
        runOnUiThread(()-> {
            appendMessage("audio device state is changed, deviceId=" + deviceId);
        });
    }

    @Override
    public void onVideoDeviceStateChanged(String deviceId,
                                          Constants.VideoDeviceType deviceType,
                                          Constants.VideoDeviceState deviceState) {
        Log.i(TAG, "+++++ onVideoDeviceStateChanged: "+deviceId);
        runOnUiThread(()-> {
            appendMessage("video device state is changed, deviceId=" + deviceId);
        });
    }

    @Override
    public void onChannelFailover(Constants.FailoverState state) {
        Log.i(TAG, "+++++ onChannelFailover: state="+state.getValue());
        runOnUiThread(()-> {
            appendMessage("channel failover state: " + state);
        });
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
