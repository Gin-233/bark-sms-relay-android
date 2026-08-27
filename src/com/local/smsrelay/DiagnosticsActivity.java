package com.local.smsrelay;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Network;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public final class DiagnosticsActivity extends Activity {
    private Config config;
    private TextView barkTestButton;
    private TextView barkPending;
    private TextView completed;
    private TextView networkStatus;
    private LinearLayout recentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelayUi.applyWindow(this);
        Config.ensureInitialized(this);
        config = new Config(this);
        setContentView(RelayUi.withSystemBars(buildUi()));
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recentList != null) {
            refreshStatus();
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RelayUi.BACKGROUND);
        root.addView(RelayUi.toolbar(this, "测试与诊断", "不显示短信正文或完整号码"),
                RelayUi.matchWrap());

        LinearLayout page = RelayUi.page(this, 6, 34);
        page.addView(buildOverview(), RelayUi.matchWrap());
        page.addView(RelayUi.sectionLabel(this, "安全测试"));
        page.addView(buildBarkTestCard(), RelayUi.matchWrap());
        page.addView(RelayUi.sectionLabel(this, "加密队列"));
        page.addView(buildQueueCard(), RelayUi.matchWrap());
        page.addView(buildRecentCard(), RelayUi.matchWrapTop(this, 10));

        TextView note = RelayUi.text(this,
                "测试只会在已验证的 Wi-Fi 上发送，并且不会写入短信队列。",
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
        ImageView icon = RelayUi.icon(this, R.drawable.ic_diagnostics, 24, RelayUi.CYAN);
        row.addView(icon, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 30), RelayUi.dp(this, 30)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 14), 0, 0, 0);
        labels.addView(RelayUi.text(this, "链路诊断中心", 18, Color.WHITE, true));
        networkStatus = RelayUi.text(this, "正在检查 Wi-Fi…", 12.5f,
                Color.rgb(194, 207, 235), false);
        networkStatus.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        labels.addView(networkStatus);
        row.addView(labels, RelayUi.weighted());
        card.addView(row, RelayUi.matchWrap());
        return card;
    }

    private View buildBarkTestCard() {
        LinearLayout card = RelayUi.card(this);
        card.addView(testHeader(R.drawable.ic_bell, "Bark 推送",
                "发送一条加密测试推送", "安全测试"), RelayUi.matchWrap());
        barkTestButton = RelayUi.button(this, "发送一条 Bark 测试", true,
                v -> confirmBarkTest());
        card.addView(barkTestButton, RelayUi.matchWrapTop(this, 16));
        return card;
    }

    private LinearLayout testHeader(int iconResource, String title,
                                    String subtitle, String badge) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(RelayUi.iconTile(this, iconResource, RelayUi.PRIMARY,
                Color.rgb(235, 241, 255), 44),
                new LinearLayout.LayoutParams(RelayUi.dp(this, 44), RelayUi.dp(this, 44)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 13), 0, RelayUi.dp(this, 8), 0);
        labels.addView(RelayUi.text(this, title, 16, RelayUi.TEXT, true));
        TextView sub = RelayUi.text(this, subtitle, 12, RelayUi.TEXT_SECONDARY, false);
        sub.setPadding(0, RelayUi.dp(this, 2), 0, 0);
        labels.addView(sub);
        row.addView(labels, RelayUi.weighted());
        row.addView(RelayUi.pill(this, badge, RelayUi.PRIMARY, Color.rgb(237, 243, 255)));
        return row;
    }

    private View buildQueueCard() {
        LinearLayout card = RelayUi.card(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(RelayUi.text(this, "投递状态", 15, RelayUi.TEXT, true),
                RelayUi.weighted());
        TextView refresh = RelayUi.text(this, "刷新", 12.5f, RelayUi.PRIMARY, true);
        refresh.setPadding(RelayUi.dp(this, 10), RelayUi.dp(this, 6),
                RelayUi.dp(this, 2), RelayUi.dp(this, 6));
        refresh.setOnClickListener(v -> refreshStatus());
        header.addView(refresh);
        card.addView(header, RelayUi.matchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, RelayUi.dp(this, 18), 0, RelayUi.dp(this, 2));
        barkPending = addQueueMetric(stats, "Bark 待发", "0", RelayUi.PRIMARY);
        completed = addQueueMetric(stats, "已归档", "0", RelayUi.SUCCESS);
        card.addView(stats, RelayUi.matchWrap());
        return card;
    }

    private TextView addQueueMetric(LinearLayout parent, String label,
                                    String initialValue, int color) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setGravity(Gravity.CENTER);
        metric.setPadding(RelayUi.dp(this, 4), RelayUi.dp(this, 13),
                RelayUi.dp(this, 4), RelayUi.dp(this, 13));
        metric.setBackground(RelayUi.rounded(Color.rgb(248, 250, 253),
                16, RelayUi.BORDER, 1, this));
        TextView value = RelayUi.text(this, initialValue, 22, color, true);
        value.setGravity(Gravity.CENTER);
        metric.addView(value);
        TextView caption = RelayUi.text(this, label, 11, RelayUi.TEXT_SECONDARY, false);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        metric.addView(caption);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (parent.getChildCount() > 0) {
            params.setMargins(RelayUi.dp(this, 8), 0, 0, 0);
        }
        parent.addView(metric, params);
        return value;
    }

    private View buildRecentCard() {
        LinearLayout card = RelayUi.card(this);
        card.addView(RelayUi.text(this, "最近记录", 15, RelayUi.TEXT, true));
        recentList = new LinearLayout(this);
        recentList.setOrientation(LinearLayout.VERTICAL);
        recentList.setPadding(0, RelayUi.dp(this, 10), 0, 0);
        card.addView(recentList, RelayUi.matchWrap());
        return card;
    }

    private void refreshStatus() {
        SmsStore store = SmsStore.get(this);
        barkPending.setText(Integer.toString(store.pendingCount()));
        completed.setText(Integer.toString(store.completedCount()));

        Network wifi = validatedWifiNetwork();
        networkStatus.setText(wifi == null
                ? "当前未检测到已验证的 Wi-Fi"
                : "Wi-Fi 已验证 · 移动数据不会用于投递");
        RelayUi.setButtonEnabled(barkTestButton, config.barkReady() && wifi != null);
        renderRecent(store.recentStatus(8));
    }

    private void renderRecent(List<String> lines) {
        recentList.removeAllViews();
        if (lines.isEmpty()) {
            recentList.addView(RelayUi.text(this, "暂无队列记录",
                    12.5f, RelayUi.TEXT_SECONDARY, false));
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                View divider = RelayUi.divider(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, RelayUi.dp(this, 1));
                params.setMargins(0, RelayUi.dp(this, 10), 0, RelayUi.dp(this, 10));
                recentList.addView(divider, params);
            }
            TextView line = RelayUi.text(this, lines.get(i), 12,
                    RelayUi.TEXT_SECONDARY, false);
            recentList.addView(line, RelayUi.matchWrap());
        }
    }

    private void confirmBarkTest() {
        new AlertDialog.Builder(this)
                .setTitle("测试 Bark？")
                .setMessage("这次发送一条加密 Bark 推送，不写入短信队列。")
                .setNegativeButton("取消", null)
                .setPositiveButton("发送测试", (dialog, which) -> sendBarkTest())
                .show();
    }

    private void sendBarkTest() {
        if (!config.barkReady()) {
            showMessage("无法测试 Bark", "Bark 尚未完整配置，请先保存 Device Key。");
            return;
        }
        Network wifi = validatedWifiNetwork();
        if (wifi == null) {
            showMessage("无法测试 Bark", "当前没有已验证的 Wi-Fi；为防止使用移动数据，本次没有发送。");
            return;
        }

        RelayUi.setButtonEnabled(barkTestButton, false);
        barkTestButton.setText("正在通过 Wi-Fi 测试 Bark…");
        new Thread(() -> {
            try {
                new BarkClient(config).sendOnNetwork(
                        wifi,
                        "短信安全转发 · Bark 测试",
                        "这是一条通过 Bark 发送的加密测试消息。",
                        "转发测试",
                        "timeSensitive",
                        "bark-test-" + UUID.randomUUID());
                runOnUiThread(() -> {
                    resetBarkTestButton();
                    showMessage("Bark 测试已发送",
                            "Bark 服务器已接受推送。请确认目标设备正常显示并解密正文。");
                });
            } catch (Exception e) {
                String safe = DeliveryEngine.safeError(e);
                runOnUiThread(() -> {
                    resetBarkTestButton();
                    showMessage("Bark 测试失败", safe);
                });
            }
        }, "bark-test").start();
    }

    private void resetBarkTestButton() {
        barkTestButton.setText("发送一条 Bark 测试");
        refreshStatus();
    }

    private Network validatedWifiNetwork() {
        return WifiNetwork.findValidated(this, null);
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }
}
