
package com.agentmina.bridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {

    private TextView statusText, screenText, resultText;

    private final int BG = Color.rgb(15, 11, 24);
    private final int CARD = Color.rgb(29, 21, 43);
    private final int PURPLE = Color.rgb(170, 105, 255);
    private final int WHITE = Color.rgb(245, 239, 255);
    private final int MUTED = Color.rgb(175, 157, 195);

    private static final String PREFS = "mina_accessibility";
    private static final String IBIS_PACKAGE =
        "jp.ne.ibis.ibispaintx.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
        handleCommand(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCommand(intent);
    }

    // Принимаем только заранее разрешённые команды
    private void handleCommand(Intent intent) {
        if (intent == null ||
            !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return;
        }

        Uri uri = intent.getData();

        if (uri == null ||
            !"mina".equals(uri.getScheme()) ||
            !"launch".equals(uri.getHost())) {
            return;
        }

        String action = uri.getQueryParameter("action");

        if ("open_ibis".equals(action)) {
            new AlertDialog.Builder(this)
                .setTitle("Команда от Mina")
                .setMessage("Открыть ibis Paint X?")
                .setNegativeButton("Отмена", (d, w) -> {
                    showResult("Команда отменена");
                })
                .setPositiveButton("Открыть", (d, w) -> {
                    openIbis();
                })
                .show();
        } else {
            showResult("Неизвестная команда");
        }
    }

    private void openIbis() {
        Intent intent = getPackageManager()
            .getLaunchIntentForPackage(IBIS_PACKAGE);

        if (intent != null) {
            startActivity(intent);
            showResult("ibis Paint X запущен");
        } else {
            showResult("ibis Paint X не найден");
        }
    }

    private void buildInterface() {
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
        subtitle.setText("ANDROID BRIDGE • v0.4");
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

        addButton(content, "▣ Открыть ibis Paint X",
            this::openIbis);

        addButton(content, "↻ Обновить информацию",
            this::refreshScreenInfo);

        addSection(content, "ТЕСТ УПРАВЛЕНИЯ");

        EditText commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setHint("Название кнопки на экране");
        commandInput.setHintTextColor(MUTED);
        commandInput.setTextColor(WHITE);
        commandInput.setPadding(14, 12, 14, 12);
        commandInput.setBackground(round(CARD, 16));
        content.addView(commandInput);

        addButton(content, "Нажать на элемент", () -> {
            String text = commandInput.getText()
                .toString().trim();

            if (text.isEmpty()) {
                showResult("Введи название элемента");
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Нажать на элемент «" + text + "»?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Нажать", (d, w) -> {
                    boolean success =
                        MinaAccessibilityService.clickText(text);

                    showResult(success
                        ? "Нажатие выполнено"
                        : "Элемент не найден или недоступен");
                })
                .show();
        });

        addButton(content, "Тестовый свайп вниз", () -> {
            new AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Выполнить тестовый свайп?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Выполнить", (d, w) -> {
                    boolean success =
                        MinaAccessibilityService.swipe(
                            500, 300, 500, 800, 500
                        );

                    showResult(success
                        ? "Свайп отправлен"
                        : "Служба недоступна");
                })
                .show();
        });

        resultText = new TextView(this);
        resultText.setTextColor(PURPLE);
        resultText.setTextSize(14);
        resultText.setPadding(4, 12, 4, 12);
        content.addView(resultText);

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

    private void showResult(String message) {
        if (resultText != null) {
            resultText.setText(message);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
