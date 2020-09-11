package video.pano.demo.mixaudiocall;

import android.app.Application;

import com.pano.rtc.api.Constants;
import com.pano.rtc.api.RtcEngine;
import com.pano.rtc.api.RtcEngineConfig;


public class PanoApplication extends Application {
    public static final String APPID = "243540d6698e4463a52cebc72ba977de";
    public static final String APP_TOKEN = "01000003Vk1qckE5V0tE3QLZ7hPtRUtZVPWA30J1nnCDV4U4kXJ0WUomGiFrpxVxB5sKFrPvnEjR0e3RPns0eoCX8QhbQmLdwqyMfnnzRGGOZZMBhZC8kzRH5UhoMV9cZBfxL7nQ3ig52nVLsJAxWs/bveLFCVSFGFyK6C4zbck+NCUrpozqVGw7b0u81P12y17LwMdH6tE5M2e14moz1XFSTtlKOXN+6dcy5aZ+CeA0iISKTlPuisD4I6vQHzZmRNQWMFVZr+5ch0krbzU=";
    public static final String PANO_SERVER = "api.pano.video";

    private RtcEngine mRtcEngine;
    private PanoEngineCallback mRtcCallback = new PanoEngineCallback();

    protected Constants.AudioAecType mAudioAecType = Constants.AudioAecType.Default;
    protected boolean mHwAcceleration = false;


    @Override
    public void onCreate() {
        super.onCreate();


        // 设置PANO媒体引擎的配置参数
        RtcEngineConfig engineConfig = new RtcEngineConfig();
        engineConfig.appId = APPID;
        engineConfig.server = PANO_SERVER;
        engineConfig.context = getApplicationContext();
        engineConfig.callback = mRtcCallback;
        engineConfig.audioAecType = mAudioAecType;
        engineConfig.videoCodecHwAcceleration = mHwAcceleration;
        try {
            mRtcEngine = RtcEngine.create(engineConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RtcEngine getPanoEngine() { return mRtcEngine; }
    public PanoEngineCallback getPanoCallback() { return mRtcCallback; }

    public void registerEventHandler(PanoEventHandler handler) { mRtcCallback.addHandler(handler); }
    public void removeEventHandler(PanoEventHandler handler) { mRtcCallback.removeHandler(handler); }

    @Override
    public void onTerminate() {
        super.onTerminate();
        RtcEngine.destroy();
    }
}
