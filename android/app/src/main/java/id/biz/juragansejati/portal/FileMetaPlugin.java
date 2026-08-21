package id.biz.juragansejati.portal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pemilih gambar yang sekaligus membaca tanggal berkas dari perangkat.
 *
 * Latar belakang: input berkas biasa di WebView hanya memberi isi gambar, nama,
 * dan satu waktu (waktu ubah). Tanggal pembuatan tidak ikut terkirim, padahal
 * file manager menampilkannya. Plugin ini membaca tanggal itu langsung dari
 * penyimpanan perangkat lewat ContentResolver, lalu mengirimkannya ke halaman
 * web bersama isi gambarnya.
 *
 * Kolom tanggal yang dicari (bergantung sumber gambar):
 *   date_taken    - waktu foto diambil kamera
 *   date_added    - waktu berkas masuk ke penyimpanan perangkat
 *   date_modified - waktu berkas terakhir diubah
 *   last_modified - versi kolom untuk berkas dari penyimpanan dokumen
 *
 * Kolom dibaca dengan menelusuri nama kolom yang benar-benar ada pada hasil
 * query, bukan diminta di awal, karena tiap sumber gambar menyediakan kolom
 * yang berbeda dan meminta kolom yang tidak ada akan menggagalkan query.
 */
@CapacitorPlugin(name = "FileMeta")
public class FileMetaPlugin extends Plugin {

	private static final long BATAS_BYTE = 12L * 1024 * 1024;

	/** Buka galeri, kembalikan isi gambar beserta tanggal berkasnya. */
	@PluginMethod
	public void pickImage(PluginCall call) {
		// setDataAndType, bukan setType: memanggil setType sesudahnya akan
		// menghapus URI galeri yang baru saja dipasang.
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
		startActivityForResult(call, intent, "hasilPilihGambar");
	}

	@ActivityCallback
	private void hasilPilihGambar(PluginCall call, ActivityResult result) {
		if (call == null) {
			return;
		}
		if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
			JSObject batal = new JSObject();
			batal.put("dibatalkan", true);
			call.resolve(batal);
			return;
		}

		Uri uri = result.getData().getData();
		if (uri == null) {
			call.reject("Gambar tidak terbaca");
			return;
		}

		try {
			JSObject hasil = bacaKeterangan(uri);
			hasil.put("data", bacaIsiBase64(uri));
			hasil.put("dibatalkan", false);
			call.resolve(hasil);
		} catch (Exception e) {
			call.reject("Gagal membaca gambar: " + e.getMessage(), e);
		}
	}

	/** Ambil nama, ukuran, jenis, dan tanggal-tanggal berkas dari perangkat. */
	private JSObject bacaKeterangan(Uri uri) {
		JSObject out = new JSObject();
		ContentResolver resolver = getContext().getContentResolver();
		out.put("mimeType", resolver.getType(uri) == null ? "image/jpeg" : resolver.getType(uri));

		Cursor c = null;
		try {
			c = resolver.query(uri, null, null, null, null);
			if (c != null && c.moveToFirst()) {
				for (String kolom : c.getColumnNames()) {
					String nama = kolom.toLowerCase(Locale.ROOT);
					int i = c.getColumnIndex(kolom);
					if (i < 0 || c.isNull(i)) {
						continue;
					}
					switch (nama) {
						case "_display_name":
						case "display_name":
							out.put("name", c.getString(i));
							break;
						case "_size":
						case "size":
							out.put("size", c.getLong(i));
							break;
						case "datetaken":
						case "date_taken":
							out.put("dateTaken", keWaktu(c.getLong(i)));
							break;
						case "date_added":
							// MediaStore menyimpan date_added dalam DETIK.
							out.put("dateAdded", keWaktu(c.getLong(i) * 1000L));
							break;
						case "date_modified":
							// MediaStore menyimpan date_modified dalam DETIK.
							out.put("dateModified", keWaktu(c.getLong(i) * 1000L));
							break;
						case "last_modified":
							// Penyimpanan dokumen memakai milidetik.
							out.put("lastModified", keWaktu(c.getLong(i)));
							break;
						default:
							break;
					}
				}
			}
		} catch (Exception ignored) {
			// Sumber gambar tidak menyediakan keterangan; isi gambar tetap dikirim.
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return out;
	}

	private String bacaIsiBase64(Uri uri) throws Exception {
		InputStream in = getContext().getContentResolver().openInputStream(uri);
		if (in == null) {
			throw new IllegalStateException("berkas tidak dapat dibuka");
		}
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] potong = new byte[8192];
			long total = 0;
			int n;
			while ((n = in.read(potong)) != -1) {
				total += n;
				if (total > BATAS_BYTE) {
					throw new IllegalStateException("ukuran gambar melebihi batas");
				}
				buf.write(potong, 0, n);
			}
			return Base64.encodeToString(buf.toByteArray(), Base64.NO_WRAP);
		} finally {
			in.close();
		}
	}

	/** Milidetik sejak 1970 menjadi "YYYY-MM-DD HH:MM:SS" waktu setempat. */
	private String keWaktu(long milidetik) {
		if (milidetik <= 0) {
			return "";
		}
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		return f.format(new Date(milidetik));
	}
}
