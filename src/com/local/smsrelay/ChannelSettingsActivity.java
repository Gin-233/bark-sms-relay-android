package com.local.smsrelay;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ChannelSettingsActivity extends Activity {
    private Config config;
    private EditText barkServer;
    private EditText barkDeviceKey;
    private TextView barkStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelayUi.applyWindow(this);
        Config.ensureInitialized(this);
        config = new Config(this);
        try {
            config.ensureBarkCrypto();
        } catch (RuntimeException ignored) {
            // Keep the settings page usable; save/show actions will report a safe error.
        }
        setContentView(RelayUi.withSystemBars(buildUi()));
        refreshStatus();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RelayUi.BACKGROUND);
        root.addView(RelayUi.toolbar(this, "Bark 与加密", "配置推送通道和正文加密"),
                RelayUi.matchWrap());

        LinearLayout page = RelayUi.page(this, 6, 28);
        page.addView(buildSecurityNote(), RelayUi.matchWrap());
        page.addView(RelayUi.sectionLabel(this, "BARK 推送"));
        page.addView(buildBarkCard(), RelayUi.matchWrap());

        TextView privacy = RelayUi.text(this,
                "Bark Device Key 和正文加密参数由 Android Keystore 保护。已保存的 Device Key 不会重新显示，也不会写入日志。",
                11.5f, RelayUi.TEXT_MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(RelayUi.dp(this, 8), RelayUi.dp(this, 20),
                RelayUi.dp(this, 8), 0);
        page.addView(privacy);

        root.addView(RelayUi.scroll(this, page), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(RelayUi.dp(this, 20), RelayUi.dp(this, 12),
                RelayUi.dp(this, 20), RelayUi.dp(this, 18));
        actions.setBackgroundColor(Color.WHITE);
        actions.setElevation(RelayUi.dp(this, 10));
        actions.addView(RelayUi.button(this, "加密保存配置", true,
                v -> saveConfiguration()), RelayUi.matchWrap());
        root.addView(actions, RelayUi.matchWrap());
        return root;
    }

    private View buildSecurityNote() {
        LinearLayout card = RelayUi.darkCard(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = RelayUi.icon(this, R.drawable.ic_lock, 22, RelayUi.CYAN);
        card.addView(icon, new LinearLayout.LayoutParams(
                RelayUi.dp(this, 28), RelayUi.dp(this, 28)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 14), 0, 0, 0);
        labels.addView(RelayUi.text(this, "推送正文加密", 17, Color.WHITE, true));
        TextView sub = RelayUi.text(this, "Bark 正文使用 AES-256-CBC 加密",
                12.5f, Color.rgb(194, 207, 235), false);
        sub.setPadding(0, RelayUi.dp(this, 3), 0, 0);
        labels.addView(sub);
        card.addView(labels, RelayUi.weighted());
        return card;
    }

    private View buildBarkCard() {
        LinearLayout card = RelayUi.card(this);
        LinearLayout header = channelHeader(
                R.drawable.ic_bell,
                "Bark",
                "设备的即时加密推送");
        card.addView(header, RelayUi.matchWrap());

        barkServer = RelayUi.input(this, "https://api.day.app", false);
        barkServer.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        barkServer.setText(config.barkServer());
        card.addView(RelayUi.field(this, "推送服务器", barkServer),
                RelayUi.matchWrapTop(this, 18));

        barkDeviceKey = RelayUi.input(this,
                secretHint(config.barkDeviceKey(), "粘贴 Bark Device Key"), true);
        barkDeviceKey.setContentDescription("Bark Device Key");
        card.addView(RelayUi.field(this, "设备密钥", barkDeviceKey),
                RelayUi.matchWrapTop(this, 14));

        TextView help = RelayUi.text(this,
                "只填写 Bark 推送 URL 最后一段 Device Key。完整 URL 和 Key 不要发送到聊天。",
                11.5f, RelayUi.TEXT_SECONDARY, false);
        help.setPadding(RelayUi.dp(this, 2), RelayUi.dp(this, 10),
                RelayUi.dp(this, 2), 0);
        card.addView(help);

        card.addView(RelayUi.button(this, "查看 Bark 加密参数", false,
                v -> showBarkEncryption()), RelayUi.matchWrapTop(this, 16));
        return card;
    }

    private LinearLayout channelHeader(int iconResource, String title,
                                       String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(RelayUi.iconTile(this, iconResource,
                RelayUi.PRIMARY, Color.rgb(235, 241, 255), 44),
                new LinearLayout.LayoutParams(RelayUi.dp(this, 44), RelayUi.dp(this, 44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(RelayUi.dp(this, 13), 0, RelayUi.dp(this, 8), 0);
        labels.addView(RelayUi.text(this, title, 16, RelayUi.TEXT, true));
        TextView sub = RelayUi.text(this, subtitle, 12, RelayUi.TEXT_SECONDARY, false);
        sub.setPadding(0, RelayUi.dp(this, 2), 0, 0);
        labels.addView(sub);
        row.addView(labels, RelayUi.weighted());

        TextView state = RelayUi.pill(this, "待配置", RelayUi.WARNING,
                Color.rgb(255, 247, 231));
        barkStatus = state;
        row.addView(state);
        return row;
    }

    private void saveConfiguration() {
        try {
            config.saveBark(
                    barkServer.getText().toString(),
                    barkDeviceKey.getText().toString());
            clearSecretFields();
            updateSecretHints();
            Scheduler.scheduleImmediate(this);
            Toast.makeText(this, "配置已加密保存", Toast.LENGTH_SHORT).show();
            refreshStatus();
        } catch (RuntimeException e) {
            showMessage("无法保存", DeliveryEngine.safeError(e));
        }
    }

    private void clearSecretFields() {
        barkDeviceKey.setText("");
    }

    private void updateSecretHints() {
        barkDeviceKey.setHint(secretHint(config.barkDeviceKey(), "粘贴 Bark Device Key"));
    }

    private void refreshStatus() {
        setStatusPill(barkStatus, config.barkReady(), "已连接");
        updateSecretHints();
    }

    private void setStatusPill(TextView pill, boolean ready, String readyLabel) {
        pill.setText(ready ? readyLabel : "待配置");
        pill.setTextColor(ready ? RelayUi.SUCCESS : RelayUi.WARNING);
        pill.setBackground(RelayUi.rounded(
                ready ? Color.rgb(233, 249, 241) : Color.rgb(255, 247, 231),
                99, Color.TRANSPARENT, 0, this));
    }

    private void showBarkEncryption() {
        String key;
        String iv;
        try {
            config.ensureBarkCrypto();
            key = config.barkAesKey();
            iv = config.barkFallbackIv();
            if (key == null || iv == null) {
                throw new IllegalStateException("Android Keystore 暂时不可用");
            }
        } catch (RuntimeException e) {
            showMessage("无法读取加密参数", DeliveryEngine.safeError(e));
            return;
        }
        TextView content = RelayUi.text(this,
                "请在 iPhone Bark → 推送加密 → 加密设置中填写：\n\n"
                        + "算法：AES256\n模式：CBC\nPadding：pkcs7\n\n"
                        + "Key：\n" + key + "\n\n"
                        + "IV：\n" + iv + "\n\n"
                        + "Key 和 IV 属于秘密，不要截图或发送到聊天。IV 只用于 Bark 保存配置；每条推送会携带新的随机 IV。",
                14.5f, RelayUi.TEXT, false);
        content.setTextIsSelectable(true);
        content.setPadding(RelayUi.dp(this, 22), RelayUi.dp(this, 8),
                RelayUi.dp(this, 22), 0);
        new AlertDialog.Builder(this)
                .setTitle("Bark 加密参数")
                .setView(content)
                .setPositiveButton("完成", null)
                .show();
    }

    private static String secretHint(String existing, String emptyHint) {
        return existing == null || existing.isEmpty() ? emptyHint : "已安全保存 · 留空不修改";
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }
}
