package ru.ascii.wallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;
import java.util.LinkedHashMap;

public final class MainActivity extends Activity {
    private static final int BG = 0xff020704, PANEL = 0xff06150b, LINE = 0xff174e29;
    private static final int TEXT = 0xffc4ffd3, MUTED = 0xff83b591, ACCENT = 0xff39ff78;
    private static final String[] SUBTITLES = {
        "Медленное вращение. Чистая геометрия.", "Мягкий рельеф из движущихся символов.",
        "Бесконечное движение замкнутой формы.", "Свет, меридианы и плавное дыхание.",
        "Спиральный танец звёздных символов.", "Знакомый цифровой дождь в новом цвете."
    };
    private Settings settings;
    private Preview preview;
    private Preview fullPreview;
    private Dialog fullDialog;
    private TextView caption, liveLabel;
    private TextView gridReadout;
    private LinearLayout cubeOptions;
    private final Button[] modes = new Button[6], colors = new Button[Settings.COLORS.length];
    private final Button[] fontButtons = new Button[Settings.FONT_NAMES.length], cubeButtons = new Button[2];
    private final LinkedHashMap<String, Control> controls = new LinkedHashMap<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(this);
        getWindow().setNavigationBarColor(BG);
        getWindow().setStatusBarColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = column(); content.setPadding(dp(22), dp(24), dp(22), dp(20));
        scroll.addView(content);

