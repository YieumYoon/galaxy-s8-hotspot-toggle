package dev.legacyhotspot.s8;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.NetworkInterface;
import java.net.SocketException;

public final class MainActivity extends Activity {
    private static final String ACTION_DRIVER_HANGED =
            "com.samsung.android.net.wifi.WIFI_AP_DRIVER_STATE_HANGED";
    private static final String ACTION_WIFI_AP_STATE_CHANGED =
            "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String SETTINGS_RECEIVER =
            "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver";
    private static final String SETTINGS_WIFI_WARNING =
            "com.samsung.android.settings.wifi.WifiWarning";

    private static final int WIFI_AP_STATE_DISABLING = 10;
    private static final int WIFI_AP_STATE_DISABLED = 11;
    private static final int WIFI_AP_STATE_ENABLING = 12;
    private static final int WIFI_AP_STATE_ENABLED = 13;
    private static final int WIFI_AP_STATE_FAILED = 14;
    private static final long OPERATION_TIMEOUT_MS = 8_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Button toggleButton;
    private TextView statusView;
    private boolean stateReceiverRegistered;
    private boolean compatibleComponents;
    private int currentHotspotState = WIFI_AP_STATE_DISABLED;

    private final Runnable operationTimeout = new Runnable() {
        @Override
        public void run() {
            int fallbackState = getInterfaceFallbackState();
            if (fallbackState == WIFI_AP_STATE_ENABLED
                    || fallbackState == WIFI_AP_STATE_DISABLED) {
                updateHotspotState(fallbackState);
            } else {
                updateHotspotState(WIFI_AP_STATE_FAILED);
            }
        }
    };

