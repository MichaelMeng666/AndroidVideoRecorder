package michael.com.recordvideolibrary.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import michael.com.recordvideolibrary.R;

public class CommonDialog extends Dialog implements View.OnClickListener {
    private Context mContext;
    private TextView tv_title;
    private TextView tv_content;
    private TextView tv_sure;
    private TextView tv_cancel;
    private View v_divider;
    protected OnDialogClickListener listener;

    private String mTitle;
    protected String mContent;
    private String mBtnSureText;
    private String mBtnCancelText;
    private int mBtnSureVisible = View.VISIBLE;
    private int mTVContentGravity = Gravity.LEFT;

    private int mContentColor;
    private int mBtnCancelColor;
    private int mBtnSureColor;

    public CommonDialog(Context context) {
        super(context, R.style.DialogWithAnim);
        mContext = context;
    }

    public CommonDialog(Context context, int theme) {
        super(context, theme);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public void setBtnSureText(String btnOkText) {
        mBtnSureText = btnOkText;
    }

    public void setBtnSureVisible(int visible) {
        mBtnSureVisible = visible;
    }

    public void setBtnCancelText(String btnCancelText) {
        mBtnCancelText = btnCancelText;
    }

    public void setContentColor(int contentColor) {
        mContentColor = contentColor;
    }

    public void setContentGravity(int tvContentGravity) {
        mTVContentGravity = tvContentGravity;
    }

    public void setBtnCancelColor(int btnCancelColor) {
        mBtnCancelColor = btnCancelColor;
    }

    public void setBtnSureColor(int btnCancelColor) {
        mBtnSureColor = btnCancelColor;
    }

    public void setOnDialogClickListener(OnDialogClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindowAttrs();
        initViews();
    }

    private void initWindowAttrs() {
        Window dialogWindow = getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);
    }

    protected void initViews() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(getLayoutId(), null);
        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = (int) mContext.getResources().getFraction(
                R.fraction.common_dialog_width, width, width);
        setContentView(view, new ViewGroup.LayoutParams(dialogWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_content = (TextView) findViewById(R.id.tv_content);
        tv_cancel = (TextView) findViewById(R.id.tv_cancel);
        tv_sure = (TextView) findViewById(R.id.tv_sure);
        v_divider = findViewById(R.id.v_divider);
        tv_cancel.setOnClickListener(this);
        tv_sure.setOnClickListener(this);
    }

    protected int getLayoutId() {
        return R.layout.new_common_dialog;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_sure) {
            dismiss();
            if (listener != null) {
                listener.onDialogClickSure();
            }
        } else if (i == R.id.tv_cancel) {
            dismiss();
            if (listener != null) {
                listener.onDialogClickCancel();
            }

        }
    }

    @Override
    public void show() {
        super.show();
        if (!TextUtils.isEmpty(mTitle)) {
            tv_title.setText(mTitle);
            tv_title.setVisibility(View.VISIBLE);
        } else {
            tv_title.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mContent)) {
            tv_content.setText(mContent);
        }
        if (Gravity.LEFT == mTVContentGravity) {
            tv_content.setGravity(Gravity.LEFT);
        }
        if (mContentColor > 0) {
            tv_content.setTextColor(mContext.getResources().getColor(mContentColor));
        }

        if (View.VISIBLE == mBtnSureVisible) {
            if (!TextUtils.isEmpty(mBtnSureText)) {
                tv_sure.setText(mBtnSureText);
            }
        } else {
            v_divider.setVisibility(View.GONE);
            tv_sure.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mBtnCancelText)) {
            tv_cancel.setText(mBtnCancelText);
        }
        if (mBtnCancelColor > 0) {
            tv_cancel.setTextColor(mContext.getResources().getColor(mBtnCancelColor));
        }
        if (mBtnSureColor > 0) {
            tv_sure.setTextColor(mContext.getResources().getColor(mBtnSureColor));
        }
    }

    public interface OnDialogClickListener {
        void onDialogClickSure();

        void onDialogClickCancel();
    }
}