        TextView eyebrow = text("[ СИСТЕМА АКТИВНА ]     v1.2", 10, ACCENT);
        eyebrow.setLetterSpacing(.1f); content.addView(eyebrow);
        TextView title = text("ASCII_FLOW▌", 30, ACCENT);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setShadowLayer(dp(10), 0, 0, 0x6639ff78);
        content.addView(title, margins(-1, -2, 0, 6, 0, 2));
        content.addView(text("> реальность состоит из символов_", 12, MUTED), margins(-1, -2, 0, 0, 0, 22));
        content.addView(text(settings.device.description(), 11, MUTED), margins(-1, -2, 0, 0, 0, 14));

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(shape(Color.BLACK, LINE, 22)); stage.setClipToOutline(true);
        preview = new Preview(); stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        stage.addView(new Scanlines(), new FrameLayout.LayoutParams(-1, -1));
        liveLabel = text("01 / КУБ", 10, MUTED); liveLabel.setLetterSpacing(.15f);
        FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        badge.setMargins(dp(18), dp(16), 0, 0); stage.addView(liveLabel, badge);
        TextView live = text("●  LIVE", 10, ACCENT);
        FrameLayout.LayoutParams liveParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        liveParams.setMargins(0, dp(16), dp(18), 0); stage.addView(live, liveParams);
        TextView stageFooter = text("ПРЕДПРОСМОТР", 9, MUTED); stageFooter.setLetterSpacing(.2f);
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        footerParams.bottomMargin = dp(16); stage.addView(stageFooter, footerParams);
        content.addView(stage, margins(-1, dp(285), 0, 0, 0, 12));
        caption = text(SUBTITLES[settings.mode()], 12, MUTED); caption.setGravity(Gravity.CENTER);
        content.addView(caption, margins(-1, -2, 0, 0, 0, 10));
        Button expand = button("[ Развернуть предпросмотр ]");
        expand.setOnClickListener(v -> openPreview());
        content.addView(expand, margins(-1, dp(48), 0, 0, 0, 12));
        section(content, "01", "РЕЖИМ ДВИЖЕНИЯ");
        String[] marks = {"[+]", "≈", "◎", "◉", "✦", "//"};
        for (int row = 0; row < 3; row++) {
            LinearLayout pair = new LinearLayout(this);
            for (int col = 0; col < 2; col++) {
                final int mode = row * 2 + col;
                Button button = button(marks[mode] + "   " + AsciiScene.NAMES[mode]); modes[mode] = button;
                button.setOnClickListener(v -> { settings.set("mode", mode); refresh(); });
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(52), 1);
                bp.setMargins(col == 0 ? 0 : dp(5), 0, col == 0 ? dp(5) : 0, dp(10)); pair.addView(button, bp);
            }
            content.addView(pair);
        }
        cubeOptions = column();
        cubeOptions.addView(text("КУБ / ПОВЕРХНОСТЬ", 12, MUTED), margins(-1, -2, 0, 6, 0, 10));
        LinearLayout cubePair = new LinearLayout(this);
        for (int i = 0; i < 2; i++) {
            final int style = i;
            cubeButtons[i] = button(i == 0 ? "Заполненный" : "Каркас / 12 рёбер");
            cubeButtons[i].setTextSize(12);
            cubeButtons[i].setOnClickListener(v -> { settings.set("cube_style", style); refresh(); });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(52), 1);
            cp.setMargins(0, 0, i == 0 ? dp(8) : 0, 0); cubePair.addView(cubeButtons[i], cp);
        }
        cubeOptions.addView(cubePair);
        cubeOptions.addView(text("Все 12 рёбер соединены. В режиме каркаса\nсквозь куб видны задние линии.", 11, MUTED), margins(-1, -2, 0, 10, 0, 12));
        content.addView(cubeOptions);
        slider(content, "Масштаб фигуры", "object_scale", 40, 140, settings.objectScale());

        section(content, "02", "ШРИФТ СИМВОЛОВ");
        for (int i = 0; i < fontButtons.length; i++) {
            final int font = i;
            LinearLayout card = column(); card.setPadding(dp(10), dp(6), dp(10), dp(4));
            card.setBackground(shape(PANEL, LINE, 3));
            fontButtons[i] = button(Settings.FONT_NAMES[i]);
            fontButtons[i].setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            fontButtons[i].setOnClickListener(v -> { settings.set("font", font); refresh(); });
            card.addView(fontButtons[i], new LinearLayout.LayoutParams(-1, dp(48)));
            FontSample sample = new FontSample(font);
            sample.setOnClickListener(v -> { settings.set("font", font); refresh(); });
            card.addView(sample, new LinearLayout.LayoutParams(-1, dp(42)));
            content.addView(card, margins(-1, -2, 0, 0, 0, 10));
        }
        content.addView(text("Terminal — пиксельный стиль старого cmd.\nClassic — гладкий консольный шрифт Cousine.", 11, MUTED), margins(-1, -2, 0, 0, 0, 14));
        section(content, "03", "ПЛОТНОСТЬ И РАЗМЕР");
        content.addView(text("Больше столбцов → мельче и плотнее сетка.\nРазмер знака меняет заполнение ячейки.\nНажмите значение для точного ввода.", 11, MUTED), margins(-1, -2, 0, 0, 0, 12));
        LinearLayout presets = new LinearLayout(this);
        String[] presetNames = {"Крупно", "Баланс", "Мелко"};
        int[] presetColumns = {36, 64, 120};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Button preset = button(presetNames[i]); preset.setTextSize(11);
            preset.setOnClickListener(v -> {
                controls.get("columns").setValue(presetColumns[index]);
                controls.get("glyph_scale").setValue(90);
                controls.get("row_spacing").setValue(100);
            });
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(48), 1);
            pp.setMargins(0, 0, i < 2 ? dp(6) : 0, 0); presets.addView(preset, pp);
        }
        content.addView(presets, margins(-1, -2, 0, 0, 0, 14));
        slider(content, "Плотность / столбцы", "columns", 24, 180, settings.columns());
        slider(content, "Размер знака", "glyph_scale", 40, 150, settings.glyphScale());
        slider(content, "Интервал строк", "row_spacing", 70, 180, settings.rowSpacing());
        Button auto = button("[ Подобрать плотность для устройства ]");
        auto.setOnClickListener(v -> {
            controls.get("columns").setValue(settings.device.columns());
            settings.prefs.edit().remove("columns").apply();
            updateGridReadout();
        });
        content.addView(auto, margins(-1, dp(52), 0, 0, 0, 10));
        gridReadout = text("", 11, ACCENT);
        gridReadout.setPadding(dp(12), dp(12), dp(12), dp(12)); gridReadout.setBackground(shape(PANEL, LINE, 3));
        content.addView(gridReadout, margins(-1, -2, 0, 0, 0, 14));

        section(content, "04", "ЦВЕТ И ДВИЖЕНИЕ");
        content.addView(text("Цвет символов", 13, MUTED), margins(-1, -2, 0, 0, 0, 10));
        HorizontalScrollView colorScroll = new HorizontalScrollView(this); colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout palette = new LinearLayout(this); colorScroll.addView(palette);
        for (int i = 0; i < colors.length; i++) {
            final int index = i;
            colors[i] = button("● " + Settings.COLOR_NAMES[i]); colors[i].setTextSize(12);
            colors[i].setContentDescription("Цвет: " + Settings.COLOR_NAMES[i]);
            colors[i].setOnClickListener(v -> { settings.set("color", index); refresh(); });
            palette.addView(colors[i], margins(dp(108), dp(46), 0, 0, 8, 0));
        }
        content.addView(colorScroll, margins(-1, -2, 0, 0, 0, 18));
        slider(content, "Скорость / 0 = пауза", "speed", 0, 180, settings.speed());
        slider(content, "Яркость", "brightness", 5, 100, settings.brightness());
        Switch eco = new Switch(this); eco.setText("Экономный режим · 15 FPS"); eco.setTextSize(14); eco.setTextColor(TEXT);
        eco.setTypeface(Typeface.MONOSPACE);
        eco.setPadding(dp(14), dp(8), dp(14), dp(8)); eco.setBackground(shape(PANEL, LINE, 14));
        eco.setChecked(settings.prefs.getBoolean("eco", false));
        eco.setOnCheckedChangeListener((v, checked) -> settings.prefs.edit().putBoolean("eco", checked).apply());
        content.addView(eco, margins(-1, -2, 0, 8, 0, 14)); eco.setMinimumHeight(dp(58));
        TextView note = text("Чёрный фон • Без рекламы • Работает офлайн\nАнимация останавливается, когда обои не видны.", 11, MUTED);
        note.setLineSpacing(dp(4), 1); content.addView(note);

        LinearLayout dock = column(); dock.setPadding(dp(22), dp(12), dp(22), dp(12));
        dock.setBackgroundColor(BG);
        Button apply = button("> УСТАНОВИТЬ ОБОИ_"); apply.setTextColor(BG);
        apply.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); apply.setBackground(shape(ACCENT, ACCENT, 3));
        apply.setOnClickListener(v -> applyWallpaper());
        dock.addView(apply, new LinearLayout.LayoutParams(-1, dp(54))); root.addView(dock);
        refresh();
    }

    private void refresh() {
        int selected = settings.mode();
        for (int i = 0; i < modes.length; i++) {
            modes[i].setBackground(shape(i == selected ? 0xff0c3319 : PANEL, i == selected ? ACCENT : LINE, 3));
            modes[i].setTextColor(i == selected ? ACCENT : TEXT); modes[i].setSelected(i == selected);
            modes[i].setContentDescription(AsciiScene.NAMES[i] + (i == selected ? ", выбрано" : ""));
        }
        int color = settings.colorIndex();
        for (int i = 0; i < colors.length; i++) {
            colors[i].setTextColor(Settings.COLORS[i]); colors[i].setSelected(i == color);
            colors[i].setBackground(shape(PANEL, i == color ? Settings.COLORS[i] : LINE, 14));
        }
        caption.setText(SUBTITLES[selected]);
        cubeOptions.setVisibility(selected == 0 ? View.VISIBLE : View.GONE);
        controls.get("object_scale").setVisibility(selected == 1 || selected == 5 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < cubeButtons.length; i++) selection(cubeButtons[i], i == (settings.wireframe() ? 1 : 0));
        for (int i = 0; i < fontButtons.length; i++) {
            selection(fontButtons[i], i == settings.font());
            fontButtons[i].setText((i == settings.font() ? "[x] " : "[ ] ") + Settings.FONT_NAMES[i]);
        }
        liveLabel.setText("0" + (selected + 1) + " / " + AsciiScene.NAMES[selected].toUpperCase(java.util.Locale.ROOT));
        preview.invalidate();
        updateGridReadout();
    }

    private void slider(LinearLayout parent, String name, String key, int min, int max, int current) {
        Control control = new Control(name, key, min, max, current);
        controls.put(key, control);
        parent.addView(control, margins(-1, -2, 0, 4, 0, 12));
    }
    private String value(String key, int n) {
        if (key.equals("speed")) return n == 0 ? "ПАУЗА" : String.format(java.util.Locale.ROOT, "%.2f×", n / 30f);
        return key.equals("columns") ? n + "" : n + "%";
    }
    private void selection(Button button, boolean selected) {
        button.setSelected(selected);
        button.setTextColor(selected ? ACCENT : TEXT);
        button.setBackground(shape(selected ? 0xff0c3319 : PANEL, selected ? ACCENT : LINE, 3));
    }
    private void updateGridReadout() {
        if (gridReadout == null) return;
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        GridGeometry grid = new GridGeometry();
        grid.configure(metrics.widthPixels, metrics.heightPixels, settings.columns(), settings.rowSpacing());
        gridReadout.setText(String.format(java.util.Locale.ROOT,
                "> Сетка экрана: %d × %d\n> Ширина знака: ~%.1f px\n> Настройки сохраняются автоматически",
                grid.cols, grid.rows, grid.cellW * settings.glyphScale() / 100f));
    }
    private void openPreview() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        fullDialog = dialog;
        preview.stop();
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        fullPreview = new Preview();
        final Preview full = fullPreview;
        root.addView(full, new FrameLayout.LayoutParams(-1, -1));
        Button close = button("[ ЗАКРЫТЬ ]");
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(160), dp(52), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        closeParams.bottomMargin = dp(36); root.addView(close, closeParams);
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(root);
        dialog.setOnDismissListener(v -> {
            full.stop(); fullPreview = null; fullDialog = null;
            if (!isFinishing() && !isDestroyed()) preview.start();
        });
        dialog.show();
        dialog.getWindow().setLayout(-1, -1);
        full.start();
    }
    private void applyWallpaper() {
        WallpaperManager manager = WallpaperManager.getInstance(this);
        if (!settings.device.wallpaperSupported() || !manager.isWallpaperSupported()) {
            Toast.makeText(this, "Система этого устройства не поддерживает живые обои. Предпросмотр доступен в приложении.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!manager.isSetWallpaperAllowed()) {
            Toast.makeText(this, "Изменение обоев ограничено настройками устройства или администратором.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivity(new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            new ComponentName(this, AsciiWallpaperService.class)));
        } catch (ActivityNotFoundException e) {
            try { startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)); }
            catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Откройте настройки обоев и выберите ASCII Flow", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void section(LinearLayout parent, String number, String title) {
        TextView label = text(number + "  /  " + title, 16, TEXT);
        label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        parent.addView(label, margins(-1, -2, 0, 12, 0, 16));
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(1); return layout; }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.MONOSPACE); return view;
    }
    private Button button(String label) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(14);
        view.setTypeface(Typeface.MONOSPACE); view.setBackground(shape(PANEL, LINE, 3));
        view.setTextColor(TEXT); view.setMinWidth(0); view.setMinimumWidth(0);
        view.setPadding(dp(8), 0, dp(8), 0); return view;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(fill);
        drawable.setCornerRadius(dp(Math.min(3, radius))); drawable.setStroke(dp(1), stroke); return drawable;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }
    @Override public void onResume() { super.onResume(); if (preview != null) { refresh(); if (fullPreview == null) preview.start(); } if (fullPreview != null) fullPreview.start(); }
    @Override public void onPause() { if (preview != null) preview.stop(); if (fullPreview != null) fullPreview.stop(); super.onPause(); }
    @Override public void onDestroy() {
        if (fullDialog != null) fullDialog.dismiss();
        if (preview != null) preview.stop();
        super.onDestroy();
    }

    private final class Control extends LinearLayout {
        final String name, key;
        final int min, max;
        final SeekBar seek;
        final Button number;
        Control(String name, String key, int min, int max, int current) {
            super(MainActivity.this); this.name = name; this.key = key; this.min = min; this.max = max;
            setOrientation(VERTICAL);
            LinearLayout heading = new LinearLayout(MainActivity.this); heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(name, 12, TEXT);
            heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            number = button(value(key, current)); number.setTextSize(12); number.setTextColor(ACCENT);
            number.setContentDescription(name + ": точный ввод значения");
            number.setOnClickListener(v -> edit());
            heading.addView(number, new LinearLayout.LayoutParams(dp(98), dp(48))); addView(heading);
            LinearLayout track = new LinearLayout(MainActivity.this); track.setGravity(Gravity.CENTER_VERTICAL);
            Button minus = button("−"), plus = button("+");
            minus.setContentDescription("Уменьшить: " + name); plus.setContentDescription("Увеличить: " + name);
            track.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
            seek = new SeekBar(MainActivity.this); seek.setMax(max - min); seek.setProgress(current - min);
            seek.setProgressTintList(ColorStateList.valueOf(ACCENT)); seek.setThumbTintList(ColorStateList.valueOf(ACCENT));
            seek.setContentDescription(name);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                    int n = progress + min;
                    settings.set(key, n); number.setText(value(key, n)); updateGridReadout();
                    preview.invalidate();
                }
                public void onStartTrackingTouch(SeekBar bar) { }
                public void onStopTrackingTouch(SeekBar bar) { }
            });
            track.addView(seek, new LinearLayout.LayoutParams(0, dp(48), 1));
            track.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48))); addView(track);
            minus.setOnClickListener(v -> setValue(seek.getProgress() + min - 1));
            plus.setOnClickListener(v -> setValue(seek.getProgress() + min + 1));
        }
        void setValue(int n) { seek.setProgress(Math.max(min, Math.min(max, n)) - min); }
        void edit() {
            EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER); input.setSingleLine(true);
            input.setTypeface(Typeface.MONOSPACE); input.setTextColor(TEXT);
            input.setText(String.valueOf(seek.getProgress() + min)); input.selectAll();
            LinearLayout box = column(); box.setPadding(dp(24), dp(8), dp(24), dp(8));
            box.addView(text("Диапазон: " + min + "–" + max + (key.equals("speed") ? "\n30 = 1×, 0 = пауза" : ""), 12, MUTED));
            box.addView(input);
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle(name).setView(box)
                .setNegativeButton("Отмена", null).setPositiveButton("Применить", null).create();
            dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                try {
                    int n = Integer.parseInt(input.getText().toString().trim());
                    if (n < min || n > max) { input.setError("Введите число от " + min + " до " + max); return; }
                    setValue(n); dialog.dismiss();
                } catch (NumberFormatException e) { input.setError("Введите целое число"); }
            }));
            dialog.show();
        }
    }

    private final class FontSample extends View {
        final GlyphRenderer renderer = new GlyphRenderer(MainActivity.this);
        final char[] sample = "AaBb 0123 @#&+ /_<>".toCharArray();
        final int font;
        FontSample(int font) { super(MainActivity.this); this.font = font; setContentDescription("Образец: " + Settings.FONT_NAMES[font]); }
        @Override protected void onDraw(Canvas canvas) {
            float step = Math.min(dp(15), getWidth() / (float)(sample.length + 1));
            renderer.configure(font, ACCENT, step * .8f);
            for (int i = 0; i < sample.length; i++) renderer.draw(canvas, sample, i, (i + 1) * step, getHeight() * .5f, 235);
        }
    }
    private final class Scanlines extends View {
        private final Paint paint = new Paint();
        Scanlines() { super(MainActivity.this); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            paint.setColor(0x0e39ff78);
            for (int y = 0; y < getHeight(); y += dp(4)) canvas.drawLine(0, y, getWidth(), y, paint);
            paint.setColor(LINE); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1));
            canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, paint);
        }
    }

    private final class Preview extends View {
        private final AsciiPainter painter = new AsciiPainter(MainActivity.this);
        private boolean running;
        private final Runnable tick = () -> { if (running) invalidate(); };
        Preview() { super(MainActivity.this); setContentDescription("Анимированный предпросмотр ASCII-обоев"); }
        void start() { running = true; painter.pause(); invalidate(); }
        void stop() { running = false; removeCallbacks(tick); painter.pause(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            painter.draw(canvas, getWidth(), getHeight(), getResources().getDisplayMetrics().density, settings, true);
            removeCallbacks(tick);
            if (running) postDelayed(tick, 1000 / settings.fps());
        }
        @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
    }
}
