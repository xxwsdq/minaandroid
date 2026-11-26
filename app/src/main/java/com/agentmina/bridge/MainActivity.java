
package com.agentmina.bridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {

    private TextView statusText, screenText;

    private final int BG = Color.rgb(15, 11, 24);
    private final int CARD = Color.rgb(29, 21, 43);
    private final int PURPLE = Color.rgb(170, 105, 255);
    private final int WHITE = Color.rgb(245, 239, 255);
    private final int MUTED = Color.rgb(175, 157, 195);

    private static final String PREFS = "mina_accessibility";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(22, 28, 22, 24);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(1);

        TextView title = new TextView(this);
        title.setText("✦ MINA");
        title.setTextSize(32);
        title.setTextColor(PURPLE);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ANDROID BRIDGE • v0.2");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 4, 0, 24);
        content.addView(subtitle);

        LinearLayout statusCard = card();

        TextView statusTitle = new TextView(this);
        statusTitle.setText("СОСТОЯНИЕ МИНЫ");
        statusTitle.setTextColor(MUTED);
        statusCard.addView(statusTitle);

        statusText = new TextView(this);
        statusText.setTextSize(17);
        statusText.setTextColor(WHITE);
        statusText.setPadding(0, 12, 0, 4);
        statusCard.addView(statusText);

        content.addView(statusCard);

        addSection(content, "УПРАВЛЕНИЕ");

        addButton(content, "◈ Включить Accessibility Service",
            () -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            ));

        addButton(content, "▣ Открыть ibis Paint X", () -> {
            Intent intent = getPackageManager()
                .getLaunchIntentForPackage("com.ibispaintx.app");

            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Приложение не найдено",
                    Toast.LENGTH_LONG).show();
            }
        });

        addButton(content, "↻ Обновить информацию",
            this::refreshScreenInfo);

        addSection(content, "ЧТО МИНА ВИДИТ");

        LinearLayout infoCard = card();

        screenText = new TextView(this);
        screenText.setTextColor(WHITE);
        screenText.setTextSize(14);
        infoCard.addView(screenText);

        content.addView(infoCard);

        TextView footer = new TextView(this);
        footer.setText("MINA AI • MADE FOR ANDROID");
        footer.setTextColor(MUTED);
        footer.setGravity(Gravity.CENTER);
        footer.setTextSize(10);
        footer.setPadding(0, 28, 0, 10);
        content.addView(footer);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);

        refreshScreenInfo();
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setPadding(18, 18, 18, 18);
        box.setBackground(round(CARD, 22));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = 16;
        box.setLayoutParams(params);

        return box;
    }

    private void addSection(LinearLayout parent, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(PURPLE);
        label.setTextSize(12);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(4, 18, 4, 12);
        parent.addView(label);
    }

    private void addButton(
        LinearLayout parent, String text, Runnable action
    ) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setBackground(round(
            Color.rgb(55, 35, 80), 18
        ));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = 10;

        parent.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(
            radius * getResources().getDisplayMetrics().density
        );
        drawable.setStroke(1, Color.rgb(91, 61, 125));
        return drawable;
    }

    private void refreshScreenInfo() {
        if (statusText == null || screenText == null) return;

        statusText.setText(
            MinaAccessibilityService.isEnabled(this)
                ? "● Служба включена"
                : "○ Служба выключена"
        );

        SharedPreferences prefs =
            getSharedPreferences(PREFS, MODE_PRIVATE);

        String app = prefs.getString(
            "last_package", "Пока нет данных"
        );

        String labels = prefs.getString(
            "last_labels", "Открой ibis Paint X."
        );

        screenText.setText(
            "Приложение:\n" + app +
            "\n\nНайденные элементы:\n" + labels
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshScreenInfo();
    }
}
