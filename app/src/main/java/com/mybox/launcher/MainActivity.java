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
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 8, 25);
    private static final int PANEL = Color.rgb(19, 28, 57);
    private static final int TEXT = Color.rgb(247, 249, 255);
    private static final int MUTED = Color.rgb(165, 180, 215);
    private static final String PREFS = "mybox";
    private static final String VPN_PACKAGE = "vpn_package";
    private static final String FAVORITES = "favorites";
    private final List<AppEntry> apps = new ArrayList<>();
    private LinearLayout root, favoriteRow, appGrid;
    private TextView clock, date, dpiStatus, vpnStatus;
    private boolean showAll = true;
    private final Handler handler = new Handler();
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clock != null) {
                Date now = new Date();
                clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                date.setText(new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now));
                handler.postDelayed(this, 30000);
            }
        }
    };

    private static class AppEntry {
        String label, packageName;
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
        loadApps();
        if (favoriteRow != null) refreshContent();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
        updateStatuses();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
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
        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
    }

    private void addResolved(Map<String, AppEntry> out, PackageManager pm, Intent query) {
        for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || out.containsKey(pkg)) continue;
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(query.hasCategory("android.intent.category.LEANBACK_LAUNCHER")
                    ? "android.intent.category.LEANBACK_LAUNCHER" : Intent.CATEGORY_LAUNCHER);
            launch.setClassName(pkg, ri.activityInfo.name);
            CharSequence label = ri.loadLabel(pm);
            out.put(pkg, new AppEntry(label == null ? pkg : label.toString(), pkg, ri.loadIcon(pm), launch));
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(38), dp(22), dp(38), dp(28));
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(7, 9, 32), Color.rgb(12, 9, 43), Color.rgb(3, 12, 35)});
        root.setBackground(backdrop);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = row();
        TextView logo = label("▣  MY BOX", 29, TEXT, true);
        header.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        TextView tagline = label("СВОБОДА В ИНТЕРНЕТЕ", 11, MUTED, true);
        header.addView(tagline);
        LinearLayout right = row();
        LinearLayout.LayoutParams rp = params(-2,-2); rp.leftMargin=dp(26); header.addView(right,rp);
        right.addView(label("◉",22,Color.rgb(80,180,255),true));
        TextView settings=label("  ⚙",25,TEXT,true); settings.setFocusable(true); settings.setContentDescription("Настройки MY BOX"); settings.setOnClickListener(v->openLauncherSettings()); focusStyle(settings); right.addView(settings);
        LinearLayout timeBox=new LinearLayout(this); timeBox.setOrientation(LinearLayout.VERTICAL); timeBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tb=params(-2,-2);tb.leftMargin=dp(22);right.addView(timeBox,tb);
        clock=label("--:--",23,TEXT,true);date=label("",11,MUTED,false);timeBox.addView(clock);timeBox.addView(date);
        root.addView(header);

        LinearLayout hero = row();
        LinearLayout.LayoutParams hp=params(-1,dp(210));hp.topMargin=dp(30);root.addView(hero,hp);
        LinearLayout dpi=heroTile("DPI","Обход блокировок","ЗАПУСТИТЬ  ▶",Color.rgb(83,34,220),Color.rgb(24,18,90));
        dpi.setOnClickListener(v->launchPackage(BuildConfig.DPI_PACKAGE,BuildConfig.DPI_TITLE));
        hero.addView(dpi,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout vpn=heroTile("VPN","Защищённое подключение","ЗАПУСТИТЬ  ▶",Color.rgb(16,105,218),Color.rgb(10,35,88));
        vpn.setOnClickListener(v->launchSelectedVpn());
        vpn.setOnLongClickListener(v->{chooseVpn();return true;});
        LinearLayout.LayoutParams h2=new LinearLayout.LayoutParams(0,-1,1);h2.leftMargin=dp(14);hero.addView(vpn,h2);
        LinearLayout youtube=appHero("YouTube","Видео и каналы","com.google.android.youtube.tv");
        LinearLayout.LayoutParams h3=new LinearLayout.LayoutParams(0,-1,1);h3.leftMargin=dp(14);hero.addView(youtube,h3);
        LinearLayout market=appHero("Play Маркет","Установка приложений","com.android.vending");
        LinearLayout.LayoutParams h4=new LinearLayout.LayoutParams(0,-1,1);h4.leftMargin=dp(14);hero.addView(market,h4);
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams h5=new LinearLayout.LayoutParams(dp(190),-1);h5.leftMargin=dp(14);hero.addView(tools,h5);
        TextView all=actionTile("▦","Все приложения  ›");all.setOnClickListener(v->{showAll=true;refreshContent();});
        tools.addView(all,new LinearLayout.LayoutParams(-1,0,1));
        TextView sys=actionTile("⚙","Настройки Android  ›");sys.setOnClickListener(v->openSystemSettings());
        LinearLayout.LayoutParams sysP=new LinearLayout.LayoutParams(-1,0,1);sysP.topMargin=dp(10);tools.addView(sys,sysP);

        LinearLayout status=row();LinearLayout.LayoutParams st=params(-1,-2);st.topMargin=dp(14);root.addView(status,st);
        dpiStatus=label("",13,MUTED,false);status.addView(dpiStatus,new LinearLayout.LayoutParams(0,-2,1));
        vpnStatus=label("",13,MUTED,false);status.addView(vpnStatus);

        sectionTitle("★  ИЗБРАННОЕ");
        favoriteRow=row();LinearLayout.LayoutParams fr=params(-1,dp(100));fr.topMargin=dp(12);root.addView(favoriteRow,fr);
        sectionTitle("ВСЕ ПРИЛОЖЕНИЯ");
        appGrid=new LinearLayout(this);appGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ag=params(-1,-2);ag.topMargin=dp(12);root.addView(appGrid,ag);
        TextView foot=label("← →  Навигация     OK  Открыть     BACK  Назад     Удерживайте OK на приложении, чтобы закрепить",11,MUTED,false);
        LinearLayout.LayoutParams fp=params(-2,-2);fp.topMargin=dp(24);root.addView(foot,fp);
        refreshContent();updateStatuses();
    }

    private void sectionTitle(String title) {
        TextView t=label(title,18,TEXT,true);
        LinearLayout.LayoutParams p=params(-1,-2);p.topMargin=dp(24);root.addView(t,p);
    }

    private LinearLayout heroTile(String title,String subtitle,String button,int accent,int deep) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(22),dp(18),dp(18),dp(18));
        box.setBackground(gradient(deep,accent,dp(18)));
        box.setFocusable(true);box.setClickable(true);
        TextView symbol=label(title.equals("DPI")?"⬡":"◈",44,Color.WHITE,true);box.addView(symbol);
        TextView name=label(title,27,TEXT,true);LinearLayout.LayoutParams np=params(-1,-2);np.topMargin=dp(2);box.addView(name,np);
        TextView sub=label(subtitle,13,Color.rgb(220,229,255),false);LinearLayout.LayoutParams sp=params(-1,-2);sp.topMargin=dp(5);box.addView(sub,sp);
        TextView action=label(button,13,TEXT,true);action.setGravity(Gravity.CENTER);action.setPadding(dp(12),dp(9),dp(12),dp(9));
        action.setBackground(panel(Color.argb(55,255,255,255),dp(20),Color.argb(80,255,255,255),dp(1)));
        LinearLayout.LayoutParams ap=params(-1,-2);ap.topMargin=dp(15);box.addView(action,ap);
        focusStyle(box);return box;
    }

    private LinearLayout appHero(String title,String subtitle,String pkg) {
        AppEntry found=findPackage(pkg);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);
        box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setBackground(gradient(Color.rgb(18,27,61),Color.rgb(42,65,128),dp(18)));
        box.setFocusable(true);box.setClickable(true);
        if(found!=null){ImageView icon=new ImageView(this);icon.setImageDrawable(found.icon);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);box.addView(icon,params(dp(66),dp(66)));}
        else box.addView(label("▶",46,Color.WHITE,true));
        TextView name=label(title,20,TEXT,true);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=params(-1,-2);np.topMargin=dp(10);box.addView(name,np);
        TextView sub=label(subtitle,12,MUTED,false);sub.setGravity(Gravity.CENTER);box.addView(sub);
        box.setOnClickListener(v->{AppEntry a=findPackage(pkg);if(a!=null)launchIntent(a.launchIntent,a.label);else Toast.makeText(this,title+" не установлено",Toast.LENGTH_SHORT).show();});
        focusStyle(box);return box;
    }

    private TextView actionTile(String symbol,String title) {
        TextView t=label(symbol+"   "+title,15,TEXT,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(16),dp(12),dp(12),dp(12));
        t.setBackground(panel(PANEL,dp(16),Color.rgb(54,75,135),dp(1)));t.setFocusable(true);focusStyle(t);return t;
    }

    private void refreshContent() {
        if(favoriteRow==null||appGrid==null)return;
        favoriteRow.removeAllViews();Set<String> fav=getFavorites();int count=0;
        for(AppEntry a:apps)if(fav.contains(a.packageName)&&count<7){addSmallApp(favoriteRow,a);count++;}
        TextView add=label("＋\nДобавить",15,TEXT,true);add.setGravity(Gravity.CENTER);add.setBackground(panel(PANEL,dp(14),Color.rgb(60,85,150),dp(1)));
        add.setFocusable(true);add.setOnClickListener(v->showAppPicker());focusStyle(add);
        LinearLayout.LayoutParams addP=new LinearLayout.LayoutParams(dp(125),-1);addP.leftMargin=dp(10);favoriteRow.addView(add,addP);
        appGrid.removeAllViews();
        int columns=getResources().getConfiguration().screenWidthDp>=900?6:4;
        LinearLayout line=null;
        for(int i=0;i<apps.size();i++){
            if(i%columns==0){line=row();LinearLayout.LayoutParams lp=params(-1,dp(104));if(i>0)lp.topMargin=dp(10);appGrid.addView(line,lp);}
            LinearLayout card=smallAppCard(apps.get(i));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(i%columns!=0)cp.leftMargin=dp(10);line.addView(card,cp);
        }
    }

    private void addSmallApp(LinearLayout row,AppEntry app) {
        LinearLayout card=smallAppCard(app);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(145),-1);p.rightMargin=dp(10);row.addView(card,p);
    }

    private LinearLayout smallAppCard(AppEntry app) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8),dp(6),dp(8),dp(6));card.setBackground(panel(PANEL,dp(14),Color.rgb(45,64,112),dp(1)));
        card.setFocusable(true);card.setClickable(true);
        ImageView icon=new ImageView(this);icon.setImageDrawable(app.icon);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(icon,params(dp(43),dp(43)));
        TextView name=label(app.label,12,TEXT,true);name.setGravity(Gravity.CENTER);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np=params(-1,-2);np.topMargin=dp(5);card.addView(name,np);
        card.setOnClickListener(v->launchIntent(app.launchIntent,app.label));
        card.setOnLongClickListener(v->{toggleFavorite(app);return true;});
        focusStyle(card);return card;
    }

    private void showAppPicker() {
        String[] names=new String[apps.size()];for(int i=0;i<apps.size();i++)names[i]=apps.get(i).label;
        new AlertDialog.Builder(this).setTitle("Добавить в избранное").setItems(names,(d,w)->toggleFavorite(apps.get(w))).setNegativeButton("Закрыть",null).show();
    }

    private void toggleFavorite(AppEntry app) {
        Set<String> fav=getFavorites();if(fav.contains(app.packageName))fav.remove(app.packageName);else fav.add(app.packageName);
        getSharedPreferences(PREFS,0).edit().putStringSet(FAVORITES,fav).apply();refreshContent();
        Toast.makeText(this,fav.contains(app.packageName)?"Добавлено в избранное":"Убрано из избранного",Toast.LENGTH_SHORT).show();
    }

    private Set<String> getFavorites() {
        Set<String> stored=getSharedPreferences(PREFS,0).getStringSet(FAVORITES,new HashSet<>());
        return new HashSet<>(stored);
    }

    private AppEntry findPackage(String pkg){for(AppEntry a:apps)if(a.packageName.equals(pkg))return a;return null;}

    private void chooseVpn() {
        // Не фильтруем по словам VPN: клиенты вроде HAP/Happ могут называться иначе.
        // Показываем все установленные приложения, у которых есть Activity запуска.
        List<AppEntry> candidates=new ArrayList<>();
        PackageManager pm=getPackageManager();
        for(android.content.pm.ApplicationInfo info:pm.getInstalledApplications(0)){
            String pkg=info.packageName;
            if(pkg.equals(getPackageName())) continue;
            Intent launch=pm.getLaunchIntentForPackage(pkg);
            if(launch==null) continue;
            String title=pm.getApplicationLabel(info).toString();
            try { candidates.add(new AppEntry(title,pkg,pm.getApplicationIcon(info),launch)); }
            catch(Exception ignored) { }
        }
        Collections.sort(candidates,Comparator.comparing(a->a.label.toLowerCase(Locale.ROOT)));
        String[] names=new String[candidates.size()+1];names[0]="Сбросить выбор";
        for(int i=0;i<candidates.size();i++)names[i+1]=candidates.get(i).label+"  ·  "+candidates.get(i).packageName;
        new AlertDialog.Builder(this).setTitle("Выбрать VPN-клиент или HAP").setItems(names,(d,w)->{
            if(w==0)getSharedPreferences(PREFS,0).edit().remove(VPN_PACKAGE).apply();
            else getSharedPreferences(PREFS,0).edit().putString(VPN_PACKAGE,candidates.get(w-1).packageName).apply();
            updateStatuses();
        }).setNegativeButton("Отмена",null).show();
    }

    private void launchSelectedVpn() {
        String pkg=getSharedPreferences(PREFS,0).getString(VPN_PACKAGE,null);
        if(pkg==null){chooseVpn();return;}launchPackage(pkg,appName(pkg));
    }

    private void launchPackage(String pkg,String title) {
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){Toast.makeText(this,title+": не установлено",Toast.LENGTH_LONG).show();return;}launchIntent(i,title);
    }

    private void launchIntent(Intent i,String title) {
        try{startActivity(i);}
        catch(Exception e){Toast.makeText(this,"Не удалось открыть "+title,Toast.LENGTH_LONG).show();}
    }

    private void updateStatuses() {
        if(dpiStatus!=null)dpiStatus.setText(getPackageManager().getLaunchIntentForPackage(BuildConfig.DPI_PACKAGE)!=null
                ?"●  DPI: приложение доступно":"○  DPI: установите "+BuildConfig.DPI_TITLE);
        if(vpnStatus!=null){String pkg=getSharedPreferences(PREFS,0).getString(VPN_PACKAGE,null);
            vpnStatus.setText(pkg==null?"VPN: клиент не выбран":"VPN: "+appName(pkg));}
    }

    private String appName(String pkg) {
        try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(Exception e){return "не установлен";}
    }

    private void openLauncherSettings() {
        String[] options={"Выбрать VPN-клиент (все приложения)","Управление избранным","Обновить список приложений","Назначить MY BOX главным экраном"};
        new AlertDialog.Builder(this).setTitle("Настройки MY BOX").setItems(options,(dialog,which)->{
            if(which==0) chooseVpn();
            else if(which==1) showAppPicker();
            else if(which==2){loadApps();refreshContent();updateStatuses();Toast.makeText(this,"Список приложений обновлён",Toast.LENGTH_SHORT).show();}
            else if(which==3) openHomeSettings();
        }).setNegativeButton("Закрыть",null).show();
    }

    private void openSystemSettings() {
        try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}
        catch(Exception e){Toast.makeText(this,"Системные настройки недоступны",Toast.LENGTH_SHORT).show();}
    }

    private void openHomeSettings() {
        try{startActivity(new Intent("android.settings.HOME_SETTINGS"));}
        catch(Exception e){Toast.makeText(this,"Выберите MY BOX в системных настройках главного экрана",Toast.LENGTH_LONG).show();}
    }

    private void focusStyle(View v) {
        v.setOnFocusChangeListener((view,focused)->view.setBackground(panel(
                focused?Color.rgb(42,52,115):PANEL,dp(16),
                focused?Color.rgb(139,91,255):Color.rgb(45,64,112),dp(focused?3:1))));
    }

    private GradientDrawable gradient(int start,int end,int radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
        d.setCornerRadius(radius);d.setStroke(dp(1),Color.argb(100,160,175,255));return d;
    }
    private GradientDrawable panel(int color,int radius,int stroke,int width) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(width,stroke);return d;
    }
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
