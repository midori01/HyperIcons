package com.midori.hypericons;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class ExperimentalActivity extends Activity {

    private Switch mSwitchKeepAlive;
    private TextView mBadgeStatus;
    private View mDotStatus;
    private TextView mTvStatusHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        mSwitchKeepAlive = findViewById(R.id.switch_5ga_keepalive);
        mBadgeStatus = findViewById(R.id.badge_5ga_keepalive_status);
        mDotStatus = findViewById(R.id.dot_exp_status);
        mTvStatusHint = findViewById(R.id.tv_exp_status_hint);

        SharedPreferences prefs = MainActivity.getSafePrefs(this);
        boolean enabled = prefs != null && prefs.getBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, false);

        if (mSwitchKeepAlive != null) {
            mSwitchKeepAlive.setChecked(enabled);
            mSwitchKeepAlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences p = MainActivity.getSafePrefs(ExperimentalActivity.this);
                    if (p != null) {
                        p.edit().putBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, isChecked).commit();
                        MainActivity.fixPermissions(ExperimentalActivity.this);
                        getContentResolver().notifyChange(IconConfigProvider.CONTENT_URI, null);

                        // Dispatch reload broadcast to notify phone and system services immediately
                        try {
                            Intent reloadIntent = new Intent(MainActivity.ACTION_RELOAD);
                            sendBroadcast(reloadIntent);
                        } catch (Throwable ignored) {}
                    }
                    updateUiState(isChecked);
                }
            });
        }

        updateUiState(enabled);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = MainActivity.getSafePrefs(this);
        boolean enabled = prefs != null && prefs.getBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, false);
        if (mSwitchKeepAlive != null && mSwitchKeepAlive.isChecked() != enabled) {
            mSwitchKeepAlive.setChecked(enabled);
        }
        updateUiState(enabled);
    }

    private void updateUiState(boolean enabled) {
        if (mBadgeStatus != null) {
            mBadgeStatus.setText(enabled ? R.string.status_enabled : R.string.status_disabled_default);
            mBadgeStatus.setTextColor(enabled ? getColor(R.color.accent) : getColor(R.color.text_caption));
        }

        if (mDotStatus != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            int dotPx = (int) (8 * getResources().getDisplayMetrics().density);
            dot.setSize(dotPx, dotPx);
            dot.setColor(enabled ? 0xFF34C759 : 0xFF8E8E93);
            mDotStatus.setBackground(dot);
        }

        if (mTvStatusHint != null) {
            mTvStatusHint.setText(enabled ? R.string.experimental_tip_active : R.string.experimental_tip_inactive);
            mTvStatusHint.setTextColor(enabled ? getColor(R.color.text_body) : getColor(R.color.text_caption));
        }
    }
}
