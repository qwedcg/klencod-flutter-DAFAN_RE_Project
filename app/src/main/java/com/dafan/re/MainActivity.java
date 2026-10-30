package com.dafan.re;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 701;
    private LinearLayout root, content, top;
    private TextView fileName, status;
    private Uri selectedUri;
    private byte[] data;
    private String currentName = "لم يتم اختيار ملف";

    private int bg = 0xFF0B1020, panel = 0xFF151D32, accent = 0xFF35C8FF;
    private int text = 0xFFEAF2FF, muted = 0xFF9BAAC4;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(4, 5, 4, 5);
        return t;
    }

    private android.graphics.drawable.GradientDrawable shape(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private TextView button(String s, Runnable action) {
        TextView b = label(s, 14, text, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(14, 13, 14, 13);
        b.setBackground(shape(panel, 18));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 18, 16, 12); root.setBackgroundColor(bg);
        setContentView(root);

        TextView brand = label("DAFAN RE", 28, accent, true);
        root.addView(brand);
        TextView sub = label("REVERSE ENGINEERING STUDIO  •  MOBILE EDITION", 10, muted, true);
        root.addView(sub);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView open = button("＋  فتح ملف", () -> pickFile());
        actions.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        TextView clear = button("مسح التحليل", () -> {
            data = null; selectedUri = null; currentName = "لم يتم اختيار ملف";
            fileName.setText(currentName); status.setText("اختر ملفاً للبدء");
            showWelcome();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
        cp.setMargins(8,0,0,0); actions.addView(clear, cp);
        root.addView(actions);

        fileName = label(currentName, 13, text, true);
        fileName.setPadding(12, 12, 12, 12);
        fileName.setBackground(shape(panel, 14));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = 12; root.addView(fileName, fp);

        status = label("اختر ملف ELF أو SO أو APK أو DEX أو PE لتحليله", 12, muted, false);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = label("DAFAN • أدوات تحليل محلية على جهازك", 10, muted, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
        showWelcome();
    }

    private void showWelcome() {
        content.removeAllViews();
        addCard("مرحباً بك في DAFAN RE",
                "الإصدار الأول يوفر فحص ترويسة الملفات، واستخراج النصوص القابلة للقراءة، وعرض Hex، واستعراض محتويات APK/ZIP.\n\nاختر ملفاً من هاتفك للبدء.");
        addCard("صيغ يتعرف عليها",
                "ELF / SO  •  APK / ZIP  •  DEX  •  PE / EXE / DLL");
        addCard("ملاحظة الإصدار",
                "هذا إصدار تأسيسي للتحليل الثابت. تفكيك Assembly وعرض Pseudocode يحتاجان دمج محرك متخصص مثل Rizin أو Ghidra، وهما غير مضمّنين في هذا الإصدار.");
    }

    private void addCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14, 12, 14, 12);
        card.setBackground(shape(panel, 16));
        TextView h = label(title, 16, accent, true);
        TextView p = label(body, 13, text, false);
        card.addView(h); card.addView(p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 8, 0, 8); content.addView(card, lp);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, PICK_FILE);
    }

    @Override protected void onActivityResult(int req, int result, Intent intent) {
        super.onActivityResult(req, result, intent);
        if (req == PICK_FILE && result == RESULT_OK && intent != null) {
            selectedUri = intent.getData();
            try {
                getContentResolver().takePersistableUriPermission(selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            currentName = queryName(selectedUri);
            fileName.setText(currentName);
            loadSelected();
        }
    }

    private String queryName(Uri uri) {
        String name = "selected.bin";
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            try { if (c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) name = c.getString(ix);
            }} finally { c.close(); }
        }
        return name;
    }

    private void loadSelected() {
        try {
            InputStream in = getContentResolver().openInputStream(selectedUri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n; long total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 100L * 1024 * 1024) throw new IOException("حجم الملف أكبر من حد الإصدار الحالي (100 MB)");
                out.write(buf, 0, n);
            }
            in.close(); data = out.toByteArray();
            status.setText("تم تحميل الملف • " + data.length + " bytes");
            showAnalysis();
        } catch (Exception e) {
            status.setText("تعذر فتح الملف: " + e.getMessage());
        }
    }

    private String kind() {
        if (data == null || data.length < 4) return "ملف غير معروف";
        if (data[0] == 0x7f && data[1]=='E' && data[2]=='L' && data[3]=='F') return "ELF / SO";
        if (data[0]=='P' && data[1]=='K') return "ZIP / APK";
        if (data[0]=='d' && data[1]=='e' && data[2]=='x' && data[3]=='\n') return "DEX";
        if (data[0]=='M' && data[1]=='Z') return "PE / EXE / DLL";
        return "Binary / Data";
    }

    private void showAnalysis() {
        content.removeAllViews();
        addCard("نظرة عامة", "الاسم: " + currentName + "\nالنوع: " + kind() +
                "\nالحجم: " + data.length + " bytes (" + String.format(Locale.US, "%.2f", data.length/1024.0) + " KB)");
        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"معلومات", "Strings", "Hex", "APK"};
        Runnable[] jobs = {
            () -> showInfo(), () -> showStrings(), () -> showHex(), () -> showArchive()
        };
        for (int x=0;x<names.length;x++) {
            final int ix=x;
            TextView b=button(names[x], jobs[x]);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
            lp.setMargins(2,6,2,6); tabs.addView(b,lp);
        }
        content.addView(tabs);
        showInfo();
    }

    private void showInfo() {
        content.removeAllViews();
        addCard("معلومات الملف", "الاسم: "+currentName+"\nالنوع المكتشف: "+kind()+
                "\nالحجم: "+data.length+" bytes\nأول 16 بايت: "+hex(0, Math.min(16,data.length)));
        if ("ELF / SO".equals(kind())) addElfInfo();
        else if ("DEX".equals(kind())) addDexInfo();
        else if ("PE / EXE / DLL".equals(kind())) addPeInfo();
        else if ("ZIP / APK".equals(kind())) addCard("ملاحظة", "هذا أرشيف ZIP؛ افتح تبويب APK لاستعراض الملفات الموجودة داخله.");
        addNavButtons();
    }

    private void addNavButtons() {
        LinearLayout row = new LinearLayout(this);
        String[] n={"Strings","Hex","APK"};
        Runnable[] r={()->showStrings(),()->showHex(),()->showArchive()};
        for(int i=0;i<n.length;i++){
            final int j=i; TextView b=button(n[i],r[i]);
            row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        content.addView(row);
    }

    private void addElfInfo() {
        if (data.length < 20) return;
        int cls=data[4]&255, enc=data[5]&255;
        ByteOrder order=enc==2?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN;
        ByteBuffer b=ByteBuffer.wrap(data).order(order);
        int machine=b.getShort(18)&0xffff;
        String arch;
        switch(machine){case 3:arch="x86";break;case 40:arch="ARM";break;case 62:arch="x86-64";break;case 183:arch="AArch64 / ARM64";break;default:arch="Unknown ("+machine+")";}
        addCard("ELF Header", "Class: "+(cls==2?"64-bit":cls==1?"32-bit":"Unknown")+
                "\nEndian: "+(enc==2?"Big endian":"Little endian")+
                "\nMachine: "+arch+"\nEntry point: "+elfEntry(b,cls));
    }

    private String elfEntry(ByteBuffer b, int cls) {
        try {
            if(cls==2 && data.length>=32) return "0x"+Long.toHexString(b.getLong(24));
            if(cls==1 && data.length>=28) return "0x"+Long.toHexString(b.getInt(24)&0xffffffffL);
        } catch(Exception ignored) {}
        return "غير متاح";
    }

    private void addDexInfo() {
        if(data.length<112)return;
        String version=new String(data,4,3);
        long fileSize=le32(32), strings=le32(56), types=le32(64), methods=le32(88);
        addCard("DEX Header","Version: "+version+"\nHeader file size: "+fileSize+
                "\nString IDs: "+strings+"\nType IDs: "+types+"\nMethod IDs: "+methods);
    }

    private void addPeInfo() {
        if(data.length<64)return;
        long off=le32(60);
        if(off+6<=data.length){
            int machine=(data[(int)off+4]&255)|((data[(int)off+5]&255)<<8);
            String a=machine==0x8664?"x86-64":machine==0x14c?"x86":machine==0xaa64?"ARM64":"Unknown";
            addCard("PE Header","PE header offset: 0x"+Long.toHexString(off)+"\nMachine: "+a);
        }
    }

    private long le32(int p) {
        if(p<0||p+4>data.length)return 0;
        return (data[p]&255L)|((data[p+1]&255L)<<8)|((data[p+2]&255L)<<16)|((data[p+3]&255L)<<24);
    }

    private void showStrings() {
        content.removeAllViews();
        StringBuilder s=new StringBuilder(); int count=0;
        for(int i=0;i<data.length && count<2500;){
            int start=i;
            while(i<data.length && printable(data[i])) i++;
            if(i-start>=4){
                s.append(String.format(Locale.US,"%08X  ",start));
                s.append(new String(data,start,Math.min(i-start,300))).append('\n');
                count++;
            }
            i++;
        }
        addCard("Strings • "+count+" نتيجة", s.length()==0?"لم يتم العثور على نصوص واضحة":s.toString());
        addNavButtons();
    }

    private boolean printable(byte b) {
        int c=b&255; return c==9 || c==10 || c==13 || (c>=32 && c<=126);
    }

    private void showHex() {
        content.removeAllViews();
        StringBuilder s=new StringBuilder();
        int limit=Math.min(data.length,8192);
        for(int i=0;i<limit;i+=16){
            s.append(String.format(Locale.US,"%08X  ",i));
            for(int j=0;j<16;j++){
                if(i+j<limit)s.append(String.format(Locale.US,"%02X ",data[i+j]&255));
                else s.append("   ");
            }
            s.append(" |");
            for(int j=0;j<16 && i+j<limit;j++){
                int c=data[i+j]&255; s.append(c>=32&&c<127?(char)c:'.');
            }
            s.append("|\n");
        }
        addCard("Hex Viewer • أول "+limit+" bytes",s.toString());
        addNavButtons();
    }

    private void showArchive() {
        content.removeAllViews();
        if(!"ZIP / APK".equals(kind())){
            addCard("APK / ZIP Explorer","الملف المحدد ليس أرشيف ZIP.");
            addNavButtons(); return;
        }
        try {
            File tmp=File.createTempFile("dafan",".zip",getCacheDir());
            FileOutputStream o=new FileOutputStream(tmp); o.write(data); o.close();
            ZipFile z=new ZipFile(tmp);
            StringBuilder s=new StringBuilder(); int n=0;
            Enumeration<? extends ZipEntry> es=z.entries();
            while(es.hasMoreElements() && n<1500){
                ZipEntry e=es.nextElement();
                s.append(e.getName()).append(e.isDirectory()?"/":"");
                if(!e.isDirectory())s.append("   [").append(e.getSize()).append(" bytes]");
                s.append('\n'); n++;
            }
            z.close(); tmp.delete();
            addCard("محتويات الأرشيف • "+n+" عنصر",s.toString());
        }catch(Exception e){addCard("خطأ","تعذر قراءة الأرشيف: "+e.getMessage());}
        addNavButtons();
    }

    private String hex(int start,int len){
        StringBuilder s=new StringBuilder();
        for(int i=start;i<start+len;i++)s.append(String.format(Locale.US,"%02X ",data[i]&255));
        return s.toString();
    }
}
