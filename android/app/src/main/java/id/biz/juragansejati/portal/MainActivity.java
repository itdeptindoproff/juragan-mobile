package id.biz.juragansejati.portal;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Pendaftaran plugin harus dilakukan sebelum super.onCreate agar
        // jembatan Capacitor mengenalinya saat WebView dibuat.
        registerPlugin(FileMetaPlugin.class);

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
}
