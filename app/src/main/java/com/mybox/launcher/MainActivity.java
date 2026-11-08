package com.mybox.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int background = Color.rgb(16, 17, 20);
    private final int surface = Color.rgb(31, 33, 38);
    private final int foreground = Color.rgb(245, 245, 245);
    private final int muted = Color.rgb(155, 160, 170);

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

        TextView status = text("VPN     Wi-Fi     Bluetooth", 14, muted, false);
        header.addView(status);
        root.addView(header);

        TextView date = text("ВАШ ТЕЛЕВИЗОР. ВАШИ ПРАВИЛА.", 13, muted, true);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        dateParams.topMargin = dp(42);
        root.addView(date, dateParams);

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tilesParams = new LinearLayout.LayoutParams(-1, dp(150));
        tilesParams.topMargin = dp(22);
        root.addView(tiles, tilesParams);

        addTile(tiles, "Приложения", "Все установленные приложения", true);
        addTile(tiles, "Избранное", "Быстрый доступ", false);
        addTile(tiles, "Настройки", "Оформление и система", false);

        TextView footer = text("MY BOX  •  ЛЁГКИЙ. БЫСТРЫЙ. ТВОЙ.", 11, muted, false);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-2, -2);
        footerParams.topMargin = dp(38);
        root.addView(footer, footerParams);

        return root;
    }

    private void addTile(LinearLayout parent, String title, String subtitle, boolean focused) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(20), dp(16), dp(16), dp(16));
        tile.setBackgroundColor(focused ? Color.rgb(53, 56, 64) : surface);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);

        TextView heading = text(title, 19, foreground, true);
        TextView detail = text(subtitle, 12, muted, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(9);
        tile.addView(heading);
        tile.addView(detail, detailParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(0), 0, dp(14), 0);
        parent.addView(tile, params);
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
