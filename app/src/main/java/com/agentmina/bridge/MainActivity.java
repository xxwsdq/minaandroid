
package com.agentmina.bridge;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private final ArrayList<String> appNames = new ArrayList<>();
    private final ArrayList<String> packageNames = new ArrayList<>();
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 30, 24, 20);

        TextView title = new TextView(this);
        title.setText("Agent Mina");
        title.setTextSize(28);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Ищу приложения...");
        status.setTextSize(16);
        layout.addView(status);

        ListView list = new ListView(this);
        layout.addView(list);

        setContentView(layout);

        loadApps();

        list.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                appNames
        ));

        list.setOnItemClickListener((parent, view, position, id) -> {
            openApp(packageNames.get(position));
        });

        handleIntent(getIntent());
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> results =
                pm.queryIntentActivities(query, 0);

        for (ResolveInfo info : results) {
            String name = info.loadLabel(pm).toString();
            String pkg = info.activityInfo.packageName;

            if (!packageNames.contains(pkg)) {
                appNames.add(name);
                packageNames.add(pkg);
            }
        }

        ArrayList<Integer> order = new ArrayList<>();
        for (int i = 0; i < appNames.size(); i++) order.add(i);

        Collections.sort(order, Comparator.comparing(
                i -> appNames.get(i).toLowerCase()
        ));

        ArrayList<String> sortedNames = new ArrayList<>();
        ArrayList<String> sortedPackages = new ArrayList<>();

        for (int i : order) {
            sortedNames.add(appNames.get(i));
            sortedPackages.add(packageNames.get(i));
        }

        appNames.clear();
        appNames.addAll(sortedNames);

        packageNames.clear();
        packageNames.addAll(sortedPackages);

        status.setText("Найдено приложений: " + appNames.size());
    }

    private void openApp(String packageName) {
        Intent launch =
                getPackageManager().getLaunchIntentForPackage(packageName);

        if (launch == null) {
            Toast.makeText(this, "Не удалось открыть приложение",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(launch);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        Uri uri = intent.getData();

        if (!"mina".equals(uri.getScheme())
                || !"launch".equals(uri.getHost())) return;

        String pkg = uri.getQueryParameter("package");

        if (pkg == null || !packageNames.contains(pkg)) {
            Toast.makeText(this, "Приложение не найдено",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        openApp(pkg);
    }
}
