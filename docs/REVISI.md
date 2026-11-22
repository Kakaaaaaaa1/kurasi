# Catatan revisi Kurasi

## Sudah diperbaiki (22 Nov, setelah audit kode)

- [x] Kolom teks tidak bisa diketik. Penyebab: `EditText` dibuat dengan `defStyleAttr = 0`, jadi tidak mewarisi `focusableInTouchMode`. Perbaikan: `Neo.Input` sekarang berinduk `android:Widget.EditText` (`values/styles.xml`).
- [x] Aplikasi crash saat mengetik harga di atas 999. Penyebab: `MoneyWatcher` memanggil `setSelection()` dengan panjang teks yang salah karena kolom angka membuang titik. Perbaikan: titik diizinkan lewat `DigitsKeyListener` dan teks diganti langsung di `Editable`.
- [x] Tombol Simpan di form item menutupi kolom saat keyboard terbuka. Sekarang berada di luar area gulir.
- [x] Scan QR lalu "Pinjamkan" bisa membuka dua sheet karena pemindai aktif lagi. Sekarang pemindai berhenti sampai sheet peminjaman ditutup.
- [x] Terverifikasi di emulator: tambah item (11 jadi 12 kartu), harga terformat 2.850.000, ubah nama item, tanpa crash.

### Audit kedua (lint + baca ulang kode)

- [x] Foto dari galeri/kamera diproses di thread utama (bisa membuat layar membeku untuk foto besar). Sekarang di thread terpisah, di form item dan di layar Nilai dan nota.
- [x] Ketuk foto di form item untuk menghapusnya.
- [x] Item yang dipindah dari Wishlist ke Dimiliki sekarang langsung punya catatan nilai pertama (grafik nilai tidak kosong).
- [x] Kode item dibuat dengan `Locale.ROOT` supaya angkanya selalu 0-9 di semua bahasa perangkat.
- [x] Semua layar dikunci portrait supaya isian form tidak hilang saat HP diputar.
- [x] Terverifikasi di emulator: catat peminjaman (status "Dipinjam ... kembali 29 Nov"), sheet dengan keyboard terbuka, buat koleksi baru dengan field.
- Lint: 2 "error" (I/O di thread utama, sudah diperbaiki) dan 169 peringatan gaya (teks hardcoded, SetTextI18n, versi library). Tidak ada yang bisa membuat crash.

Dicatat 22 Nov 2026 dini hari, untuk dikerjakan besok pagi.

## Perlu diuji (belum sempat dicoba di emulator)

- [ ] Form tambah item sampai tersimpan: isi nama, harga, kondisi, field koleksi, lalu simpan dan cek kartunya muncul di Binder.
- [ ] Ubah item dan hapus item.
- [ ] Foto dari galeri untuk item (tombol Galeri) dan dari kamera (tombol Kamera).
- [ ] Foto nota dari galeri dan kamera di layar Nilai dan nota.
- [ ] Scan QR sungguhan: cetak atau tampilkan label di layar lain, scan dengan HP asli.
- [ ] Catat peminjaman dan pengembalian lewat hasil scan.
- [ ] Cetak label dan bagikan PDF.
- [ ] Bagikan kartu dan kartu statistik sebagai gambar.
- [ ] Editor koleksi: koleksi baru dari template, koleksi kosong, tambah field tiap tipe, hapus koleksi.
- [ ] Wishlist: tambah barang, ubah harga di bawah target (notifikasi), tandai sudah dibeli.
- [ ] Set: buat set, pilih set di form item, cek progres di Pencapaian.
- [ ] Batas bulanan di Statistik dan tombol "Cek pengingat sekarang" di Pengaturan.
- [ ] Notifikasi (izin Android 13+), widget layar utama.
- [ ] Ekspor CSV, cadangan JSON, dan pulihkan dari cadangan.
- [ ] Pasang PIN dari Pengaturan, sidik jari atau wajah, hapus PIN.
- [ ] Semua di atas di HP asli, bukan hanya emulator.

## Sudah diketahui, perlu dibenahi

- Data contoh memakai foto placeholder. Perlu foto asli untuk screenshot dan video demo.
- Belum ada tombol hapus atau ganti foto setelah foto dipilih di form (hanya bisa memilih foto baru).
- Tombol menu "Wishlist", "Pencapaian", "Kelola koleksi", dan "Pengaturan" ada di ikon menu pojok kanan atas Binder. Mockup memakai dock saja, jadi cek apakah letaknya sudah nyaman.
- APK masih versi debug. Untuk setor, debug sudah cukup. Versi release butuh tanda tangan (keystore).

## Pertanyaan: foto dari galeri

