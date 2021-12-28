package video.pano.demo.mixaudiocall;

import android.app.Application;

import com.pano.rtc.api.Constants;
import com.pano.rtc.api.RtcEngine;
import com.pano.rtc.api.RtcEngineConfig;


public class PanoApplication extends Application {
    /* Please refer to Glossary to understand the meaning of App ID, Channel ID, Token, User ID, and User Name:
       请参考 名词解释 了解 App ID、Channel ID、Token、User ID、User Name 的含义：
       https://developer.pano.video/getting-started/terms/

       You can use temporary token for temporary testing:
       可以使用 临时token 来进行临时测试：
       https://developer.pano.video/getting-started/firstapp/#14-%E7%94%9F%E6%88%90%E4%B8%B4%E6%97%B6token
    */
    public static final String APPID = %% Your App ID %%;
    public static final String APP_TOKEN = %% Your Token %%;
    public static final String PANO_SERVER = "api.pano.video";

    private RtcEngine mRtcEngine;
    private PanoEngineCallback mRtcCallback = new PanoEngineCallback();
    protected Constants.AudioAecType mAudioAecType = Constants.AudioAecType.Default;

    @Override
    public void onCreate() {
        super.onCreate();


        // 设置PANO媒体引擎的配置参数
        RtcEngineConfig engineConfig = new RtcEngineConfig();
        engineConfig.appId = APPID;
        // 仅私有化环境需要配置 server 属性
        engineConfig.server = PANO_SERVER;
        engineConfig.context = getApplicationContext();
        engineConfig.callback = mRtcCallback;
        engineConfig.audioAecType = mAudioAecType;
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
        mRtcEngine.destroy();
    }
}
