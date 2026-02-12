package com.example.plateit.utils;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.plateit.R;
import com.google.android.material.button.MaterialButton;

public class AppBarHelper {
    public static void setup(Activity activity, String title, boolean showBack) {
        setup(activity, title, showBack, 0, null);
    }

    public static void setup(Activity activity, String title, boolean showBack, int secondaryIcon,
            Runnable onSecondary) {
        setup(activity, title, showBack, secondaryIcon, null, onSecondary);
    }

    /**
     * Setup with optional text action button (takes precedence over icon if
     * actionText is not null)
     */
    public static void setup(Activity activity, String title, boolean showBack, int secondaryIcon,
            String actionText, Runnable onAction) {
        View appBar = activity.findViewById(R.id.app_bar);
        if (appBar != null) {
            TextView tvTitle = appBar.findViewById(R.id.tvAppBarTitle);
            if (tvTitle != null) {
                tvTitle.setText(title);
            }

            View btnBack = appBar.findViewById(R.id.btnBack);
            if (btnBack != null) {
                if (showBack) {
                    btnBack.setVisibility(View.VISIBLE);
                    btnBack.setOnClickListener(v -> activity.onBackPressed());
                } else {
                    btnBack.setVisibility(View.GONE);
                }
            }

            ImageButton btnSecondary = appBar.findViewById(R.id.btnSecondaryAction);
            MaterialButton btnText = appBar.findViewById(R.id.btnTextAction);

            if (actionText != null && btnText != null) {
                btnText.setVisibility(View.VISIBLE);
                btnText.setText(actionText);
                btnText.setOnClickListener(v -> {
                    if (onAction != null)
                        onAction.run();
                });
                if (btnSecondary != null)
                    btnSecondary.setVisibility(View.GONE);
            } else if (secondaryIcon != 0 && btnSecondary != null) {
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setImageResource(secondaryIcon);
                btnSecondary.setOnClickListener(v -> {
                    if (onAction != null)
                        onAction.run();
                });
                if (btnText != null)
                    btnText.setVisibility(View.GONE);
            } else {
                if (btnSecondary != null)
                    btnSecondary.setVisibility(View.GONE);
                if (btnText != null)
                    btnText.setVisibility(View.GONE);
            }
        }
    }

    public static void setup(View rootView, String title, boolean showBack, Runnable onBack) {
        View appBar = rootView.findViewById(R.id.app_bar);
        if (appBar != null) {
            TextView tvTitle = appBar.findViewById(R.id.tvAppBarTitle);
            if (tvTitle != null) {
                tvTitle.setText(title);
            }
            View btnBack = appBar.findViewById(R.id.btnBack);
            if (btnBack != null) {
                if (showBack) {
                    btnBack.setVisibility(View.VISIBLE);
                    if (onBack != null) {
                        btnBack.setOnClickListener(v -> onBack.run());
                    }
                } else {
                    btnBack.setVisibility(View.GONE);
                }
            }
        }
    }
}