Sudah ada. Setiap item punya foto sendiri: di form item ada tombol Galeri dan Kamera. Nota pembelian juga punya foto sendiri di layar Nilai dan nota. Yang belum teruji adalah alurnya di perangkat asli, dan belum ada opsi menghapus foto.

## Untuk setor ke web dosen

- [ ] Rekam video demo singkat.
- [ ] Ambil screenshot sendiri (sudah ada 9 di `docs/screenshots` dari emulator).
- [ ] Gambar ulang activity diagram dengan notasi UML (isi dari `docs/activity-diagrams.md`).
- [ ] Isi Nama, Nama App, dan Deskripsi di form.

## Fitur tambahan (sudah dikerjakan dan dicek di emulator)

- Pencarian di Binder (nama atau kode) dan urutan: Terbaru, Skor tertinggi, Harga tertinggi, Nama A-Z. Ada pesan "tidak ada kartu yang cocok".
- Riwayat pinjam per item di layar kartu (5 terakhir, tag DIPINJAM / KEMBALI / TERLAMBAT).
- Mode gelap. Pilihan di Pengaturan: Ikuti sistem, Terang, Gelap. Sudah dicek di Binder, Kartu, Form, Statistik, Wishlist, Pencapaian, Pengaturan, Label QR, dan Tutorial. QR dan gambar ekspor tetap warna terang supaya terbaca.
- Tutorial 4 langkah saat pertama dibuka, bisa diulang dari Pengaturan > Lihat tutorial.

Perlu dites di HP asli: mode gelap di layar Koleksi dan editor koleksi, sheet bawah (pinjam, nilai), layar Kunci PIN, dan layar Scan.

## Audit copywriting dan teks panjang (sudah dikerjakan)

- Istilah "field" diganti "detail item" di editor koleksi, disertai penjelasan, contoh (Penulis, Tahun terbit, Ukuran, Aroma), label "Isinya berupa", dan keterangan tiap jenis isian (Tulisan, Angka, Tanggal, Pilihan).
- Label di form item tidak lagi menampilkan jenis teknis ("penulis · teks"), cukup nama detailnya.
- Sapaan "lu" diganti "kamu" di seluruh teks aplikasi.
- Pesan kosong, galat, dan izin dibuat lebih ramah dan memberi tahu langkah berikutnya (scan QR, kamera, nama kosong, set, statistik).
- Penjelasan skor ditambahkan di tutorial dan di form item.
- Nilai kotak statistik kartu yang terlalu panjang (misalnya nama penulis) diperkecil lalu dipotong dengan "…". Isi lengkap ada di panel Rincian. Nama koleksi panjang di gambar kartu statistik juga dipotong.

## Perbaikan kenyamanan (sudah dikerjakan)

- Form item dan editor koleksi menanyakan "Buang perubahan?" kalau tombol kembali ditekan setelah ada isian yang belum disimpan. Tanpa perubahan, langsung menutup.
- Hapus set dan hapus dari wishlist sekarang meminta konfirmasi. Hapus set menegaskan bahwa item tetap ada.
- Form item memberi petunjuk "Harga belum diisi, jadi skor masih rendah" kalau harga masih 0.
- Simpan koleksi memberi tahu kalau ada detail tanpa nama yang tidak ikut disimpan.
- Pemindai QR punya tombol senter (hanya muncul di HP yang punya lampu kilat). Belum bisa dites di emulator.
- Muat data contoh tidak lagi menambah item ganda kalau data contoh sudah ada.


## Perbaikan besar kelemahan (sudah dikerjakan dan dites di emulator)

- Cadangan sekarang berupa ZIP berisi data.json dan folder photos/, jadi foto item dan foto nota ikut tersalin. Cadangan JSON lama tetap bisa dipulihkan. Diuji: foto dipilih dari galeri, dicadangkan, berkas foto dihapus dari penyimpanan app, dipulihkan, lalu foto kembali dan tampil di kartu. Foto sisa data lama dibersihkan saat memulihkan.
- Nilai koleksi di Statistik, widget, dan kartu statistik tidak lagi menghitung item yang sudah Terjual. Persentase naik/turun dibandingkan dengan modal barang yang masih dimiliki. Total pengeluaran tetap menghitung semuanya. Diuji: menandai satu item Terjual menurunkan nilai koleksi tepat sebesar nilai item itu.
- Pencarian di Binder sekarang ikut mencari di catatan dan detail buatan pengguna (misalnya nama penulis atau artis), kecuali detail bertipe tanggal. Diuji dengan "Herbert" dan "Miles".
- Kartu punya deskripsi untuk pembaca layar (TalkBack), misalnya "Kartu Kind of Blue, pressing 1959, EPIC, skor 80, ...". Diuji lewat hierarki aksesibilitas, belum dengan TalkBack sungguhan.
- Foto dari galeri sudah teruji di emulator (sebelumnya belum).

