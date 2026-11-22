# Pembaruan otomatis lewat GitHub Releases

Aplikasi mengecek rilis terbaru di GitHub, menawarkan pembaruan, mengunduh APK-nya, lalu menyerahkannya ke pemasang Android. Tidak perlu lagi memindahkan berkas debug ke HP setiap ada perubahan.

## Cara kerja di HP
1. Saat aplikasi dibuka (paling sering tiap 3 jam) dan lewat notifikasi harian, aplikasi memanggil `https://api.github.com/repos/<pemilik>/<repo>/releases/latest`.
2. Kalau `tag_name` (misalnya `v1.2`) lebih besar dari versi terpasang, muncul dialog "Versi 1.2 tersedia" beserta catatan rilis.
3. "Unduh dan pasang" mengunduh APK dengan progres, lalu membuka pemasang Android. Pemasangan pertama meminta izin "Izinkan dari sumber ini" sekali saja.
4. Pengaturan > Cek pembaruan melakukan hal yang sama secara manual.

## Cara menerbitkan versi baru (di komputer)
1. Naikkan `versionCode` dan `versionName` di `app/build.gradle`, commit.
2. Jalankan `powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Notes "apa yang berubah"`.
   Skrip membangun APK, menjalankan tes, membuat tag, mendorong ke GitHub, dan membuat rilis dengan APK terlampir.

## Hal penting
- **Tanda tangan harus sama.** Android hanya mau menimpa aplikasi dengan APK bertanda tangan sama. APK ini ditandatangani kunci debug di `%USERPROFILE%\.android\debug.keystore`. Cadangkan berkas itu. Kalau hilang atau build dilakukan di komputer lain dengan kunci berbeda, pembaruan akan ditolak dan aplikasi harus dihapus dulu sebelum dipasang ulang (data ikut hilang, jadi cadangkan lewat Pengaturan sebelumnya).
- **Versi harus naik.** `versionCode` yang lebih kecil ditolak Android, dan `versionName` harus lebih besar supaya aplikasi menganggapnya pembaruan.
- **Repo harus publik** karena aplikasi mengunduh tanpa login.
- **Izin internet.** Sebelum fitur ini aplikasi sepenuhnya offline. Sekarang aplikasi memakai internet hanya untuk dua hal: cek rilis di api.github.com dan unduh APK dari GitHub.
- **Versi lama tidak punya fitur ini.** APK versi 1.0 yang sudah terpasang di HP teman perlu dipasang manual sekali dengan versi 1.1. Setelah itu pembaruan berikutnya otomatis.

## Hasil uji ujung ke ujung (emulator Android 16)
- Pasang v1.1 resmi dari GitHub, terbitkan v1.2, buka Pengaturan > Cek pembaruan: dialog "Versi 1.2 tersedia" muncul dengan catatan rilis dan ukuran 7,6 MB. Unduhan berhasil dan ukurannya sama dengan di GitHub. Sheet izin pemasangan muncul, dan setelah izin dinyalakan pemasang Android menampilkan "Update this app?" (bukan pasang baru, jadi tanda tangan cocok).
- Pemasangan lewat pemasang sistem di emulator ditolak Google Play Protect (`INSTALL_FAILED_VERIFICATION_FAILURE`). File yang sama berhasil dipasang lewat `adb install -r`: versi naik ke 1.2, 11 kartu tetap utuh, dan folder unduhan dibersihkan otomatis saat aplikasi dibuka. Emulator tidak punya akses ke server verifikasi Google, jadi belum terbukti apakah HP asli menerimanya. Di HP asli Play Protect biasanya hanya menampilkan "Scan app?", lalu pasang.
- Kalau HP teman menolak dengan pesan serupa: ketuk "Install anyway" atau "More details", atau matikan sementara Play Protect.
- Cek otomatis saat aplikasi dibuka dibatasi tiap 3 jam, jadi saat menguji pakai Pengaturan > Cek pembaruan.
- Emulator perlu `-dns-server 8.8.8.8` kalau DNS-nya tidak jalan.
