
package com.agentmina.bridge;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.util.LinkedHashSet;
import java.util.Set;

public class MinaAccessibilityService extends AccessibilityService {

    private static final String PREFS = "mina_accessibility";

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

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append("• ").append(label);
        }

        if (result.length() == 0) {
            result.append("Видимые подписи не найдены.");
        }

        SharedPreferences prefs =
            getSharedPreferences(PREFS, MODE_PRIVATE);

        prefs.edit()
            .putString("last_package", packageName)
            .putString("last_labels", result.toString())
            .apply();
    }

    private void collectLabels(
        AccessibilityNodeInfo node,
        Set<String> output,
        int depth
    ) {
        if (node == null || depth > 12 || output.size() >= 35) {
            return;
        }

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
}
