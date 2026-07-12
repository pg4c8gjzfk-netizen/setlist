# Setlist Studio

XLSXの演目一覧から、公演ごとの香盤表を編集・再生成・保存するWindowsデスクトップアプリです。各ワークシートを独立した公演として扱い、シート間で演目を移動・混在させません。CSVには対応していません。

## 基本操作

1. `SetlistStudio.exe`を起動します。
2. `XLSXを選択`から入力ファイルを指定します。
3. 元の曲順を使う場合は`編集を開始`、シート内で並べ直す場合は`生成`を選びます。
4. 編集画面で曲順、時間、出演者、固定状態を調整します。
5. 後日再編集する場合は`編集状態を保存`、配布する場合は`配布用XLSX出力`を選びます。

画面右上の`ダーク表示`／`ライト表示`で配色を切り替えられます。選択した配色は次回起動時にも引き継がれます。

配布用XLSXには、演者別の○列、各演目の出演人数、総時間、各演者の出演回数が出力されます。印刷設定はA4横向き・1ページ幅です。

## 入力XLSXの形式

1行目を見出し、2行目以降を演目にします。最後には必ず`人数`列を置いてください。

| 曲名 | 時間 | 演者A | 演者B | 人数 |
|---|---:|:---:|:---:|---:|
| オープニング | 3:30 | ○ |  | 1 |
| フィナーレ | 4:05 | ○ | ○ | 2 |

- 時間は`m:ss`形式で、秒は`00`～`59`です。
- 出演は`○`、`◯`、`〇`、`O`のいずれかで指定できます。
- `メモ`または`予備`を名前に含むシートは読み飛ばします。
- 入力不備がある場合は、シート名と行番号を表示して読込を中止します。

## 保存場所とログ

既定の保存先は`ドキュメント\Setlist Studio`です。障害調査用ログは次に保存されます。

```text
ドキュメント\Setlist Studio\logs\setlist-studio.log
```

ログは2 MBで切り替わり、直近3世代を保持します。問い合わせ時は、問題が起きた操作とログを一緒に確認してください。

## 開発とWindows配布物の作成

開発にはJDK 21とApache Mavenが必要です。

```powershell
mvn clean test
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\build-windows-package.ps1
```

MavenがPATHにない場合は実行ファイルを指定できます。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\build-windows-package.ps1 `
  -MavenCommand "C:\path\to\mvn.cmd" `
  -JavaHome "C:\path\to\jdk-21"
```

完成物は`target\dist\SetlistStudio-1.0.0-windows-x64.zip`です。Java実行環境を同梱するため、利用者側のJavaインストールは不要です。現状はコード署名を行っていないため、配布先のWindows環境によっては初回起動時に警告が表示される場合があります。
