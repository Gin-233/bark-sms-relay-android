package com.local.smsrelay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class RelayUi {
    static final int BACKGROUND = Color.rgb(246, 248, 252);
    static final int SURFACE = Color.WHITE;
    static final int TEXT = Color.rgb(20, 28, 45);
    static final int TEXT_SECONDARY = Color.rgb(92, 104, 126);
    static final int TEXT_MUTED = Color.rgb(133, 145, 165);
    static final int BORDER = Color.rgb(228, 233, 242);
    static final int PRIMARY = Color.rgb(49, 109, 250);
    static final int PRIMARY_DARK = Color.rgb(37, 76, 191);
    static final int NAVY = Color.rgb(14, 28, 53);
    static final int NAVY_LIGHT = Color.rgb(34, 57, 105);
    static final int CYAN = Color.rgb(75, 220, 210);
    static final int SUCCESS = Color.rgb(40, 190, 126);
    static final int WARNING = Color.rgb(245, 165, 53);
    static final int DANGER = Color.rgb(225, 74, 91);

    private RelayUi() {}

    static void applyWindow(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(BACKGROUND);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    static View withSystemBars(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int insetTop;
            int insetBottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetTop = bars.top;
                insetBottom = bars.bottom;
            } else {
                insetTop = insets.getSystemWindowInsetTop();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            target.setPadding(left, top + insetTop, right, bottom + insetBottom);
            return insets;
        });
        view.requestApplyInsets();
        return view;
    }

    static LinearLayout page(Context context, int topPadding, int bottomPadding) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(context, 20), dp(context, topPadding),
                dp(context, 20), dp(context, bottomPadding));
        page.setBackgroundColor(BACKGROUND);
        return page;
    }

    static ScrollView scroll(Context context, View content) {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    static LinearLayout toolbar(Activity activity, String title, String subtitle) {
        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(activity, 12), dp(activity, 8),
                dp(activity, 20), dp(activity, 10));
        toolbar.setBackgroundColor(BACKGROUND);

        ImageButton back = new ImageButton(activity);
        back.setImageResource(R.drawable.ic_arrow_back);
        back.setColorFilter(TEXT);
        back.setContentDescription("返回");
        back.setPadding(dp(activity, 12), dp(activity, 12),
                dp(activity, 12), dp(activity, 12));
        back.setBackground(ripple(circle(Color.TRANSPARENT), Color.argb(24, 30, 55, 105)));
        back.setOnClickListener(v -> finish(activity));
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(activity, 6), 0, 0, 0);
        labels.addView(text(activity, title, 21, TEXT, true));
        TextView sub = text(activity, subtitle, 12.5f, TEXT_SECONDARY, false);
        sub.setPadding(0, dp(activity, 2), 0, 0);
        labels.addView(sub);
        toolbar.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return toolbar;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 18), dp(context, 18),
                dp(context, 18), dp(context, 18));
        card.setBackground(rounded(SURFACE, 22, BORDER, 1, context));
        card.setElevation(dp(context, 1));
        return card;
    }

    static LinearLayout darkCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 20), dp(context, 20),
                dp(context, 20), dp(context, 20));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{NAVY, NAVY_LIGHT});
        background.setCornerRadius(dp(context, 26));
        card.setBackground(background);
        card.setElevation(dp(context, 3));
        return card;
    }

    static TextView sectionLabel(Context context, String value) {
        TextView label = text(context, value, 13, TEXT_SECONDARY, true);
        label.setLetterSpacing(0.05f);
        label.setPadding(dp(context, 2), dp(context, 24), 0, dp(context, 10));
        return label;
    }

    static LinearLayout menuRow(Context context, int iconResource, String title,
                                String subtitle, String badge, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 16), dp(context, 15),
                dp(context, 14), dp(context, 15));
        row.setMinimumHeight(dp(context, 82));
        row.setBackground(ripple(
                rounded(SURFACE, 20, BORDER, 1, context),
                Color.argb(24, 49, 109, 250)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        row.setElevation(dp(context, 1));

        row.addView(iconTile(context, iconResource, PRIMARY, Color.rgb(235, 241, 255), 44));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(context, 14), 0, dp(context, 8), 0);
        labels.addView(text(context, title, 16, TEXT, true));
        TextView sub = text(context, subtitle, 12.5f, TEXT_SECONDARY, false);
        sub.setPadding(0, dp(context, 3), 0, 0);
        labels.addView(sub);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (badge != null && !badge.isEmpty()) {
            TextView status = pill(context, badge, PRIMARY, Color.rgb(237, 243, 255));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.setMargins(0, 0, dp(context, 6), 0);
            row.addView(status, badgeParams);
        }

        ImageView arrow = icon(context, R.drawable.ic_arrow_forward, 18, TEXT_MUTED);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)));
        return row;
    }

    static ImageView iconTile(Context context, int iconResource, int tint,
                              int background, int size) {
        ImageView icon = icon(context, iconResource, size - 20, tint);
        icon.setPadding(dp(context, 10), dp(context, 10),
                dp(context, 10), dp(context, 10));
        icon.setBackground(rounded(background, 14, Color.TRANSPARENT, 0, context));
        return icon;
    }

    static ImageView icon(Context context, int iconResource, int size, int tint) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconResource);
        icon.setColorFilter(tint);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setAdjustViewBounds(true);
        icon.setMinimumWidth(dp(context, size));
        icon.setMinimumHeight(dp(context, size));
        return icon;
    }

    static TextView text(Context context, String value, float size, int color, boolean medium) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif",
                Typeface.NORMAL));
        return view;
    }

    static TextView pill(Context context, String value, int foreground, int background) {
        TextView pill = text(context, value, 11.5f, foreground, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(context, 10), dp(context, 5),
                dp(context, 10), dp(context, 5));
        pill.setBackground(rounded(background, 99, Color.TRANSPARENT, 0, context));
        return pill;
    }

    static TextView button(Context context, String value, boolean primary,
                           View.OnClickListener listener) {
        TextView button = text(context, value, 15, primary ? Color.WHITE : PRIMARY, true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 52));
        button.setPadding(dp(context, 16), dp(context, 12),
                dp(context, 16), dp(context, 12));
        Drawable content;
        if (primary) {
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{PRIMARY, PRIMARY_DARK});
            gradient.setCornerRadius(dp(context, 16));
            content = gradient;
        } else {
            content = rounded(SURFACE, 16, Color.rgb(203, 216, 246), 1, context);
        }
        button.setBackground(ripple(content,
                primary ? Color.argb(50, 255, 255, 255) : Color.argb(32, 49, 109, 250)));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(listener);
        return button;
    }

    static void setButtonEnabled(TextView button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.48f);
    }

    static EditText input(Context context, String hint, boolean password) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(TEXT_MUTED);
        input.setSelectAllOnFocus(false);
        input.setPadding(dp(context, 15), dp(context, 10),
                dp(context, 15), dp(context, 10));
        input.setMinHeight(dp(context, 56));
        input.setBackground(rounded(Color.rgb(250, 251, 254), 15, BORDER, 1, context));
        if (password) {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return input;
    }

    static LinearLayout field(Context context, String label, EditText input) {
        LinearLayout field = new LinearLayout(context);
        field.setOrientation(LinearLayout.VERTICAL);
        TextView caption = text(context, label, 12.5f, TEXT_SECONDARY, true);
        caption.setPadding(dp(context, 2), 0, 0, dp(context, 7));
        field.addView(caption);
        field.addView(input, matchWrap());
        return field;
    }

    static View divider(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(BORDER);
        return divider;
    }

    static View dot(Context context, int color, int size) {
        View dot = new View(context);
        dot.setBackground(circle(color));
        dot.setMinimumWidth(dp(context, size));
        dot.setMinimumHeight(dp(context, size));
        return dot;
    }

    static GradientDrawable rounded(int fill, int radius, int stroke,
                                    int strokeWidth, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radius));
        if (strokeWidth > 0) {
            drawable.setStroke(dp(context, strokeWidth), stroke);
        }
        return drawable;
    }

    static GradientDrawable circle(int fill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        return drawable;
    }

    static RippleDrawable ripple(Drawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams matchWrapTop(Context context, int topMargin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(context, topMargin), 0, 0);
        return params;
    }

    static LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static void open(Activity source, Class<?> destination) {
        source.startActivity(new Intent(source, destination));
        source.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    static void finish(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
