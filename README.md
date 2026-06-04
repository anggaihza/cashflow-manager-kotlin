# BizTrack

Aplikasi Android native untuk pencatatan keuangan bisnis kecil/UMKM. Aplikasi berjalan offline-first, tanpa login, tanpa backend, dan menyimpan data di perangkat.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Room Database
- DataStore Preferences
- MVVM + Repository
- Export CSV dan backup/restore JSON lokal

## Fitur MVP

- Onboarding nama bisnis, mata uang, akun kas pertama, saldo awal, dan dark mode
- Dashboard ringkasan pemasukan, pengeluaran, profit, saldo kas, grafik sederhana, kategori pengeluaran terbesar, dan transaksi terbaru
- Tambah, edit, detail, hapus transaksi pemasukan/pengeluaran
- Saldo akun kas otomatis bertambah/berkurang sesuai transaksi
- List transaksi dengan search dan filter tipe transaksi
- Kelola kategori transaksi
- Kelola akun kas/dompet
- Laporan bulanan dengan export CSV dan share file
- Backup JSON lokal dan restore dari file
- Pengaturan nama bisnis, mata uang, dark mode, dan hapus semua data

## Build

Project memakai Gradle wrapper. Di mesin ini build berhasil dengan JBR Android Studio:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

APK debug dibuat di:

```text
app/build/outputs/apk/debug/app-debug.apk
```
