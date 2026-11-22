# Activity Diagram Kurasi

Diagram ditulis dalam Mermaid (tampil otomatis di GitHub dan banyak editor Markdown). Untuk laporan, gambar ulang
dengan notasi UML activity diagram (lingkaran awal, kotak aksi, belah ketupat keputusan, lingkaran akhir),
atau ekspor langsung dari mermaid.live.

## 1. Membuat koleksi (template atau kustom)

```mermaid
flowchart TD
    A([Mulai]) --> B[Buka Kelola koleksi]
    B --> C[Tekan Koleksi baru]
    C --> D{Pilih awal}
    D -- Template --> E[Isi nama, warna, dan field dari template]
    D -- Kosong --> F[Isi nama dan warna sendiri]
    E --> G[Ubah, tambah, atau hapus field]
    F --> G
    G --> H{Nama terisi dan field valid?}
    H -- Tidak --> I[Tampilkan pesan kesalahan] --> G
    H -- Ya --> J[Simpan koleksi dan field ke SQLite]
    J --> K([Selesai])
```

## 2. Menambah item dan membuat kartu

```mermaid
flowchart TD
    A([Mulai]) --> B[Tekan tombol plus]
    B --> C[Pilih koleksi dan status]
    C --> D[Isi foto, nama, harga, kondisi, tanggal]
    D --> E[Form field dibuat otomatis dari field koleksi]
    E --> F[Hitung skor dan tier secara langsung]
    F --> G[Pratinjau kartu diperbarui]
    G --> H{Nama terisi?}
    H -- Tidak --> I[Tampilkan pesan] --> D
    H -- Ya --> J[Buat kode item otomatis]
    J --> K[Simpan item dan nilai field ke SQLite]
    K --> L{Wishlist dan harga di bawah target?}
    L -- Ya --> M[Kirim notifikasi harga turun]
    L -- Tidak --> N([Selesai])
    M --> N
```

## 3. Menghitung skor dan tier

```mermaid
flowchart TD
    A([Mulai]) --> B[Ambil nilai, kondisi, tanggal dimiliki]
    B --> C[Skor nilai: skala logaritmik 50 rb sampai 2,5 jt]
    C --> D[Skor kondisi: Baru 100, Mulus 85, Bagus 65, Bekas 40]
    D --> E[Skor usia: 25 poin per tahun, maksimal 100]
    E --> F[Skor = 60% nilai + 30% kondisi + 10% usia]
    F --> G{Skor}
    G -- 85 ke atas --> H[Legendary]
    G -- 70 sampai 84 --> I[Epic]
    G -- 50 sampai 69 --> J[Rare]
    G -- di bawah 50 --> K[Common]
    H --> L([Selesai])
    I --> L
    J --> L
    K --> L
```

## 4. Membagikan kartu

```mermaid
flowchart TD
    A([Mulai]) --> B[Buka halaman kartu]
    B --> C[Tekan Bagikan]
    C --> D[Gambar kartu di Canvas ukuran besar]
    D --> E[Simpan PNG ke folder cache]
    E --> F[Buat URI lewat FileProvider]
    F --> G[Tampilkan menu bagikan Android]
    G --> H([Selesai])
```

## 5. Membuat dan mencetak label QR

```mermaid
flowchart TD
    A([Mulai]) --> B[Buka halaman kartu, tekan Label QR]
    B --> C[Buat gambar QR berisi kurasi:KODE]
    C --> D[Pilih ukuran, isi label, dan cakupan]
    D --> E{Aksi}
    E -- Cetak --> F[Bangun PDF A4 berisi label]
    F --> G[Kirim ke PrintManager]
    E -- Bagikan PDF --> H[Bangun PDF A4 berisi label]
    H --> I[Simpan ke cache dan bagikan]
    G --> J([Selesai])
    I --> J
```

## 6. Scan QR dan mencatat peminjaman

```mermaid
flowchart TD
    A([Mulai]) --> B[Buka Scan]
    B --> C{Izin kamera diberikan?}
    C -- Tidak --> D[Minta izin] --> C
    C -- Ya --> E[Pindai QR]
    E --> F{QR berawalan kurasi: ?}
    F -- Tidak --> G[Tampilkan pesan bukan QR Kurasi] --> E
    F -- Ya --> H{Item ada di database?}
    H -- Tidak --> I[Tampilkan pesan tidak ditemukan] --> E
    H -- Ya --> J[Tampilkan ringkasan item]
    J --> K{Sedang dipinjam?}
    K -- Ya --> L[Tandai sudah kembali]
    K -- Tidak --> M[Isi peminjam dan lama pinjam]
    M --> N[Simpan peminjaman]
    L --> O([Selesai])
    N --> O
```

## 7. Menghitung dan menampilkan statistik

```mermaid
flowchart TD
    A([Mulai]) --> B[Buka Statistik]
    B --> C[Query total harga beli dan nilai sekarang]
    C --> D[Query pengeluaran per koleksi]
    D --> E[Hitung pengeluaran 6 bulan terakhir]
    E --> F[Bandingkan pengeluaran bulan ini dengan batas]
    F --> G[Tampilkan angka, bar, dan grafik]
    G --> H{Bagikan kartu statistik?}
    H -- Ya --> I[Gambar kartu statistik dan bagikan]
    H -- Tidak --> J([Selesai])
    I --> J
```

## 8. Wishlist dan notifikasi target harga

```mermaid
flowchart TD
    A([Mulai]) --> B[Tambah item berstatus Wishlist]
    B --> C[Isi harga sekarang dan target harga]
    C --> D[Simpan]
    D --> E[Pengguna memperbarui harga di kemudian hari]
    E --> F{Harga lebih kecil atau sama dengan target?}
    F -- Ya --> G[Kirim notifikasi harga turun]
    F -- Tidak --> H[Tetap di wishlist]
    G --> I{Sudah dibeli?}
    H --> I
    I -- Ya --> J[Pindahkan ke binder sebagai Dimiliki]
    I -- Tidak --> K([Selesai])
    J --> K
```

## 9. Pengingat harian (WorkManager)

```mermaid
flowchart TD
    A([Worker berjalan tiap 24 jam]) --> B{Notifikasi aktif?}
    B -- Tidak --> Z([Selesai])
    B -- Ya --> C[Cek pinjaman jatuh tempo besok atau terlambat]
    C --> D[Cek garansi 30 hari dan 7 hari sebelum habis]
    D --> E[Cek pengeluaran bulan ini terhadap batas 80% dan 100%]
    E --> F{Pengingat ini sudah dikirim?}
    F -- Ya --> G[Lewati]
    F -- Tidak --> H[Kirim notifikasi dan tandai terkirim]
    G --> Z
    H --> Z
```

## 10. Membuka aplikasi dengan PIN

```mermaid
flowchart TD
    A([Buka aplikasi]) --> B{PIN terpasang?}
    B -- Tidak --> G[Tampilkan Binder]
    B -- Ya --> C{Sudah terbuka dan ditinggal kurang dari 30 detik?}
    C -- Ya --> G
    C -- Tidak --> D{Biometrik aktif?}
    D -- Ya --> E[Tampilkan prompt sidik jari atau wajah]
    D -- Tidak --> F[Minta PIN 4 angka]
    E -- Berhasil --> G
    E -- Gagal atau pilih PIN --> F
    F --> H{PIN benar?}
    H -- Ya --> G
    H -- Tidak --> I[Tampilkan PIN salah] --> F
    G --> J([Selesai])
```
