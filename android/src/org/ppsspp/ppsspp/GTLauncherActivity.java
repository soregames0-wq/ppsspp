package org.ppsspp.ppsspp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;

/** Menu proprio: selecao da ROM do Gran Turismo + configuracoes (substitui os menus do PPSSPP). */
public class GTLauncherActivity extends Activity {
    static final int REQ_ROM = 4242;
    static final int BG = 0xFF0B0F14, CARD = 0xFF151D27, ACCENT = 0xFFE10600, TEXT = 0xFFEAF1F8, DIM = 0xFF8A98A8;
    SharedPreferences prefs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("gt", MODE_PRIVATE);
        showMain();
    }

    @Override protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // ---------- helpers de UI ----------
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    Button button(String label, boolean primary, View.OnClickListener l) {
        Button x = new Button(this);
        x.setText(label); x.setTextColor(TEXT); x.setAllCaps(true); x.setTextSize(16);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary ? ACCENT : CARD); g.setCornerRadius(dp(14));
        x.setBackground(g); x.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        p.setMargins(0, dp(8), 0, dp(8)); x.setLayoutParams(p);
        return x;
    }

    // ---------- tela principal ----------
    void showMain() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);
        root.setPadding(dp(32), dp(24), dp(32), dp(24));

        TextView title = text("GRAN TURISMO", 34, TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView sub = text("PSP  |  EUROPE", 14, ACCENT, true);
        sub.setGravity(Gravity.CENTER);
        String rom = prefs.getString("rom_name", null);
        TextView st = text(rom == null ? "Nenhuma ROM selecionada" : "ROM: " + rom, 13, DIM, false);
        st.setGravity(Gravity.CENTER);
        st.setPadding(0, dp(16), 0, dp(24));

        root.addView(title); root.addView(sub); root.addView(st);
        root.addView(button("Jogar", true, new View.OnClickListener() { public void onClick(View v) { play(); } }));
        root.addView(button("Selecionar ROM", false, new View.OnClickListener() { public void onClick(View v) { pickRom(); } }));
        root.addView(button("Configuracoes", false, new View.OnClickListener() { public void onClick(View v) { showSettings(); } }));
        setContentView(root);
    }

    void pickRom() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_ROM);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_ROM && res == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) {}
            String name = u.getLastPathSegment();
            if (name != null && name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
            prefs.edit().putString("rom_uri", u.toString()).putString("rom_name", name).apply();
            showMain();
        }
    }

    void play() {
        String uri = prefs.getString("rom_uri", null);
        if (uri == null) { Toast.makeText(this, "Selecione a ROM primeiro", Toast.LENGTH_SHORT).show(); pickRom(); return; }
        writeIni();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setClassName(getPackageName(), "org.ppsspp.ppsspp.PpssppActivity");
        i.setData(Uri.parse(uri));
        i.putExtra("org.ppsspp.ppsspp.Shortcuts", uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivity(i);
    }

    // ---------- tela de configuracoes ----------
    Spinner resSpin; SeekBar skipBar, opBar, scBar; Switch soundSw, vsyncSw, touchSw;

    SeekBar seek(LinearLayout parent, String label, int max, int cur) {
        parent.addView(text(label, 14, DIM, false));
        SeekBar s = new SeekBar(this); s.setMax(max); s.setProgress(cur);
        parent.addView(s);
        return s;
    }

    Switch sw(LinearLayout parent, String label, boolean cur) {
        Switch s = new Switch(this); s.setText(label); s.setTextColor(TEXT); s.setChecked(cur);
        s.setPadding(0, dp(10), 0, dp(10));
        parent.addView(s);
        return s;
    }

    void showSettings() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(28), dp(24), dp(28), dp(24));
        sv.addView(c);

        c.addView(text("CONFIGURACOES", 24, TEXT, true));
        c.addView(text("Grafico", 13, ACCENT, true));

        c.addView(text("Resolucao", 14, DIM, false));
        resSpin = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Automatica", "1x (480x272)", "2x", "3x", "4x", "5x"});
        resSpin.setAdapter(ad); resSpin.setSelection(prefs.getInt("res", 2));
        c.addView(resSpin);
        skipBar = seek(c, "Frameskip (0 = desligado)", 5, prefs.getInt("skip", 0));
        vsyncSw = sw(c, "VSync", prefs.getBoolean("vsync", true));

        c.addView(text("Audio", 13, ACCENT, true));
        soundSw = sw(c, "Som ligado", prefs.getBoolean("sound", true));

        c.addView(text("Controles na tela", 13, ACCENT, true));
        touchSw = sw(c, "Mostrar botoes touch", prefs.getBoolean("touch", true));
        opBar = seek(c, "Opacidade dos botoes (%)", 100, prefs.getInt("opacity", 65));
        scBar = seek(c, "Tamanho dos botoes (50-200%)", 150, prefs.getInt("scale", 100) - 50);

        c.addView(button("Salvar e voltar", true, new View.OnClickListener() { public void onClick(View v) {
            prefs.edit().putInt("res", resSpin.getSelectedItemPosition()).putInt("skip", skipBar.getProgress())
                .putBoolean("vsync", vsyncSw.isChecked()).putBoolean("sound", soundSw.isChecked())
                .putBoolean("touch", touchSw.isChecked()).putInt("opacity", opBar.getProgress())
                .putInt("scale", scBar.getProgress() + 50).apply();
            writeIni(); showMain();
        }}));
        c.addView(button("Voltar sem salvar", false, new View.OnClickListener() { public void onClick(View v) { showMain(); } }));
        setContentView(sv);
    }

    @Override public void onBackPressed() { showMain(); }

    // ---------- escreve no ppsspp.ini ----------
    void writeIni() {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) return;
            File f = new File(dir, "PSP/SYSTEM/ppsspp.ini");
            f.getParentFile().mkdirs();
            ArrayList<String> lines = new ArrayList<String>();
            if (f.exists()) {
                BufferedReader r = new BufferedReader(new FileReader(f));
                String l; while ((l = r.readLine()) != null) lines.add(l);
                r.close();
            }
            set(lines, "Graphics", "InternalResolution", String.valueOf(prefs.getInt("res", 2)));
            set(lines, "Graphics", "FrameSkip", String.valueOf(prefs.getInt("skip", 0)));
            set(lines, "Graphics", "VSyncInterval", prefs.getBoolean("vsync", true) ? "True" : "False");
            set(lines, "Sound", "Enable", prefs.getBoolean("sound", true) ? "True" : "False");
            set(lines, "Control", "ShowTouchControls", prefs.getBoolean("touch", true) ? "True" : "False");
            set(lines, "Control", "TouchButtonOpacity", String.valueOf(prefs.getInt("opacity", 65)));
            set(lines, "Control", "ActionButtonScale", String.valueOf(prefs.getInt("scale", 100) / 100f));
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            for (String s : lines) { w.write(s); w.newLine(); }
            w.close();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    static void set(ArrayList<String> L, String sec, String key, String val) {
        int s = -1;
        for (int i = 0; i < L.size(); i++) if (L.get(i).trim().equals("[" + sec + "]")) { s = i; break; }
        if (s < 0) { L.add("[" + sec + "]"); L.add(key + " = " + val); return; }
        int i = s + 1;
        for (; i < L.size() && !L.get(i).trim().startsWith("["); i++) {
            String t = L.get(i).trim();
            int eq = t.indexOf('=');
            if (eq > 0 && t.substring(0, eq).trim().equals(key)) { L.set(i, key + " = " + val); return; }
        }
        L.add(s + 1, key + " = " + val);
    }
}
