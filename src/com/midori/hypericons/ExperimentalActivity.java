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

        setupExplainCard();

        bindOption(R.id.switch_ignore_uli, R.id.badge_ignore_uli_status, ConfigKeys.KEY_IGNORE_ULI);
        bindOption(R.id.switch_nsa_nr_ca, R.id.badge_nsa_nr_ca_status, ConfigKeys.KEY_NSA_NR_CA);

        View btnRestartPhone = findViewById(R.id.btn_restart_phone);
        if (btnRestartPhone != null) {
            btnRestartPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmRestartPhone();
                }
            });
        }
    }

    private void setupExplainCard() {
        final View header = findViewById(R.id.explain_header);
        final View body = findViewById(R.id.explain_content);
        final View chevron = findViewById(R.id.explain_chevron);
        if (header == null || body == null || chevron == null) return;
        describeExplainCard(header, false);
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean expand = body.getVisibility() != View.VISIBLE;
                body.setVisibility(expand ? View.VISIBLE : View.GONE);
                chevron.animate().rotation(expand ? 180f : 0f).setDuration(200).start();
                describeExplainCard(header, expand);
            }
        });
    }

    private void describeExplainCard(View header, boolean expanded) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            header.setStateDescription(getString(expanded ? R.string.card_state_expanded : R.string.card_state_collapsed));
        }
    }

    private void bindOption(int switchId, int badgeId, final String key) {
        final Switch toggle = findViewById(switchId);
        final TextView badge = findViewById(badgeId);
        if (toggle == null || badge == null) return;
        toggle.setChecked(isOptionOn(key));
        showOptionBadge(badge, toggle.isChecked());
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences p = MainActivity.getSafePrefs(ExperimentalActivity.this);
                if (p != null) {
                    p.edit().putString(key, isChecked ? ConfigKeys.VALUE_ON : ConfigKeys.DEFAULT_VALUE).commit();
                    MainActivity.fixPermissions(ExperimentalActivity.this);
                    getContentResolver().notifyChange(IconConfigProvider.CONTENT_URI, null);
                }
                showOptionBadge(badge, isChecked);
            }
        });
    }

    private boolean isOptionOn(String key) {
        SharedPreferences prefs = MainActivity.getSafePrefs(this);
        return prefs != null && ConfigKeys.VALUE_ON.equals(prefs.getString(key, ConfigKeys.DEFAULT_VALUE));
    }

    private void showOptionBadge(TextView badge, boolean on) {
        badge.setText(on ? R.string.status_enabled : R.string.status_disabled_default);
        badge.setTextColor(on ? getColor(R.color.accent) : getColor(R.color.text_caption));
    }

    private void confirmRestartPhone() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_restart_phone_title)
                .setMessage(R.string.dialog_restart_phone_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_restart_phone_text, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        restartPhoneProcess();
                    }
                })
                .show();
    }

    /** Kills the persistent com.android.phone process; the system restarts it and the hooks load again. */
    private void restartPhoneProcess() {
        android.widget.Toast.makeText(this, R.string.toast_restart_phone_requesting, android.widget.Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success;
                try {
                    Process process = Runtime.getRuntime().exec(new String[]{
                            "su", "-c", "pid=$(pidof com.android.phone) && kill $pid"});
                    success = process.waitFor() == 0;
                } catch (Throwable t) {
                    success = false;
                }
                final boolean ok = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        android.widget.Toast.makeText(ExperimentalActivity.this,
                                ok ? R.string.toast_restart_phone_success : R.string.toast_restart_phone_failed,
                                ok ? android.widget.Toast.LENGTH_SHORT : android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
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
