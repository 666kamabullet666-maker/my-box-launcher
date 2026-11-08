package com.mybox.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 12, 31);
    private static final int PANEL = Color.rgb(20, 31, 62);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(174, 188, 218);
    private static final int BLUE = Color.rgb(35, 112, 225);
    private static final int PURPLE = Color.rgb(103, 48, 220);
    private static final String PREFS = "mybox";
    private static final String VPN_PACKAGE = "vpn_package";
    private LinearLayout root;
    private TextView dpiStatus;
    private TextView vpnStatus;
    private LinearLayout appGrid;
    private final List<AppEntry> apps = new ArrayList<>();

    private static class AppEntry {
        String label;
        String packageName;
        android.graphics.drawable.Drawable icon;
        Intent launchIntent;
        AppEntry(String l, String p, android.graphics.drawable.Drawable i, Intent intent) {
            label=l; packageName=p; icon=i; launchIntent=intent;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        loadApps();
        buildScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dpiStatus != null) updateStatuses();
        if (appGrid != null) { loadApps(); refreshApps(); }
    }

    private void loadApps() {
        Map<String, AppEntry> unique = new LinkedHashMap<>();
        PackageManager pm = getPackageManager();
        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        addResolved(unique, pm, leanback);
        addResolved(unique, pm, launcher);
        apps.clear();
        apps.addAll(unique.values());
        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase()));
    }

    private void addResolved(Map<String, AppEntry> out, PackageManager pm, Intent query) {
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(query.hasCategory("android.intent.category.LEANBACK_LAUNCHER")
                    ? "android.intent.category.LEANBACK_LAUNCHER" : Intent.CATEGORY_LAUNCHER);
            launch.setClassName(pkg, ri.activityInfo.name);
            String key = pkg;
            if (!out.containsKey(key)) {
                CharSequence label = ri.loadLabel(pm);
                out.put(key, new AppEntry(label == null ? pkg : label.toString(), pkg, ri.loadIcon(pm), launch));
            }
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(24), dp(42), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = row();
        TextView brand = label("MY BOX", 30, TEXT, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(label("ANDROID TV  •  " + android.os.Build.VERSION.RELEASE, 13, MUTED, true));
        root.addView(header);

        TextView tagline = label("ПОДКЛЮЧАЙСЯ. СМОТРИ. ИСПОЛЬЗУЙ.", 13, MUTED, true);
        LinearLayout.LayoutParams tp = params(-2, -2); tp.topMargin=dp(8); root.addView(tagline,tp);

        LinearLayout hero = row();
        LinearLayout.LayoutParams hp = params(-1, dp(172)); hp.topMargin=dp(26); root.addView(hero,hp);
        LinearLayout dpi = heroTile("DPI", "Запустить " + BuildConfig.DPI_TITLE, PURPLE);
        dpi.setOnClickListener(v -> launchPackage(BuildConfig.DPI_PACKAGE, BuildConfig.DPI_TITLE, dpiStatus));
        hero.addView(dpi,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout vpn = heroTile("VPN", "Выбрать и запустить VPN-клиент", BLUE);
        vpn.setOnClickListener(v -> chooseVpn());
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0,-1,1); vp.leftMargin=dp(16); hero.addView(vpn,vp);

        LinearLayout statuses = row();
        LinearLayout.LayoutParams sp = params(-1,-2); sp.topMargin=dp(12); root.addView(statuses,sp);
        dpiStatus=label("",13,MUTED,false); statuses.addView(dpiStatus,new LinearLayout.LayoutParams(0,-2,1));
        vpnStatus=label("",13,MUTED,false); statuses.addView(vpnStatus);

        LinearLayout appsHeader=row(); LinearLayout.LayoutParams ah=params(-1,-2); ah.topMargin=dp(28); root.addView(appsHeader,ah);
        appsHeader.addView(label("ВСЕ ПРИЛОЖЕНИЯ",20,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView refresh=label("ОБНОВИТЬ  ↻",12,MUTED,true); refresh.setFocusable(true); refresh.setOnClickListener(v->{loadApps();refreshApps();}); appsHeader.addView(refresh);

        appGrid = new LinearLayout(this); appGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gp=params(-1,-2); gp.topMargin=dp(14); root.addView(appGrid,gp);
        refreshApps();

        TextView foot=label("Навигация: ← ↑ → ↓   OK — открыть   BACK — назад",12,MUTED,false);
        LinearLayout.LayoutParams fp=params(-2,-2); fp.topMargin=dp(24); root.addView(foot,fp);
        updateStatuses();
    }

    private void refreshApps() {
        if(appGrid==null) return;
        appGrid.removeAllViews();
        if(apps.isEmpty()) {
            appGrid.addView(label("Не найдено приложений с поддержкой запуска на Android TV.",14,MUTED,false));
            return;
        }
        int columns = getResources().getConfiguration().screenWidthDp >= 900 ? 6 : 4;
        LinearLayout line=null;
        for(int i=0;i<apps.size();i++) {
            if(i%columns==0) { line=row(); LinearLayout.LayoutParams lp=params(-1,dp(104)); if(i>0) lp.topMargin=dp(10); appGrid.addView(line,lp); }
            AppEntry entry=apps.get(i);
            LinearLayout card=appCard(entry);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);
            if(i%columns!=0) cp.leftMargin=dp(10);
            line.addView(card,cp);
        }
    }

    private LinearLayout appCard(AppEntry app) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER); card.setPadding(dp(8),dp(8),dp(8),dp(8));
        card.setBackground(panel(PANEL,dp(14),Color.rgb(43,65,112),dp(1)));
        card.setFocusable(true); card.setClickable(true);
        ImageView icon=new ImageView(this); icon.setImageDrawable(app.icon);
        LinearLayout.LayoutParams ip=params(dp(42),dp(42)); card.addView(icon,ip);
        TextView name=label(app.label,13,TEXT,true); name.setGravity(Gravity.CENTER);
        name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np=params(-1,-2); np.topMargin=dp(7); card.addView(name,np);
        card.setOnClickListener(v->launchIntent(app.launchIntent,app.label));
        focusStyle(card);
        return card;
    }

    private LinearLayout heroTile(String title,String sub,int accent) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL); box.setPadding(dp(24),dp(18),dp(18),dp(18));
        box.setBackground(panel(accent,dp(20),Color.argb(170,150,175,255),dp(1)));
        box.setFocusable(true); box.setClickable(true);
        TextView t=label(title,27,TEXT,true); box.addView(t);
        TextView s=label(sub,14,Color.rgb(225,232,250),false);
        LinearLayout.LayoutParams p=params(-1,-2); p.topMargin=dp(10); box.addView(s,p);
        focusStyle(box); return box;
    }

    private void chooseVpn() {
        List<AppEntry> candidates=new ArrayList<>();
        for(AppEntry a:apps) {
            String s=(a.label+" "+a.packageName).toLowerCase();
            if(s.contains("vpn") || s.contains("wireguard") || s.contains("openvpn") ||
                    s.contains("v2ray") || s.contains("sing") || s.contains("hiddify") ||
                    s.contains("nekobox") || s.contains("outline") || s.contains("clash") ||
                    s.contains("amnezia") || s.contains("1.1.1.1")) candidates.add(a);
        }
        if(candidates.isEmpty()) candidates.addAll(apps);
        String[] names=new String[candidates.size()+1];
        names[0]="Сбросить выбор VPN";
        for(int i=0;i<candidates.size();i++) names[i+1]=candidates.get(i).label;
        new AlertDialog.Builder(this).setTitle("Выбрать VPN-приложение")
            .setItems(names,(d,which)->{
                if(which==0){getSharedPreferences(PREFS,0).edit().remove(VPN_PACKAGE).apply();updateStatuses();return;}
                AppEntry chosen=candidates.get(which-1);
                getSharedPreferences(PREFS,0).edit().putString(VPN_PACKAGE,chosen.packageName).apply();
                launchIntent(chosen.launchIntent,chosen.label); updateStatuses();
            }).setNegativeButton("Отмена",null).show();
    }

    private void launchPackage(String pkg,String title,TextView status) {
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){Toast.makeText(this,title+": не установлено",Toast.LENGTH_LONG).show(); if(status!=null)status.setText(title+": не установлено");return;}
        launchIntent(i,title);
    }

    private void launchIntent(Intent i,String title) {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        catch(Exception e){Toast.makeText(this,"Не удалось открыть "+title,Toast.LENGTH_LONG).show();}
    }

    private void updateStatuses() {
        if(dpiStatus!=null) dpiStatus.setText(getPackageManager().getLaunchIntentForPackage(BuildConfig.DPI_PACKAGE)!=null
                ? "● DPI: приложение найдено" : "○ DPI: установите "+BuildConfig.DPI_TITLE);
        if(vpnStatus!=null) {
            String pkg=getSharedPreferences(PREFS,0).getString(VPN_PACKAGE,null);
            vpnStatus.setText(pkg==null?"VPN: не выбран":"VPN: "+appName(pkg));
        }
    }

    private String appName(String pkg) {
        try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(Exception e){return "не установлен";}
    }

    private void focusStyle(View v) {
        v.setOnFocusChangeListener((view,focused)->view.setBackground(panel(
            focused?Color.rgb(46,76,145):PANEL,dp(16),
            focused?Color.rgb(90,160,255):Color.rgb(43,65,112),dp(focused?3:1))));
    }

    private GradientDrawable panel(int color,int radius,int stroke,int width) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(width,stroke); return d;
    }
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}