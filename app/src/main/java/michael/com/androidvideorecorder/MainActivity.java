package michael.com.androidvideorecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import michael.com.recordvideolibrary.activity.RecorderActivity;
import michael.com.recordvideolibrary.common.CommonDialog;

public class MainActivity extends AppCompatActivity {

    private TextView tv_start_record;
    private CommonDialog permissionDialog;
    private Context context = this;
    private final int VIDEO_REQUEST_PERMISSION_CODE = 0X001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_start_record = (TextView) findViewById(R.id.tv_start_record);
        tv_start_record.setOnClickListener(new OnClickStartBtnListener());
    }

    private class OnClickStartBtnListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            int permissionState_Camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            int permissionState_Storage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            int permissionState_Audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
            if (permissionState_Camera != PackageManager.PERMISSION_GRANTED || permissionState_Storage != PackageManager.PERMISSION_GRANTED || permissionState_Audio != PackageManager.PERMISSION_GRANTED) {
                // 用户已经拒绝过一次，给用户解释提示对话框
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.CAMERA)) {
                    permissionDialog = new CommonDialog(context);
                    permissionDialog.setContent("请授予相应权限");
                    permissionDialog.setBtnSureText("确定");
                    permissionDialog.setBtnCancelText("取消");
                    permissionDialog.setCancelable(true);
                    permissionDialog.setCanceledOnTouchOutside(false);
                    permissionDialog.setOnDialogClickListener(new OnClickPermissionDialog());
                    permissionDialog.show();
                } else {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, VIDEO_REQUEST_PERMISSION_CODE);
                }
            } else {
                Intent intent = new Intent(MainActivity.this, RecorderActivity.class);
                startActivity(intent);
            }
        }
    }

    private class OnClickPermissionDialog implements CommonDialog.OnDialogClickListener {

        @Override
        public void onDialogClickSure() {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, VIDEO_REQUEST_PERMISSION_CODE);
        }

        @Override
        public void onDialogClickCancel() {
            permissionDialog.dismiss();
        }
    }
}