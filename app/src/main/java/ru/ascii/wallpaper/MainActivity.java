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
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.MotionEvent;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private Settings settings;
    private Preview preview;
    private Preview fullPreview;
    private Dialog fullDialog;
    private LinearLayout cubeOptions;
    private LinearLayout mediaOptions;
    private PresetStore presetStore;
    private TextView symbolsLabel, mediaLabel;
    private final Button[] modes = new Button[AsciiScene.NAMES.length], colors = new Button[Settings.COLORS.length];
    private final Button[] fontButtons = new Button[Settings.FONT_NAMES.length], cubeButtons = new Button[2];
    private final LinkedHashMap<String, Control> controls = new LinkedHashMap<>();
    private final LinkedHashMap<String, Switch> toggles = new LinkedHashMap<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(this);
        presetStore = new PresetStore(this,settings);
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

        TextView title = text("ASCII Flow", 25, TEXT);
        content.addView(title, margins(-1, -2, 0, 0, 0, 16));

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(shape(Color.BLACK, LINE, 22)); stage.setClipToOutline(true);
        preview = new Preview(); stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        content.addView(stage, margins(-1, dp(240), 0, 0, 0, 10));
        slider(content, "Частота кадров", "fps_limit", 0, settings.maxFps(), settings.requestedFps());
        toggle(content, "Экономный режим (до 15 FPS)", "eco", false);
        Button expand = button("На весь экран");
        expand.setOnClickListener(v -> openPreview());
        content.addView(expand, margins(-1, dp(48), 0, 0, 0, 12));
        section(content, "ЧАСЫ НА БЛОКИРОВКЕ");
        toggle(content,"ASCII-часы на экране блокировки","lock_clock",false);
        slider(content,"Положение часов по вертикали","lock_clock_y",0,100,settings.lockClockY());
        action(content,"[ Предпросмотр ASCII-часов ]",() -> openPreview(true));
        section(content, "РЕЖИМ ДВИЖЕНИЯ");
        String[] marks = {"[+]", "≈", "◎", "◉", "✦", "//", "(~)", "[i]", "[>]", "●", "*", "·", "ϟ", "♣", "✿", "^", "∧", "▥", "≈", "♧", "❄"};
        for (int row = 0; row < (modes.length+1)/2; row++) {
            LinearLayout pair = new LinearLayout(this);
            pair.setBaselineAligned(false);
            pair.setGravity(Gravity.TOP);
            boolean single = row * 2 + 1 == modes.length;
            for (int col = 0; col < 2; col++) {
                final int mode = row * 2 + col;
                if(mode>=modes.length)break;
                Button button = button(marks[mode] + "   " + AsciiScene.NAMES[mode]); modes[mode] = button;
                button.setGravity(Gravity.CENTER);
                button.setMaxLines(2);
                button.setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
                button.setOnClickListener(v -> { settings.set("mode", mode); refresh(); });
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(52), 1);
                bp.setMargins(col == 0 ? 0 : dp(5), 0, col == 0 && !single ? dp(5) : 0, dp(10));
                pair.addView(button, bp);
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
        slider(content,"Вращение по X","rotation_x",0,200,settings.parameter("rotation_x",100,0,200));
        slider(content,"Вращение по Y","rotation_y",0,200,settings.parameter("rotation_y",100,0,200));
        toggle(content,"Вращение галактики","galaxy_spin",true);
        slider(content,"Скорость вращения галактики","galaxy_rotation",0,200,settings.parameter("galaxy_rotation",100,0,200));
        slider(content,"Частота волн","wave_frequency",25,250,settings.parameter("wave_frequency",100,25,250));
        slider(content,"Сила волн","wave_strength",10,200,settings.parameter("wave_strength",100,10,200));
        slider(content,"Толщина тора","torus_thickness",30,160,settings.parameter("torus_thickness",100,30,160));
        slider(content,"Пульсация сферы","sphere_pulse",0,250,settings.parameter("sphere_pulse",100,0,250));
        slider(content,"Рукава галактики","galaxy_arms",1,8,settings.parameter("galaxy_arms",3,1,8));
        toggle(content,"Анимация звёзд (пульсация)","stars_animation",false);
        toggle(content,"Волны от чёрной дыры при касании","blackhole_waves",true);
        slider(content,"Длина хвостов","rain_length",25,250,settings.parameter("rain_length",100,25,250));
        slider(content,"Шаг струй дождя","rain_spacing",1,6,settings.parameter("rain_spacing",2,1,6));
        slider(content,"Скорость кругов","ripple_speed",25,250,settings.parameter("ripple_speed",100,25,250));
        slider(content,"Жизнь волны, сек.","ripple_life",1,10,settings.parameter("ripple_life",4,1,10));
        toggle(content,"Волны от касаний поверх других режимов","touch",true);
        mediaOptions=column();
        mediaLabel=text("",12,MUTED); mediaOptions.addView(mediaLabel);
        Button choose=button("[ Выбрать файл ]"); choose.setOnClickListener(v -> chooseMedia());
        mediaOptions.addView(choose,margins(-1,dp(52),0,10,0,10));
        slider(mediaOptions,"Контраст медиа","media_contrast",25,250,settings.parameter("media_contrast",100,25,250));
        toggle(mediaOptions,"Инверсия света и тени","media_invert",false);
        content.addView(mediaOptions,margins(-1,-2,0,8,0,18));

        section(content, "ШРИФТ СИМВОЛОВ");
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
        symbolsLabel=text("",12,MUTED); content.addView(symbolsLabel);
        Button symbolsButton=button("[ Свои ASCII-символы ]"); symbolsButton.setOnClickListener(v -> editSymbols());
        content.addView(symbolsButton,margins(-1,dp(52),0,8,0,14));
        section(content, "ПЛОТНОСТЬ И РАЗМЕР");
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
        });
        content.addView(auto, margins(-1, dp(52), 0, 0, 0, 10));
        section(content, "ЦВЕТ И ДВИЖЕНИЕ");
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
        section(content, "МОИ ПРЕСЕТЫ");
        action(content,"Сохранить текущие настройки",this::savePreset);
        action(content,"Открыть / удалить пресет",this::showPresets);
        action(content,"Экспорт пресетов в файл",() -> document(Intent.ACTION_CREATE_DOCUMENT,"application/json",202));
        action(content,"Импорт пресетов из файла",() -> document(Intent.ACTION_OPEN_DOCUMENT,"*/*",203));

        LinearLayout dock = column(); dock.setPadding(dp(22), dp(12), dp(22), dp(12));
        dock.setBackgroundColor(BG);
        Button apply = button("Установить обои"); apply.setTextColor(BG);
        apply.setTypeface(getResources().getFont(R.font.terminal_ui), Typeface.BOLD); apply.setBackground(shape(ACCENT, ACCENT, 3));
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
        cubeOptions.setVisibility(selected == 0 ? View.VISIBLE : View.GONE);
        showControl("object_scale",selected==0||selected==2||selected==3||selected==4||selected==9);
        showControl("rotation_x",selected==0||selected==2||selected==3); showControl("rotation_y",selected==0||selected==2||selected==3);
        showControl("galaxy_spin",selected==4);
        showControl("galaxy_rotation",selected==4&&settings.prefs.getBoolean("galaxy_spin",true));
        showControl("blackhole_waves",selected==9);
        showControl("wave_frequency",selected==1); showControl("wave_strength",selected==1);
        showControl("torus_thickness",selected==2); showControl("sphere_pulse",selected==3);
        showControl("galaxy_arms",selected==4);
        showControl("stars_animation",selected==11);
        showControl("rain_length",selected==5); showControl("rain_spacing",selected==5);
        showControl("ripple_speed",settings.ripplesActive()); showControl("ripple_life",settings.ripplesActive());
        mediaOptions.setVisibility(selected==7||selected==8?View.VISIBLE:View.GONE);
        mediaLabel.setText(settings.mediaUri().isEmpty()?"Файл ещё не выбран":"Файл выбран. Можно заменить его ниже.");
        symbolsLabel.setText(settings.symbols().isEmpty()?"Символы: стандартные для эффекта":"Символы: "+settings.symbols());
        for (int i = 0; i < cubeButtons.length; i++) selection(cubeButtons[i], i == (settings.wireframe() ? 1 : 0));
        for (int i = 0; i < fontButtons.length; i++) {
            selection(fontButtons[i], i == settings.font());
            fontButtons[i].setText((i == settings.font() ? "[x] " : "[ ] ") + Settings.FONT_NAMES[i]);
        }
        preview.invalidate();
    }

    private void showControl(String key,boolean visible) {
        View view = controls.containsKey(key) ? controls.get(key) : toggles.get(key);
        if (view != null) view.setVisibility(visible?View.VISIBLE:View.GONE);
    }
    private void toggle(LinearLayout parent,String label,String key,boolean fallback) {
        Switch view=new Switch(this); view.setText(label); view.setTextColor(TEXT); view.setTextSize(12);
        view.setTypeface(getResources().getFont(R.font.terminal_ui)); view.setMinimumHeight(dp(56)); view.setChecked(settings.prefs.getBoolean(key,fallback));
        view.setOnCheckedChangeListener((v,checked) -> { settings.prefs.edit().putBoolean(key,checked).apply(); refresh(); });
        parent.addView(view,margins(-1,-2,0,6,0,10));
        toggles.put(key,view);
    }
    private void action(LinearLayout parent,String label,Runnable task) {
        Button button=button(label); button.setOnClickListener(v -> task.run());
        parent.addView(button,margins(-1,dp(52),0,0,0,10));
    }
    private void message(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    private void editSymbols() {
        EditText input=new EditText(this); input.setText(settings.symbols()); input.setTextColor(TEXT); input.setTypeface(getResources().getFont(R.font.terminal_ui));
        input.setSingleLine(true); input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(94)});
        LinearLayout box=column(); box.setPadding(dp(24),dp(8),dp(24),dp(8));
        box.addView(text("Латиница, цифры, ASCII-знаки — до 94.\nПорядок: от лёгких к плотным.\nПустое поле возвращает стандартный набор.",12,MUTED));
        box.addView(input);
        LinearLayout examples=new LinearLayout(this);
        for(String sample:new String[]{"01"," .:-=+*#%@","<>/\\{}[]"}) { Button b=button(sample); b.setTextSize(11); b.setOnClickListener(v -> input.setText(sample)); examples.addView(b,new LinearLayout.LayoutParams(0,dp(48),1)); }
        box.addView(examples);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Свои символы").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Применить",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(b -> {
            try { String value=PresetStore.cleanSymbols(input.getText().toString()); settings.prefs.edit().putString("symbols",value).apply(); refresh(); dialog.dismiss(); }
            catch(IllegalArgumentException e) { input.setError(e.getMessage()); }
        })); dialog.show();
    }
    private void savePreset() {
        EditText input=new EditText(this); input.setSingleLine(true); input.setHint("Название пресета"); input.setTextColor(TEXT);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(50)});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Сохранить пресет").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(b -> {
            String name=input.getText().toString().trim();
            if(java.util.Arrays.asList(presetStore.names()).contains(name)) {
                new AlertDialog.Builder(this).setMessage("Заменить пресет «"+name+"»?").setNegativeButton("Отмена",null)
                    .setPositiveButton("Заменить",(d,w) -> { try { presetStore.save(name); dialog.dismiss(); message("Пресет обновлён"); } catch(Exception e){input.setError(e.getMessage());} }).show();
            } else try { presetStore.save(name); dialog.dismiss(); message("Пресет сохранён"); } catch(Exception e){input.setError(e.getMessage());}
        })); dialog.show();
    }
    private void showPresets() {
        String[] names=presetStore.names(); if(names.length==0){message("Пока нет сохранённых пресетов");return;}
        new AlertDialog.Builder(this).setTitle("Мои пресеты").setItems(names,(d,i) -> {
            new AlertDialog.Builder(this).setTitle(names[i]).setItems(new String[]{"Применить","Удалить"},(a,which) -> {
                if(which==0) { try { presetStore.apply(names[i]); recreate(); }catch(Exception e){message("Не удалось применить пресет");} }
                else new AlertDialog.Builder(this).setMessage("Удалить «"+names[i]+"»?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(v,w)->presetStore.remove(names[i])).show();
            }).show();
        }).show();
    }
    private void chooseMedia() { document(Intent.ACTION_OPEN_DOCUMENT,settings.mode()==8?"video/*":"image/*",settings.mode()==8?201:200); }
    private void document(String action,String type,int request) {
        Intent intent=new Intent(action).setType(type).addCategory(Intent.CATEGORY_OPENABLE);
        if(request==202)intent.putExtra(Intent.EXTRA_TITLE,"ascii-flow-presets.json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(request==200||request==201)intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent,request); }catch(ActivityNotFoundException e){message("На устройстве нет приложения для выбора файлов");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==200||request==201) {
            try {
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                settings.prefs.edit().putString(request==201?"video_uri":"photo_uri",uri.toString()).putInt("mode",request==201?8:7)
                    .putInt("media_revision",settings.prefs.getInt("media_revision",0)+1).apply();
                refresh(); message("Файл выбран");
            } catch(Exception e){message("Не удалось сохранить доступ к файлу. Выберите файл из памяти устройства.");}
            return;
        }
        if(request!=202&&request!=203)return;
        final String export;
        try { export=request==202?presetStore.exportAll():""; }catch(Exception e){message("Не удалось подготовить пресеты");return;}
        new Thread(() -> {
            String status;
            try {
                if(request==202) {
                    try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")) {
                        if(out==null)throw new IOException(); out.write(export.getBytes(StandardCharsets.UTF_8));
                    }
                    status="Пресеты экспортированы";
                } else {
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    try(InputStream in=getContentResolver().openInputStream(uri)) {
                        if(in==null)throw new IOException(); byte[] buffer=new byte[4096]; int n;
                        while((n=in.read(buffer))!=-1) { if(bytes.size()+n>65536)throw new IllegalArgumentException("Файл пресетов слишком большой (максимум 64 КБ)"); bytes.write(buffer,0,n); }
                    }
                    int count=presetStore.importAll(new String(bytes.toByteArray(),StandardCharsets.UTF_8)); status="Импортировано пресетов: "+count;
                }
            } catch(Exception e) { status=e instanceof IllegalArgumentException?e.getMessage():"Не удалось прочитать или записать файл пресетов"; }
            final String done=status; runOnUiThread(() -> { if(!isDestroyed())message(done); });
        },"ascii-preset-io").start();
    }

    private void slider(LinearLayout parent, String name, String key, int min, int max, int current) {
        Control control = new Control(name, key, min, max, current);
        controls.put(key, control);
        parent.addView(control, margins(-1, -2, 0, 4, 0, 12));
    }
    private String value(String key, int n) {
        if (key.equals("fps_limit")) return n == 0 ? "Авто" : n + " FPS";
        if (key.equals("speed")) return n == 0 ? "ПАУЗА" : String.format(java.util.Locale.ROOT, "%.2f×", n / 30f);
        return key.equals("columns")||key.equals("galaxy_arms")||key.equals("rain_spacing")||key.equals("ripple_life") ? n + "" : n + "%";
    }
    private void selection(Button button, boolean selected) {
        button.setSelected(selected);
        button.setTextColor(selected ? ACCENT : TEXT);
        button.setBackground(shape(selected ? 0xff0c3319 : PANEL, selected ? ACCENT : LINE, 3));
    }
    private void openPreview() {
        openPreview(false);
    }
    private void openPreview(boolean clockPreview) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        fullDialog = dialog;
        preview.stop();
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        fullPreview = new Preview(clockPreview);
        final Preview full = fullPreview;
        root.addView(full, new FrameLayout.LayoutParams(-1, -1));
        Button close = button("Закрыть");
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(160), dp(52), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        closeParams.bottomMargin = dp(36); root.addView(close, closeParams);
        close.setOnClickListener(v -> dialog.dismiss());
        if (clockPreview) {
            LinearLayout panel = column(); panel.setBackgroundColor(0xe606150b);
            panel.setPadding(dp(14), dp(6), dp(14), dp(6));
            panel.addView(new Control("Положение часов", "lock_clock_y", 0, 100, settings.lockClockY()));
            FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            panelParams.bottomMargin = dp(100); root.addView(panel, panelParams);
        }
        dialog.setContentView(root);
        dialog.setOnDismissListener(v -> {
            full.stop(); fullPreview = null; fullDialog = null;
            Control section = controls.get("lock_clock_y");
            if (section != null) section.setValue(settings.lockClockY());
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
    private void section(LinearLayout parent, String title) {
        TextView label = text(title, 16, TEXT);
        label.setTypeface(getResources().getFont(R.font.terminal_ui), Typeface.BOLD);
        parent.addView(label, margins(-1, -2, 0, 12, 0, 16));
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(1); return layout; }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(getResources().getFont(R.font.terminal_ui)); return view;
    }
    private Button button(String label) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(14);
        view.setTypeface(getResources().getFont(R.font.terminal_ui)); view.setBackground(shape(PANEL, LINE, 3));
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
                    settings.set(key, n); number.setText(value(key, n));
                    preview.invalidate();
                    if (fullPreview != null) fullPreview.invalidate();
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
            input.setTypeface(getResources().getFont(R.font.terminal_ui)); input.setTextColor(TEXT);
            input.setText(String.valueOf(seek.getProgress() + min)); input.selectAll();
            LinearLayout box = column(); box.setPadding(dp(24), dp(8), dp(24), dp(8));
            box.addView(text("Диапазон: " + min + "–" + max + (key.equals("speed") ? "\n30 = 1×, 0 = пауза" : key.equals("fps_limit") ? "\n0 = авто" : ""), 12, MUTED));
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
    private final class Preview extends View {
        private final AsciiPainter painter = new AsciiPainter(MainActivity.this);
        private final LockClockRenderer clock=new LockClockRenderer(MainActivity.this);
        private final boolean clockPreview;
        private boolean running;
        private final FramePacer pacer = new FramePacer();
        private final Runnable tick = () -> { if (running) invalidate(); };
        Preview() { this(false); }
        Preview(boolean clockPreview) { super(MainActivity.this);this.clockPreview=clockPreview; setContentDescription(clockPreview?"Предпросмотр ASCII-часов":"Анимированный предпросмотр ASCII-обоев"); }
        void start() { running = true; painter.pause(); invalidate(); }
        void stop() { running = false; removeCallbacks(tick); painter.pause(); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if(clockPreview)return super.onTouchEvent(event);
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN || event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN) {
                int i=event.getActionIndex(); painter.touch(event.getX(i),event.getY(i),getWidth(),getHeight(),settings); invalidate();
                getParent().requestDisallowInterceptTouchEvent(true); return true;
            }
            if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override protected void onDraw(Canvas canvas) {
            long startedAt = android.os.SystemClock.uptimeMillis();
            super.onDraw(canvas);
            if(clockPreview)clock.draw(canvas,getWidth(),getHeight(),settings);
            else painter.draw(canvas, getWidth(), getHeight(), getResources().getDisplayMetrics().density, settings, true);
            removeCallbacks(tick);
            if (running) {
                long next = pacer.nextFrame(startedAt, clockPreview ? 1 : settings.fps());
                postDelayed(tick, Math.max(0, next - android.os.SystemClock.uptimeMillis()));
            }
        }
        @Override protected void onDetachedFromWindow() { stop(); painter.close(); clock.close(); super.onDetachedFromWindow(); }
    }
}
