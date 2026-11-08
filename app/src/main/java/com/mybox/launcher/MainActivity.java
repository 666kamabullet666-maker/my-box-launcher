package com.mybox.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int background = Color.rgb(16, 17, 20);
    private final int surface = Color.rgb(31, 33, 38);
    private final int foreground = Color.rgb(245, 245, 245);
    private final int muted = Color.rgb(155, 160, 170);
    private TextView dpiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(buildHome());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dpiStatus != null) {
            boolean installed = getPackageManager().getLaunchIntentForPackage(BuildConfig.DPI_PACKAGE) != null;
            dpiStatus.setText(installed ? "Приложение установлено • запуск доступен" : "Не установлено • требуется установка APK");
        }
    }

    private View buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(48), dp(28), dp(48), dp(28));
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text("MY BOX", 28, foreground, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(text("VPN     Wi-Fi     Bluetooth", 14, muted, false));
        root.addView(header);

        TextView tagline = text("ВАШ ТЕЛЕВИЗОР. ВАШИ ПРАВИЛА.", 13, muted, true);
        LinearLayout.LayoutParams taglineParams = new LinearLayout.LayoutParams(-2, -2);
        taglineParams.topMargin = dp(32);
        root.addView(tagline, taglineParams);

        TextView engine = text(BuildConfig.DPI_TITLE, 23, foreground, true);
        LinearLayout.LayoutParams engineParams = new LinearLayout.LayoutParams(-2, -2);
        engineParams.topMargin = dp(18);
        root.addView(engine, engineParams);

        TextView description = text(BuildConfig.DPI_DESCRIPTION, 13, muted, false);
        description.setMaxWidth(dp(850));
        description.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(8);
        root.addView(description, descParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, dp(118));
        controlsParams.topMargin = dp(22);
        root.addView(controls, controlsParams);

        LinearLayout dpiTile = tile("DPI • ЗАПУСТИТЬ", "Открыть выбранный движок", true);
        dpiTile.setOnClickListener(v -> launchDpi());
        controls.addView(dpiTile, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout vpnTile = tile("VPN", "Назначение приложения — в настройках", false);
        vpnTile.setOnClickListener(v -> Toast.makeText(this,
                "Выбор VPN-приложения добавим в настройках", Toast.LENGTH_SHORT).show());
        LinearLayout.LayoutParams vpnParams = new LinearLayout.LayoutParams(0, -1, 1);
        vpnParams.leftMargin = dp(14);
        controls.addView(vpnTile, vpnParams);

        dpiStatus = text("Проверка приложения…", 12, muted, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(12);
        root.addView(dpiStatus, statusParams);

        TextView footer = text("DPI работает локально через Android VpnService. Это не VPN-сервер и не смена IP.", 11, muted, false);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(26);
        root.addView(footer, footerParams);
        return root;
    }

    private LinearLayout tile(String title, String subtitle, boolean focused) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(20), dp(16), dp(16), dp(16));
        tile.setBackgroundColor(focused ? Color.rgb(53, 56, 64) : surface);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.addView(text(title, 18, foreground, true));
        TextView detail = text(subtitle, 12, muted, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8);
        tile.addView(detail, p);
        return tile;
    }

    private void launchDpi() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(BuildConfig.DPI_PACKAGE);
        if (launch == null) {
            Toast.makeText(this, BuildConfig.DPI_TITLE + ": приложение не установлено", Toast.LENGTH_LONG).show();
            dpiStatus.setText("Не установлено • установите APK движка DPI");
            return;
        }
        try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            Toast.makeText(this, BuildConfig.DPI_TITLE + ": приложение открыто", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть " + BuildConfig.DPI_TITLE, Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
