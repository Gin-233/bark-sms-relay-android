package com.local.smsrelay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SystemAccessActivity extends Activity {
    private static final int PERMISSION_REQUEST = 101;

    private TextView accessTitle;
    private TextView accessSubtitle;
    private LinearLayout permissionList;
    private TextView permissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelayUi.applyWindow(this);
        setContentView(RelayUi.withSystemBars(buildUi()));
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionList != null) {
            refreshStatus();
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RelayUi.BACKGROUND);
        root.addView(RelayUi.toolbar(this, "系统访问", "权限与后台可靠性"),
                RelayUi.matchWrap());

        LinearLayout page = RelayUi.page(this, 6, 34);
        page.addView(buildOverview(), RelayUi.matchWrap());
        page.addView(RelayUi.sectionLabel(this, "必要权限"));
        page.addView(buildPermissionCard(), RelayUi.matchWrap());
        page.addView(RelayUi.sectionLabel(this, "后台与自启动"));
        page.addView(buildBackgroundCard(), RelayUi.matchWrap());

        TextView note = RelayUi.text(this,
                "应用只申请接收和读取短信、显示通知及联网所需的最小权限；不会申请联系人、定位或移动数据控制权限。",
                11.5f, RelayUi.TEXT_MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(RelayUi.dp(this, 8), RelayUi.dp(this, 20),
                RelayUi.dp(this, 8), 0);
        page.addView(note);
        root.addView(RelayUi.scroll(this, page), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private View buildOverview() {
        LinearLayout card = RelayUi.darkCard(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = RelayUi.icon(this, R.drawable.ic_shield, 24, RelayUi.CYAN);
        row.addView(icon, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 30), RelayUi.dp(this, 30)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 14), 0, 0, 0);
        accessTitle = RelayUi.text(this, "系统访问完整", 18, Color.WHITE, true);
        labels.addView(accessTitle);
        accessSubtitle = RelayUi.text(this, "短信接收与通知均已授权", 12.5f,
                Color.rgb(194, 207, 235), false);
        accessSubtitle.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        labels.addView(accessSubtitle);
        row.addView(labels, RelayUi.weighted());
        card.addView(row, RelayUi.matchWrap());
        return card;
    }

    private View buildPermissionCard() {
        LinearLayout card = RelayUi.card(this);
        card.addView(RelayUi.text(this, "权限检查", 15, RelayUi.TEXT, true));
        TextView caption = RelayUi.text(this,
                "缺少任一短信权限都会降低接收可靠性。",
                12, RelayUi.TEXT_SECONDARY, false);
        caption.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        card.addView(caption);

        permissionList = new LinearLayout(this);
        permissionList.setOrientation(LinearLayout.VERTICAL);
        permissionList.setPadding(0, RelayUi.dp(this, 14), 0, 0);
        card.addView(permissionList, RelayUi.matchWrap());

        permissionButton = RelayUi.button(this, "授予短信读取和通知权限", true,
                v -> requestRelayPermissions());
        card.addView(permissionButton, RelayUi.matchWrapTop(this, 16));
        return card;
    }

    private View buildBackgroundCard() {
        LinearLayout card = RelayUi.card(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(RelayUi.iconTile(this, R.drawable.ic_power,
                RelayUi.PRIMARY, Color.rgb(235, 241, 255), 44),
                new LinearLayout.LayoutParams(RelayUi.dp(this, 44), RelayUi.dp(this, 44)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 13), 0, 0, 0);
        labels.addView(RelayUi.text(this, "保持后台运行", 16, RelayUi.TEXT, true));
        TextView sub = RelayUi.text(this, "适合长期插电、常连 Wi-Fi 放置",
                12, RelayUi.TEXT_SECONDARY, false);
        sub.setPadding(0, RelayUi.dp(this, 2), 0, 0);
        labels.addView(sub);
        header.addView(labels, RelayUi.weighted());
        card.addView(header, RelayUi.matchWrap());

        TextView guidance = RelayUi.text(this,
                "请在系统设置中允许后台运行。部分厂商还需要开启自启动，并将电池优化设为“不限制”。",
                12.5f, RelayUi.TEXT_SECONDARY, false);
        guidance.setPadding(RelayUi.dp(this, 2), RelayUi.dp(this, 16),
                RelayUi.dp(this, 2), 0);
        card.addView(guidance);
        card.addView(RelayUi.button(this, "打开应用系统设置", false,
                v -> openAppSettings()), RelayUi.matchWrapTop(this, 16));
        return card;
    }

    private void refreshStatus() {
        boolean receive = granted(Manifest.permission.RECEIVE_SMS);
        boolean read = granted(Manifest.permission.READ_SMS);
        boolean notifications = Build.VERSION.SDK_INT < 33
                || granted(Manifest.permission.POST_NOTIFICATIONS);
        boolean all = receive && read && notifications;

        accessTitle.setText(all ? "系统访问完整" : "需要完成授权");
        accessSubtitle.setText(all
                ? "短信接收与通知均已授权"
                : "请处理下方标记为待授权的项目");
        renderPermissionRows(receive, read, notifications);
        permissionButton.setText(all ? "权限已经齐全" : "授予短信读取和通知权限");
        RelayUi.setButtonEnabled(permissionButton, !all);
    }

    private void renderPermissionRows(boolean receive, boolean read, boolean notifications) {
        permissionList.removeAllViews();
        addPermissionRow("接收新短信", "监听系统 SMS_RECEIVED 广播", receive);
        addPermissionDivider();
        addPermissionRow("读取短信收件箱", "解锁后补齐可能遗漏的消息", read);
        if (Build.VERSION.SDK_INT >= 33) {
            addPermissionDivider();
            addPermissionRow("显示状态通知", "用于后台状态与系统提示", notifications);
        }
    }

    private void addPermissionRow(String title, String subtitle, boolean granted) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, RelayUi.dp(this, 8), 0, RelayUi.dp(this, 8));
        ImageView icon = RelayUi.icon(this,
                granted ? R.drawable.ic_check : R.drawable.ic_alert,
                18, granted ? RelayUi.SUCCESS : RelayUi.WARNING);
        icon.setBackground(RelayUi.rounded(
                granted ? Color.rgb(233, 249, 241) : Color.rgb(255, 247, 231),
                12, Color.TRANSPARENT, 0, this));
        icon.setPadding(RelayUi.dp(this, 7), RelayUi.dp(this, 7),
                RelayUi.dp(this, 7), RelayUi.dp(this, 7));
        row.addView(icon, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 34), RelayUi.dp(this, 34)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 12), 0, RelayUi.dp(this, 8), 0);
        labels.addView(RelayUi.text(this, title, 14, RelayUi.TEXT, true));
        TextView sub = RelayUi.text(this, subtitle, 11.5f,
                RelayUi.TEXT_SECONDARY, false);
        sub.setPadding(0, RelayUi.dp(this, 2), 0, 0);
        labels.addView(sub);
        row.addView(labels, RelayUi.weighted());
        row.addView(RelayUi.pill(this, granted ? "已授权" : "待授权",
                granted ? RelayUi.SUCCESS : RelayUi.WARNING,
                granted ? Color.rgb(233, 249, 241) : Color.rgb(255, 247, 231)));
        permissionList.addView(row, RelayUi.matchWrap());
    }

    private void addPermissionDivider() {
        permissionList.addView(RelayUi.divider(this), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, RelayUi.dp(this, 1)));
    }

    private boolean granted(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRelayPermissions() {
        List<String> permissions = new ArrayList<>();
        if (!granted(Manifest.permission.RECEIVE_SMS)) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if (!granted(Manifest.permission.READ_SMS)) {
            permissions.add(Manifest.permission.READ_SMS);
        }
        if (Build.VERSION.SDK_INT >= 33
                && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (permissions.isEmpty()) {
            refreshStatus();
            return;
        }
        requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            SmsImporter.importRecent(this);
            refreshStatus();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