Masih terbuka: skor kelangkaan hanya dari harga, nilai sekarang masih manual, teks belum di strings.xml, belum ada tes otomatis, harga jual item terjual belum dicatat, dan sentuhan getar belum ada.

## Uji layar kecil dan font besar (360dp, font 1,3x) dan perbaikannya

- Hint pencarian Binder dipersingkat ("Cari nama atau detail") karena terpotong di layar sempit.
- Label "LEGENDARY" di kartu Statistik tidak lagi patah menjadi dua baris. Ukurannya menyesuaikan lebar.
- Tag Wishlist "DI BAWAH TARGET" diganti "TERCAPAI" supaya tidak menjepit nama barang.
- Baris warna di editor koleksi sekarang bisa digeser. Sebelumnya warna ke-7 (kuning) terpotong dan tidak bisa dipilih di layar sempit.
- Kolom isian di form item memakai tombol "Next" di keyboard.
- Tombol aplikasi memberi getar halus saat disentuh.
- Layar yang sudah dicek di 360dp: Binder, Kartu, Form item, Statistik, Wishlist, Pencapaian, Pengaturan, editor koleksi, Scan.

## Lint (sudah dikerjakan)

- Lint: 0 error. Semua `toUpperCase()` dan `toLowerCase()` kini memakai `Locale.ROOT`, begitu juga pembuatan hash PIN, supaya teks tidak rusak di HP berbahasa Turki (i menjadi İ) atau berangka non-Latin. Peringatan DefaultLocale turun dari 30 menjadi 0.
- Sisa peringatan non-kritis: teks tertulis langsung di kode (SetTextI18n, HardcodedText), orientasi dikunci portrait, beberapa warna dan ikon belum terpakai.

## Fitur besar: harga jual, tier manual, tes otomatis (sudah dikerjakan)

- Harga jual untuk item Terjual (opsional) di form item. Kartu menampilkan "Terjual Rp ... (untung/rugi Rp ...)". Statistik punya panel "Hasil penjualan" yang menjumlah untung/rugi dari item yang harga jualnya dicatat. Kolom `harga_jual` ikut di ekspor CSV.
- Tingkat kelangkaan bisa diatur manual (Otomatis, Common, Rare, Epic, Legendary) di form item. Skor dijepit ke rentang tier pilihan, sehingga warna kartu, statistik, urutan, dan hitungan di Binder ikut berubah.
- Database naik ke versi 2 dengan migrasi `ALTER TABLE` (kolom sold_price dan tier_override). Diuji: app versi lama dengan 34 item dipasang-ulang ke versi baru, semua data dan foto tetap utuh, tanpa error SQL.
- 9 tes otomatis (JUnit) untuk rumus skor, batas tier, dan override. Jalankan dengan `gradlew testDebugUnitTest`.
- Cadangan ZIP memuat dua kolom baru otomatis (dump memakai SELECT *). Cadangan lama (tanpa kolom itu) tetap bisa dipulihkan karena kolom baru punya nilai bawaan.

## Dock kaca (liquid glass)

- Dock bawah (Binder, Scan, Statistik, tambah) sekarang berbahan kaca: `ui/view/GlassDock.java`. Isi halaman di belakangnya diburamkan sungguhan (Android 12 ke atas, lewat RenderEffect), diberi tint warna kertas, kilau tipis di atas, tepi dalam terang, dan tepi tinta tipis. Warna ikon, tombol aktif (hitam), dan tombol + (lime) tidak berubah.
- Android 11 ke bawah tidak punya blur ini, jadi dock memakai tint yang lebih pekat (hampir solid) supaya tetap terbaca.
- Blur dihitung ulang saat Binder atau Statistik di-scroll. Diuji di emulator Android 16 mode terang dan gelap. Belum diuji di HP asli, jadi perlu dicek apakah scroll tetap lancar.

## Dock bisa digeser (gaya tab bar iOS)

- Lingkaran indikator tab aktif sekarang "lensa" yang bisa digeser dengan jari. Saat digeser, lensa membesar sedikit dan mengikuti jari, ikon di bawahnya berganti warna, dan ada getar tipis tiap melewati tab. Saat dilepas, lensa menempel ke tab terdekat dengan sedikit membal, lalu layar berpindah. Ketuk biasa tetap bisa dan lensa meluncur ke tab yang diketuk.
- Setelah kembali dari layar lain (misalnya Scan lewat tombol kembali), lensa pulang ke tab milik layar itu. Bug ketuk-tidak-membuka-apa-apa setelah kembali sudah diperbaiki.
- Tombol + tetap terpisah dan tidak ikut digeser.
- Pendengar klik pada ikon tetap ada supaya TalkBack tetap bisa memakai dock.
- Diuji di emulator: ketuk, kembali lalu ketuk lagi, geser dari Binder ke Statistik, dan tahan di tengah geseran. Belum diuji di HP asli.

