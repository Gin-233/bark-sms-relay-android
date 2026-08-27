package com.local.smsrelay;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private Config config;
    private Switch relaySwitch;
    private boolean updatingSwitch;
    private View liveDot;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private TextView barkMetric;
    private TextView pendingMetric;
    private TextView relaySubtitle;
    private LinearLayout recentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelayUi.applyWindow(this);
        Config.ensureInitialized(this);
        config = new Config(this);
        setContentView(RelayUi.withSystemBars(buildUi()));
        Scheduler.ensurePeriodic(this);
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (heroTitle != null) {
            SmsImporter.importRecent(this);
            refreshStatus();
        }
    }

    private View buildUi() {
        LinearLayout page = RelayUi.page(this, 12, 38);
        page.addView(buildBrandHeader());
        page.addView(buildHero(), RelayUi.matchWrapTop(this, 20));
        page.addView(buildRelayControl(), RelayUi.matchWrapTop(this, 12));

        page.addView(RelayUi.sectionLabel(this, "管理中心"));
        page.addView(RelayUi.menuRow(
                this,
                R.drawable.ic_channels,
                "Bark 与加密",
                "推送服务器 · Device Key · 正文加密",
                null,
                v -> RelayUi.open(this, ChannelSettingsActivity.class)),
                RelayUi.matchWrap());
        page.addView(RelayUi.menuRow(
                this,
                R.drawable.ic_diagnostics,
                "测试与诊断",
                "安全测试 · 队列状态 · 最近活动",
                null,
                v -> RelayUi.open(this, DiagnosticsActivity.class)),
                RelayUi.matchWrapTop(this, 10));
        page.addView(RelayUi.menuRow(
                this,
                R.drawable.ic_shield,
                "系统访问",
                "短信权限 · 通知 · 后台运行",
                null,
                v -> RelayUi.open(this, SystemAccessActivity.class)),
                RelayUi.matchWrapTop(this, 10));

        page.addView(RelayUi.sectionLabel(this, "最近活动"));
        page.addView(buildRecentCard(), RelayUi.matchWrap());

        LinearLayout security = new LinearLayout(this);
        security.setOrientation(LinearLayout.HORIZONTAL);
        security.setGravity(Gravity.CENTER);
        security.setPadding(0, RelayUi.dp(this, 22), 0, 0);
        ImageView lock = RelayUi.icon(this, R.drawable.ic_lock, 14, RelayUi.TEXT_MUTED);
        security.addView(lock, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 16), RelayUi.dp(this, 16)));
        TextView footer = RelayUi.text(this,
                " 本地加密 · 仅 Wi-Fi · 记录保留 30 天  ·  v1.0.0",
                11.5f, RelayUi.TEXT_MUTED, false);
        security.addView(footer);
        page.addView(security);
        return RelayUi.scroll(this, page);
    }

    private View buildBrandHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_brand_mark);
        logo.setPadding(RelayUi.dp(this, 11), RelayUi.dp(this, 11),
                RelayUi.dp(this, 11), RelayUi.dp(this, 11));
        GradientDrawable logoBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{RelayUi.NAVY, RelayUi.PRIMARY});
        logoBackground.setCornerRadius(RelayUi.dp(this, 17));
        logo.setBackground(logoBackground);
        row.addView(logo, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 54), RelayUi.dp(this, 54)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 14), 0, 0, 0);
        labels.addView(RelayUi.text(this, "短信安全转发", 23, RelayUi.TEXT, true));
        TextView subtitle = RelayUi.text(this,
                "SECURE SMS RELAY", 11.5f, RelayUi.PRIMARY, true);
        subtitle.setLetterSpacing(0.11f);
        subtitle.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        labels.addView(subtitle);
        row.addView(labels, RelayUi.weighted());
        return row;
    }

    private View buildHero() {
        LinearLayout hero = RelayUi.darkCard(this);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        liveDot = RelayUi.dot(this, RelayUi.SUCCESS, 9);
        top.addView(liveDot, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 9), RelayUi.dp(this, 9)));
        TextView live = RelayUi.text(this, "  LIVE STATUS", 11, Color.rgb(194, 207, 235), true);
        live.setLetterSpacing(0.10f);
        top.addView(live, RelayUi.weighted());
        top.addView(RelayUi.pill(this, "仅 WI-FI", RelayUi.CYAN,
                Color.argb(38, 255, 255, 255)));
        hero.addView(top, RelayUi.matchWrap());

        heroTitle = RelayUi.text(this, "安全链路运行中", 25, Color.WHITE, true);
        heroTitle.setPadding(0, RelayUi.dp(this, 18), 0, 0);
        hero.addView(heroTitle);
        heroSubtitle = RelayUi.text(this,
                "已准备接收、加密并推送新的短信", 13, Color.rgb(194, 207, 235), false);
        heroSubtitle.setPadding(0, RelayUi.dp(this, 5), 0, RelayUi.dp(this, 20));
        hero.addView(heroSubtitle);

        View line = new View(this);
        line.setBackgroundColor(Color.argb(36, 255, 255, 255));
        hero.addView(line, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, RelayUi.dp(this, 1)));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        metrics.setPadding(0, RelayUi.dp(this, 16), 0, 0);
        barkMetric = addMetric(metrics, "BARK", "已连接");
        metrics.addView(metricDivider(), metricDividerParams());
        pendingMetric = addMetric(metrics, "待处理", "0");
        hero.addView(metrics, RelayUi.matchWrap());
        return hero;
    }

    private TextView addMetric(LinearLayout parent, String label, String value) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(RelayUi.text(this, label, 10.5f,
                Color.rgb(157, 175, 213), true));
        TextView metric = RelayUi.text(this, value, 15, Color.WHITE, true);
        metric.setPadding(0, RelayUi.dp(this, 4), 0, 0);
        block.addView(metric);
        parent.addView(block, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return metric;
    }

    private View metricDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(45, 255, 255, 255));
        return divider;
    }

    private LinearLayout.LayoutParams metricDividerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                RelayUi.dp(this, 1), RelayUi.dp(this, 36));
        params.setMargins(RelayUi.dp(this, 10), 0, RelayUi.dp(this, 14), 0);
        return params;
    }

    private View buildRelayControl() {
        LinearLayout card = RelayUi.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(RelayUi.iconTile(this, R.drawable.ic_power,
                RelayUi.PRIMARY, Color.rgb(235, 241, 255), 44),
                new LinearLayout.LayoutParams(RelayUi.dp(this, 44), RelayUi.dp(this, 44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 14), 0, RelayUi.dp(this, 10), 0);
        labels.addView(RelayUi.text(this, "自动转发", 16, RelayUi.TEXT, true));
        relaySubtitle = RelayUi.text(this, "包含每日安全心跳", 12.5f,
                RelayUi.TEXT_SECONDARY, false);
        relaySubtitle.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        labels.addView(relaySubtitle);
        card.addView(labels, RelayUi.weighted());

        relaySwitch = new Switch(this);
        relaySwitch.setShowText(false);
        relaySwitch.setContentDescription("启用短信转发与每日心跳");
        relaySwitch.setChecked(config.enabled());
        relaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitch) {
                return;
            }
            config.setEnabled(isChecked);
            if (isChecked) {
                Scheduler.scheduleImmediate(this);
            } else {
                Scheduler.cancelDelivery(this);
            }
            Toast.makeText(this, isChecked ? "短信转发已启用" : "短信转发已暂停",
                    Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        card.addView(relaySwitch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View buildRecentCard() {
        LinearLayout card = RelayUi.card(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(RelayUi.text(this, "加密队列摘要", 15, RelayUi.TEXT, true),
                RelayUi.weighted());
        TextView refresh = RelayUi.text(this, "刷新", 12.5f, RelayUi.PRIMARY, true);
        refresh.setPadding(RelayUi.dp(this, 10), RelayUi.dp(this, 6),
                RelayUi.dp(this, 4), RelayUi.dp(this, 6));
        refresh.setOnClickListener(v -> refreshStatus());
        header.addView(refresh);
        card.addView(header, RelayUi.matchWrap());

        recentList = new LinearLayout(this);
        recentList.setOrientation(LinearLayout.VERTICAL);
        recentList.setPadding(0, RelayUi.dp(this, 10), 0, 0);
        card.addView(recentList, RelayUi.matchWrap());
        return card;
    }

    private void refreshStatus() {
        SmsStore store = SmsStore.get(this);
        boolean receive = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean read = checkSelfPermission(Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean permissionReady = receive && read;
        boolean enabled = config.enabled();

        int dotColor;
        if (!enabled) {
            dotColor = RelayUi.WARNING;
            heroTitle.setText("短信转发已暂停");
            heroSubtitle.setText("新短信会保留在本机，恢复后继续处理");
        } else if (!permissionReady) {
            dotColor = RelayUi.DANGER;
            heroTitle.setText("需要完成系统授权");
            heroSubtitle.setText("授予短信权限后才能可靠接收新消息");
        } else if (!config.barkReady()) {
            dotColor = RelayUi.WARNING;
            heroTitle.setText("等待配置 Bark");
            heroSubtitle.setText("主通道尚未连接，请打开通道设置");
        } else {
            dotColor = RelayUi.SUCCESS;
            heroTitle.setText("安全链路运行中");
            heroSubtitle.setText("已准备接收、加密并推送新的短信");
        }
        liveDot.setBackground(RelayUi.circle(dotColor));
        barkMetric.setText(config.barkReady() ? "已连接" : "待配置");
        pendingMetric.setText(Integer.toString(store.pendingCount()));
        relaySubtitle.setText(enabled ? "包含每日安全心跳" : "点击开关恢复服务");

        updatingSwitch = true;
        relaySwitch.setChecked(enabled);
        updatingSwitch = false;
        renderRecent(store.recentStatus(3));
    }

    private void renderRecent(List<String> lines) {
        recentList.removeAllViews();
        if (lines.isEmpty()) {
            TextView empty = RelayUi.text(this,
                    "暂无处理记录。新短信到达后会在这里显示脱敏状态。",
                    12.5f, RelayUi.TEXT_SECONDARY, false);
            empty.setPadding(0, RelayUi.dp(this, 4), 0, RelayUi.dp(this, 2));
            recentList.addView(empty);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                View divider = RelayUi.divider(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, RelayUi.dp(this, 1));
                dividerParams.setMargins(0, RelayUi.dp(this, 11), 0, RelayUi.dp(this, 11));
                recentList.addView(divider, dividerParams);
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            View dot = RelayUi.dot(this, RelayUi.PRIMARY, 7);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    RelayUi.dp(this, 7), RelayUi.dp(this, 7));
            dotParams.setMargins(0, RelayUi.dp(this, 6), RelayUi.dp(this, 10), 0);
            row.addView(dot, dotParams);
            TextView line = RelayUi.text(this, lines.get(i), 12,
                    RelayUi.TEXT_SECONDARY, false);
            row.addView(line, RelayUi.weighted());
            recentList.addView(row, RelayUi.matchWrap());
        }
    }
}
