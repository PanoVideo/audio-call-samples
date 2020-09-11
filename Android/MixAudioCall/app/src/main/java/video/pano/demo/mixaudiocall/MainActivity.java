package video.pano.demo.mixaudiocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.pano.rtc.api.RtcEngine;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 10;

    private EditText mChannelId;
    private EditText mUserId;

    private Switch mMode1v1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChannelId = findViewById(R.id.editChannelID);
        mChannelId.setText("12349");
        mChannelId.setRawInputType(Configuration.KEYBOARD_QWERTY);
        mUserId = findViewById(R.id.editUserID);
        long uid = (long)10000 + new Random().nextInt(1000) ;
        mUserId.setText( Long.toString(uid) );
        findViewById(R.id.buttonJoinChannel).setOnClickListener(v -> joinChannel());

        mMode1v1 = findViewById(R.id.switchMode1v1);
        mMode1v1.setChecked(false);

        mChannelId.requestFocus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (RtcEngine.checkPermission(this).size() == 0) {
                    startCall(PanoApplication.APP_TOKEN, mChannelId.getText().toString(), mUserId.getText().toString());
                } else {
                    Toast.makeText(MainActivity.this, "Some permissions are denied", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void joinChannel() {
        String userId = mUserId.getText().toString();
        String channelId = mChannelId.getText().toString();
        if (TextUtils.isEmpty(userId)) {
            mUserId.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            mChannelId.setError("Required");
            return;
        }

        final List<String> missed = RtcEngine.checkPermission(this);
        if (checkPermission(READ_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid())
                != PackageManager.PERMISSION_GRANTED) {
            missed.add(READ_EXTERNAL_STORAGE);
        }
        if (missed.size() != 0) {

            List<String> showRationale = new ArrayList<>();
            for (String permission : missed) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    showRationale.add(permission);
                }
            }

            if (showRationale.size() > 0) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Please allow the permissions")
                        .setPositiveButton("OK", (dialog, which) ->
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        missed.toArray(new String[0]),
                                        PERMISSION_REQUEST_CODE))
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, missed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }

            return;
        }

        startCall(PanoApplication.APP_TOKEN, channelId, userId);
    }

    private void startCall(String token, String channelId, String userId) {
        Intent intent = new Intent();
        intent.putExtra("token", token);
        intent.putExtra("channelId", channelId);
        intent.putExtra("userId", Long.parseLong(userId));
        intent.putExtra("mode1v1", mMode1v1.isChecked());
        intent.setClass(this, CallActivity.class);
        startActivity(intent);
    }
}
