# Kurasi

Aplikasi Android untuk mengoleksi apa saja. Setiap item otomatis menjadi **kartu koleksi** dengan tier kelangkaan
(Common, Rare, Epic, Legendary), punya **label QR** untuk ditempel di barang aslinya, dan bisa **dipinjamkan** lewat
scan QR. Dilengkapi statistik pengeluaran, wishlist dengan target harga, pencapaian, serta catatan nilai, nota, dan garansi.

Dibuat sebagai proyek akhir mata kuliah Mobile Programming. Native Android, Java + XML, database SQLite, tanpa
backend. Internet hanya dipakai untuk mengecek dan mengunduh pembaruan dari GitHub Releases (lihat `docs/PEMBARUAN.md`).

## Fitur

| Fitur | Penjelasan |
|---|---|
| Koleksi template dan kustom | Pakai template (Sneakers, Buku, Tanaman, Vinyl, Kamera) atau rancang sendiri. Tiap koleksi punya field sendiri: teks, angka, tanggal, pilihan. |
| Kartu bertier | Skor 0-100 dihitung dari nilai (60%), kondisi (30%), dan lama dimiliki (10%). Tier: Common, Rare (50+), Epic (70+), Legendary (85+). |
| Bagikan kartu | Kartu digambar di Canvas lalu diekspor jadi PNG. |
| Label QR | Stiker QR ukuran kecil/sedang/besar. Cetak langsung atau bagikan PDF A4 berisi banyak label. |
| Scan dan pinjam | Scan QR di barang untuk membuka kartunya atau mencatat peminjaman dan pengembalian. |
| Statistik | Total pengeluaran, nilai sekarang, per koleksi, grafik 6 bulan, batas bulanan, dan kartu statistik untuk dibagikan. |
| Wishlist | Barang incaran dengan target harga. Notifikasi saat harga di bawah target. |
| Pencapaian | Level kolektor (XP), lencana, dan progres set (misalnya satu seri buku). |
| Nilai, nota, garansi | Riwayat nilai item, foto nota, dan hitung mundur garansi dengan pengingat. |
| Pengingat | Pinjaman jatuh tempo, garansi hampir habis, dan batas pengeluaran bulanan (WorkManager, harian). |
| Keamanan | Kunci PIN 4 angka dan sidik jari atau wajah. |
| Data | Ekspor CSV, cadangan dan pemulihan ZIP (data beserta foto; cadangan JSON lama tetap bisa dipulihkan), widget layar utama, data contoh untuk demo. |

## Teknologi

- Java 17, XML View (tanpa Compose), Material 3 sebagai dasar, gaya neobrutalism dibuat sendiri (drawable XML + Canvas)
- SQLite (`SQLiteOpenHelper`), 7 tabel dengan relasi `ON DELETE CASCADE`
- ZXing (`core` untuk membuat QR, `zxing-android-embedded` untuk memindai)
- WorkManager untuk pengingat harian, BiometricPrompt untuk kunci
- Font Bebas Neue dan JetBrains Mono (lisensi OFL), disertakan di `res/font`

## Struktur

```
app/src/main/java/id/kurasi/app/
  data/    Models, Db (skema), Repo (semua query), Templates, SampleData
  ui/      Activity per layar, Dock, NeoSheet, LoanSheet, Lock
  ui/view/ CardRenderer (kartu di Canvas), CardView, WordmarkView
  util/    Rarity (rumus skor), QrUtil, LabelPdf, StatsCard, CsvExport, ImageStore, Fmt, Neo, Prefs
  notify/  Notifier, ReminderWorker
  widget/  KurasiWidget
docs/      Activity diagram dan catatan rancangan
```

## Skema database

```
collections(_id, name, color, template, prefix, position)
field_defs(_id, collection_id, name, type, options, position)
items(_id, collection_id, code, name, photo, price, value, condition, acquired_at, status,
      target_price, note, score, set_id, store, receipt_photo, warranty_until, created_at)
item_values(item_id, field_id, value)        -- nilai field dinamis tiap item
value_history(_id, item_id, value, at)
loans(_id, item_id, borrower, loaned_at, due_at, returned_at)
sets(_id, name, target_count)
```

## Cara build

Butuh Android Studio (atau JDK 17+ dan Android SDK 36).

```
./gradlew assembleDebug
```

APK ada di `app/build/outputs/apk/debug/app-debug.apk`. Pertama kali dibuka, aplikasi membuat empat koleksi bawaan.
Untuk mencoba semua fitur, buka Menu, Pengaturan, lalu Muat data contoh.

## Catatan rancangan

- Kode item (`SNK-014`) dibuat otomatis dari awalan koleksi. Isi QR berformat `kurasi:SNK-014`.
- Skor tier dihitung ulang setiap item disimpan atau nilainya diperbarui.


## Hak cipta

© 2026 Kaka Anditya Pambayung. Semua hak dilindungi. Kode dipublikasikan agar bisa dilihat dan dipelajari; menyalin, mengubah, atau membagikan ulang memerlukan izin tertulis pemilik. Rincian ada di berkas [LICENSE](LICENSE).
