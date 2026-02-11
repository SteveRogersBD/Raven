package com.example.plateit.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;
import com.example.plateit.R;

public class LoadingDialog {

    private Dialog dialog;
    private Activity activity;

    public LoadingDialog(Activity activity) {
        this.activity = activity;
    }

    public void startLoadingDialog() {
        if (activity == null || activity.isFinishing())
            return;

        if (dialog != null && dialog.isShowing())
            return;

        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loading);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    public void startLoadingDialog(String message) {
        if (activity == null || activity.isFinishing())
            return;

        startLoadingDialog();

        if (dialog != null) {
            TextView tvMessage = dialog.findViewById(R.id.tvLoadingMessage);
            if (tvMessage != null) {
                tvMessage.setText(message);
            }
        }
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
