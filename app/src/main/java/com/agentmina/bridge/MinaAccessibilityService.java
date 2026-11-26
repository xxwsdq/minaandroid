
package com.agentmina.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Context;
import android.provider.Settings;

import java.util.LinkedHashSet;
import java.util.Set;

public class MinaAccessibilityService extends AccessibilityService {

    private static final String PREFS = "mina_accessibility";
    private static MinaAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();
        Set<String> labels = new LinkedHashSet<>();

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root != null) {
            collectLabels(root, labels, 0);
            root.recycle();
        }

        StringBuilder result = new StringBuilder();
        int count = 0;

        for (String label : labels) {
            if (count++ >= 35) break;
            if (result.length() > 0) result.append("\n");
            result.append("• ").append(label);
        }

        if (result.length() == 0) {
            result.append("Видимые подписи не найдены.");
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString("last_package", packageName)
            .putString("last_labels", result.toString())
            .apply();
    }

    private void collectLabels(
        AccessibilityNodeInfo node,
        Set<String> output,
        int depth
    ) {
        if (node == null || depth > 12 || output.size() >= 35) return;

        addLabel(output, node.getText());
        addLabel(output, node.getContentDescription());

        for (int i = 0; i < node.getChildCount(); i++) {
            if (output.size() >= 35) break;

            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {
                collectLabels(child, output, depth + 1);
                child.recycle();
            }
        }
    }

    private void addLabel(Set<String> output, CharSequence value) {
        if (value == null) return;

        String text = value.toString().trim();

        if (!text.isEmpty() && text.length() <= 100) {
            output.add(text);
        }
    }

    // Нажатие на элемент по названию
    public static boolean clickText(String text) {
        if (instance == null || text == null) return false;

        AccessibilityNodeInfo root =
            instance.getRootInActiveWindow();

        if (root == null) return false;

        boolean result = instance.findAndClick(root, text);
        root.recycle();

        return result;
    }

    private boolean findAndClick(
        AccessibilityNodeInfo node,
        String text
    ) {
        if (node == null) return false;

        CharSequence label = node.getText();
        CharSequence description = node.getContentDescription();

        boolean matches =
            (label != null && label.toString().equalsIgnoreCase(text)) ||
            (description != null &&
             description.toString().equalsIgnoreCase(text));

        if (matches) {
            if (node.isClickable() &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }

            AccessibilityNodeInfo parent = node.getParent();

            if (parent != null) {
                boolean clicked = parent.isClickable() &&
                    parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    );
                parent.recycle();

                if (clicked) return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {
                boolean result = findAndClick(child, text);
                child.recycle();

                if (result) return true;
            }
        }

        return false;
    }

    // Адаптивный свайп по экрану
    public static boolean swipe(
        float x1, float y1,
        float x2, float y2,
        long duration
    ) {
        if (instance == null) return false;

        android.util.DisplayMetrics metrics =
            instance.getResources().getDisplayMetrics();

        float startX = metrics.widthPixels / 2f;
        float startY = metrics.heightPixels * 0.30f;
        float endX = metrics.widthPixels / 2f;
        float endY = metrics.heightPixels * 0.75f;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        GestureDescription.Builder builder =
            new GestureDescription.Builder();

        builder.addStroke(
            new GestureDescription.StrokeDescription(
                path, 0, Math.max(300, duration)
            )
        );

        return instance.dispatchGesture(
            builder.build(), null, null
        );
    }

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabled == null) return false;

        String expected = context.getPackageName() + "/" +
            MinaAccessibilityService.class.getName();

        return enabled.toLowerCase().contains(
            expected.toLowerCase()
        );
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
}
