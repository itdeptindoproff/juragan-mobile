package id.biz.juragansejati.portal;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.CapConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Navigation bar (taskbar) dibuat menyatu dengan aplikasi.
 *
 * Bawaannya Android menaruh scrim hitam tipis di belakang navigation bar saat
 * aplikasi menggambar edge-to-edge — khusus mode navigasi 3-tombol, supaya
 * ikonnya tetap terbaca di atas konten apa pun. Scrim itu tidak peduli tema
 * sistem maupun warna aplikasi, dan tidak bisa disentuh dari CSS/WebView.
 *
 * Catatan versi: targetSdk 36, jadi setNavigationBarColor() sudah DIABAIKAN di
 * Android 15+ (API 35) — warnanya dipaksa transparan oleh sistem. Pemanggilan di
 * bawah hanya berguna untuk Android 14 ke bawah yang masih menghormatinya.
 *
 * Karena tampilan web-nya selalu terang (belum ada mode gelap), ikon navigation
 * bar dibuat GELAP secara tetap — bukan mengikuti tema sistem. Kalau ikut tema,
 * di mode gelap ikonnya jadi putih di atas latar aplikasi yang putih, alias
 * hilang. Begitu web-nya punya mode gelap, ganti `true` di bawah menjadi
 * pengecekan Configuration.UI_MODE_NIGHT_MASK.
 */
public class MainActivity extends BridgeActivity {

    /**
     * Android memuat portal LANGSUNG dari server produksi, jadi update tampilan
     * di server langsung sampai ke HP tanpa upload ke Google Play. iOS tetap
     * memakai bundel lokal www/ (aplikasi yang hanya membuka situs berisiko
     * ditolak App Review Apple). Perubahan native (plugin, izin, App Links)
     * tetap butuh rilis baru di Google Play.
     */
    private static final String SERVER_URL = "https://app.juragansejati.biz.id/portal/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Harus sebelum super.onCreate(): BridgeActivity.load() memakai `config`.
        config = configDariServer();
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        // Konten digambar sampai ke belakang system bar; inset dilaporkan ke
        // WebView sehingga env(safe-area-inset-*) di CSS terisi.
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Android 14 ke bawah: transparankan bar-nya sendiri.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        // Inti perbaikan: matikan scrim otomatis di mode 3-tombol (API 29+).
        // Tanpa ini, bar tetap hitam tipis walau warnanya sudah transparan.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }

        // Ikon gelap — cocok di atas latar aplikasi yang terang.
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightNavigationBars(true);
        controller.setAppearanceLightStatusBars(true);
    }

    /**
     * capacitor.config.json hasil `cap sync` (dipakai bersama iOS) ditambah
     * server.url. Gagal dibaca → null → Capacitor memakai config bawaan
     * (bundel lokal), jadi aplikasi tetap bisa dibuka.
     */
    private CapConfig configDariServer() {
        try (InputStream in = getAssets().open("capacitor.config.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            JSONObject json = new JSONObject(out.toString("UTF-8"));
            JSONObject server = json.optJSONObject("server");
            if (server == null) {
                server = new JSONObject();
            }
            server.put("url", SERVER_URL);
            json.put("server", server);

            File dir = new File(getCacheDir(), "capconfig");
            dir.mkdirs();
            try (FileOutputStream f = new FileOutputStream(new File(dir, "capacitor.config.json"))) {
                f.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
            return CapConfig.loadFromFile(this, dir.getAbsolutePath());
        } catch (IOException | JSONException e) {
            return null;
        }
    }
}
