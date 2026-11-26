
package com.agentmina.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedHashSet;
import java.util.Set;

public class MinaAccessibilityService extends AccessibilityService {

    public static MinaAccessibilityService instance;

    private static final String PREFS = "mina_accessibility";
    private static final int MAX_LABELS = 50;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        saveScreenInfo();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        saveScreenInfo();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabled == null) return false;

        return enabled.toLowerCase().contains(
                context.getPackageName().toLowerCase()
        );
    }

    // Получить подписи элементов текущего экрана.
    public static String getScreenText() {
        if (instance == null) return "";

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return "";

        StringBuilder result = new StringBuilder();
        collectText(root, result, 0);

        return result.toString();
    }

    private static void collectText(
            AccessibilityNodeInfo node,
            StringBuilder result,
            int depth
    ) {
        if (node == null || depth > 20) return;

        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();

        if (text != null && text.length() > 0) {
            result.append(text).append("\n");
        }

        if (description != null && description.length() > 0) {
            result.append(description).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectText(node.getChild(i), result, depth + 1);
        }
    }

    // Нажать элемент с точным названием.
    public static boolean clickText(String target) {
        return clickInternal(target, false);
    }

    // Нажать элемент, название которого содержит текст.
    public static boolean clickContains(String target) {
        return clickInternal(target, true);
    }

    private static boolean clickInternal(
            String target,
            boolean partial
    ) {
        if (instance == null || target == null || target.trim().isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return false;

        return findAndClick(root, target.trim().toLowerCase(), partial, 0);
    }

    private static boolean findAndClick(
            AccessibilityNodeInfo node,
            String target,
            boolean partial,
            int depth
    ) {
        if (node == null || depth > 20) return false;

        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();

        boolean matches = matchesText(text, target, partial)
                || matchesText(description, target, partial);

        if (matches && node.isEnabled()) {
            AccessibilityNodeInfo current = node;

            while (current != null) {
                if (current.isClickable()
                        && current.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                        )) {
                    return true;
                }

                AccessibilityNodeInfo parent = current.getParent();

                if (current != node) {
                    current.recycle();
                }

                current = parent;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);

            if (findAndClick(child, target, partial, depth + 1)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesText(
            CharSequence value,
            String target,
            boolean partial
    ) {
        if (value == null) return false;

        String normalized = value.toString().trim().toLowerCase();

        if (partial) {
            return normalized.contains(target);
        }

        return normalized.equals(target);
    }

    // Ввести текст в активное поле.
    public static boolean typeText(String text) {
        if (instance == null || text == null) return false;

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return false;

        AccessibilityNodeInfo field = findEditable(root, 0);

        if (field == null) return false;

        Bundle arguments = new Bundle();
        arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
        );

        boolean success = field.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
        );

        field.recycle();
        return success;
    }

    private static AccessibilityNodeInfo findEditable(
            AccessibilityNodeInfo node,
            int depth
    ) {
        if (node == null || depth > 20) return null;

        if (node.isEditable() && node.isEnabled()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found =
                    findEditable(node.getChild(i), depth + 1);

            if (found != null) return found;
        }

        return null;
    }

    // Прокрутить экран.
    public static boolean swipe(
            float x1,
            float y1,
            float x2,
            float y2,
            long duration
    ) {
        if (instance == null) return false;

        float width = instance.getResources()
                .getDisplayMetrics().widthPixels;

        float height = instance.getResources()
                .getDisplayMetrics().heightPixels;

        // Значения от 0 до 1 считаются долями экрана.
        if (x1 >= 0 && x1 <= 1 && y1 >= 0 && y1 <= 1
                && x2 >= 0 && x2 <= 1 && y2 >= 0 && y2 <= 1) {
            x1 *= width;
            x2 *= width;
            y1 *= height;
            y2 *= height;
        }

        x1 = clamp(x1, 1, width - 1);
        x2 = clamp(x2, 1, width - 1);
        y1 = clamp(y1, 1, height - 1);
        y2 = clamp(y2, 1, height - 1);

        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        path,
                                        0,
                                        Math.max(250, Math.min(duration, 1500))
                                )
                        )
                        .build();

        return instance.dispatchGesture(gesture, null, null);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    // Вернуться на предыдущий экран.
    public static boolean pressBack() {
        return instance != null && instance.performGlobalAction(
                GLOBAL_ACTION_BACK
        );
    }

    // Перейти на главный экран.
    public static boolean pressHome() {
        return instance != null && instance.performGlobalAction(
                GLOBAL_ACTION_HOME
        );
    }

    // Сохранить информацию о текущем экране.
    private void saveScreenInfo() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            Set<String> labels = new LinkedHashSet<>();
            collectLabels(root, labels, 0);

            StringBuilder result = new StringBuilder();

            int count = 0;
            for (String label : labels) {
                if (count++ >= MAX_LABELS) break;
                if (result.length() > 0) result.append("\n");
                result.append(label);
            }

            SharedPreferences prefs = getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            );

            prefs.edit()
                    .putString(
                            "last_package",
                            String.valueOf(root.getPackageName())
                    )
                    .putString("last_labels", result.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private void collectLabels(
            AccessibilityNodeInfo node,
            Set<String> labels,
            int depth
    ) {
        if (node == null || depth > 20) return;

        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();

        if (text != null && text.length() > 0) {
            labels.add(text.toString().trim());
        }

        if (description != null && description.length() > 0) {
            labels.add(description.toString().trim());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectLabels(node.getChild(i), labels, depth + 1);
        }
    }
            }