## Tutorial bisa digeser dan ada tombol Kembali

- Tutorial memakai `ViewPager2`: halaman mengikuti jari saat digeser kiri dan kanan.
- Tombol "‹ KEMBALI" di kiri atas (muncul mulai halaman 2), tombol LEWATI di kanan atas, tombol bawah "Lanjut", dan titik halaman yang bisa diketuk untuk lompat.
- Tombol kembali sistem mundur satu halaman dulu, baru keluar dari tutorial.
- Isi halaman diletakkan di tengah layar. Di layar kecil atau font besar, isi halaman bisa digulir.
- Bug yang ketemu saat tes: pada pembukaan pertama, titik halaman dan tombol bawah tidak tergambar sampai halaman digeser. Diperbaiki dengan membuat ulang keduanya sekali setelah layout pertama. Penyebab pastinya belum diketahui, jadi ini perbaikan yang menutupi gejala.
- Diuji: buka dari Pengaturan, pemakai baru (data dihapus lalu tutorial muncul otomatis), geser kiri dan kanan, tombol Kembali, tombol sistem Kembali, sampai "Mulai kosong".

## Halaman ikut bergeser saat dock digeser (satu pager)

- Binder, Scan, dan Statistik sekarang tiga halaman dalam satu `ViewPager2` (`BinderActivity` jadi host, isi layarnya jadi `BinderFragment`, `ScanFragment`, `StatsFragment`). Dock kaca ada di host, di atas pager.
- Menggeser lensa dock menggeser halamannya secara langsung (fake drag): Binder terdorong ke kiri dan halaman berikutnya masuk dari kanan, mengikuti jari. Sebaliknya, menggeser halaman dengan jari menggerakkan lensa. Mengetuk tab menggeser halaman dengan animasi.
- Kamera di halaman Scan hanya hidup saat halaman itu aktif. Tombol kembali dari Scan atau Statistik pulang ke Binder dulu. Tombol X di Scan juga pulang ke Binder.
- Layar Scan sekarang ikut punya dock (dulu layar penuh tanpa dock), jadi bisa digeser balik. Tombol senter dinaikkan di atas dock.
- Diuji di emulator: geser lensa sebagian lalu lepas, ketuk tiap tab, geser halaman dengan jari, tombol kembali, buka kartu lalu kembali, dock bersembunyi saat mengetik di kolom cari. Lint 0 error, 9 tes unit lolos.
- Perubahan ini cukup besar. Yang paling perlu dicek di HP asli: kamera Scan saat pertama digeser masuk (izin kamera), dan kehalusan geseran.

## Pembaruan dari dalam aplikasi (versi 1.1)

- Aplikasi mengecek GitHub Releases, menawarkan versi baru, mengunduh dengan progres, dan menyerahkan ke pemasang Android. Detail dan cara menerbitkan rilis ada di `docs/PEMBARUAN.md`. Skrip: `tools/release.ps1`.
- Cek otomatis saat aplikasi dibuka (maksimal tiap 3 jam), notifikasi harian "Kurasi versi X tersedia", dan Pengaturan > Cek pembaruan.
- Tes unit untuk perbandingan versi dan pembacaan jawaban GitHub (6 tes). Total 15 tes.
- Versi naik ke 1.1 (versionCode 2). Aplikasi kini memakai izin INTERNET dan REQUEST_INSTALL_PACKAGES.
- Belum diuji ujung ke ujung karena repo GitHub belum dibuat: alamat repo di `Updater.REPO` sudah `Kakaaaaaaa1/kurasi`.

## Hak cipta

- Berkas `LICENSE`: © 2026 Kaka Anditya Pambayung, semua hak dilindungi (kode boleh dilihat dan dipelajari, menyalin atau membagikan ulang perlu izin tertulis). Bagian "Hak cipta" ditambahkan di README. Pengaturan > Tentang menampilkan "© 2026 KAKA ANDITYA PAMBAYUNG".
- README tidak lagi menulis "tanpa internet": internet dipakai untuk cek dan unduh pembaruan.
- Perubahan ini baru ada di repo. APK 1.1 yang sudah dibagikan belum memuat baris hak cipta di Pengaturan. Baris itu ikut di rilis berikutnya (1.2).