    private final BroadcastReceiver hotspotStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_WIFI_AP_STATE_CHANGED.equals(intent.getAction())) {
                updateHotspotState(intent.getIntExtra(
                        EXTRA_WIFI_AP_STATE, getInterfaceFallbackState()));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(48), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView title = new TextView(this);
        title.setText("S8 Hotspot");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(25, 25, 25));
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(16)));

        TextView description = new TextView(this);
        description.setText("Connect this phone to the Wi-Fi network you want to share, "
                + "then tap the button below.\nThis starts Wi-Fi sharing without a SIM card.");
        description.setTextSize(17);
        description.setTextColor(Color.rgb(70, 70, 70));
        description.setGravity(Gravity.CENTER);
        root.addView(description, matchWrap(dp(28)));

        toggleButton = new Button(this);
        toggleButton.setText("Start Hotspot");
        toggleButton.setTextSize(20);
        toggleButton.setAllCaps(false);
        toggleButton.setMinHeight(dp(58));
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentHotspotState == WIFI_AP_STATE_ENABLED) {
                    stopHotspot();
                } else {
                    startHotspot();
                }
            }
        });
        root.addView(toggleButton, matchWrap(dp(24)));

        statusView = new TextView(this);
        statusView.setText("Checking hotspot status…");
        statusView.setTextSize(18);
        statusView.setTextColor(Color.rgb(70, 70, 70));
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView, matchWrap(0));

        setContentView(root);

        compatibleComponents = hasCompatibleSettingsComponents();
        if (!compatibleComponents) {
            statusView.setText("●  This firmware is not compatible");
            statusView.setTextColor(Color.rgb(180, 35, 35));
            toggleButton.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!compatibleComponents) {
            return;
        }

        IntentFilter filter = new IntentFilter(ACTION_WIFI_AP_STATE_CHANGED);
        Intent currentState = registerReceiver(hotspotStateReceiver, filter);
        stateReceiverRegistered = true;

        if (currentState != null) {
            updateHotspotState(currentState.getIntExtra(
                    EXTRA_WIFI_AP_STATE, getInterfaceFallbackState()));
        } else {
            updateHotspotState(getInterfaceFallbackState());
        }
    }

    @Override
    protected void onStop() {
        if (stateReceiverRegistered) {
            unregisterReceiver(hotspotStateReceiver);
            stateReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(operationTimeout);
        super.onDestroy();
    }

    private boolean hasCompatibleSettingsComponents() {
        PackageManager packageManager = getPackageManager();
        try {
            ActivityInfo receiverInfo = packageManager.getReceiverInfo(
                    new ComponentName(SETTINGS_PACKAGE, SETTINGS_RECEIVER), 0);
            ActivityInfo warningInfo = packageManager.getActivityInfo(
                    new ComponentName(SETTINGS_PACKAGE, SETTINGS_WIFI_WARNING), 0);
            return receiverInfo.exported && warningInfo.exported;
        } catch (PackageManager.NameNotFoundException exception) {
            return false;
        }
    }

    private void startHotspot() {
        Intent intent = new Intent(ACTION_DRIVER_HANGED);
        intent.setComponent(new ComponentName(SETTINGS_PACKAGE, SETTINGS_RECEIVER));
        intent.putExtra("wifi_ap_error_code", 14);

        try {
            sendBroadcast(intent);
            updateHotspotState(WIFI_AP_STATE_ENABLING);
            scheduleOperationTimeout();
        } catch (RuntimeException exception) {
            updateHotspotState(WIFI_AP_STATE_FAILED);
        }
    }

    private void stopHotspot() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SETTINGS_PACKAGE, SETTINGS_WIFI_WARNING));
        intent.putExtra("req_type", 1);
        intent.putExtra("extra_type", 1);
        try {
            startActivity(intent);
            updateHotspotState(WIFI_AP_STATE_DISABLING);
            scheduleOperationTimeout();
        } catch (RuntimeException exception) {
            showStopFailure("Could not stop the hotspot");
        }
    }

    private void scheduleOperationTimeout() {
        mainHandler.removeCallbacks(operationTimeout);
        mainHandler.postDelayed(operationTimeout, OPERATION_TIMEOUT_MS);
    }

    private void updateHotspotState(int state) {
        currentHotspotState = state;
        if (state != WIFI_AP_STATE_ENABLING && state != WIFI_AP_STATE_DISABLING) {
            mainHandler.removeCallbacks(operationTimeout);
        }

        switch (state) {
            case WIFI_AP_STATE_DISABLING:
                statusView.setText("●  Hotspot is turning off…");
                statusView.setTextColor(Color.rgb(190, 125, 20));
                toggleButton.setText("Please Wait…");
                toggleButton.setEnabled(false);
                break;
            case WIFI_AP_STATE_DISABLED:
                statusView.setText("●  Hotspot is OFF");
                statusView.setTextColor(Color.rgb(95, 95, 95));
                toggleButton.setText("Start Hotspot");
                toggleButton.setEnabled(true);
                break;
            case WIFI_AP_STATE_ENABLING:
                statusView.setText("●  Hotspot is starting…");
                statusView.setTextColor(Color.rgb(45, 105, 190));
                toggleButton.setText("Starting…");
                toggleButton.setEnabled(false);
                break;
            case WIFI_AP_STATE_ENABLED:
                statusView.setText("●  Hotspot is ON");
                statusView.setTextColor(Color.rgb(20, 135, 65));
                toggleButton.setText("Stop Hotspot");
                toggleButton.setEnabled(true);
                break;
            case WIFI_AP_STATE_FAILED:
                statusView.setText("●  Hotspot operation failed");
                statusView.setTextColor(Color.rgb(180, 35, 35));
                toggleButton.setText("Try Again");
                toggleButton.setEnabled(true);
                break;
            default:
                statusView.setText("●  Hotspot status unavailable");
                statusView.setTextColor(Color.rgb(190, 125, 20));
                toggleButton.setText("Start Hotspot");
                toggleButton.setEnabled(true);
                break;
        }
    }

    private void showStopFailure(String message) {
        statusView.setText("●  " + message);
        statusView.setTextColor(Color.rgb(180, 35, 35));
        toggleButton.setText("Stop Hotspot");
        toggleButton.setEnabled(true);
    }

    private int getInterfaceFallbackState() {
        try {
            NetworkInterface hotspotInterface = NetworkInterface.getByName("swlan0");
            if (hotspotInterface != null && hotspotInterface.isUp()) {
                return WIFI_AP_STATE_ENABLED;
            }
            return WIFI_AP_STATE_DISABLED;
        } catch (SocketException exception) {
            return -1;
        }
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomMargin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
