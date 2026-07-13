# audit-01 result

Subagent: Hilbert (`gpt-5.4-mini`)

## Reported findings

1. エディタ終了時の未保存変更喪失
2. ヘッダー行がないXLSXを空シートとして扱う
3. 保存済みXLSXの表示シートを並び替えるとセッション対応がずれる
4. シート名の部分一致によって正規シートを読み飛ばす

各指摘には対象コード、再現手順、影響、最小修正案、回帰テスト案が提示された。

## Integration status

- 1: 棄却。通常経路では編集のたびに`SetlistFrame.handleProjectChanged`へ現在プロジェクトが渡り、メイン画面が未保存状態を保持する。エディタを閉じても内容は失われない。
- 2: 採用。`XlsxPerformanceReader`は1行目が存在しない場合に空公演として成功扱いしていた。見出し付き空公演は許可したまま、見出し行自体がない場合だけシート名・1行目付きの入力エラーに変更した。
- 3: 棄却。表示シートを並べ替えると`verifyHiddenMetadata`が演目ID等の不一致を検出して読込を拒否するため、別公演として黙って復元されることはない。
- 4: 今回は棄却。実装は「メモ」「予備」の部分一致であり、「営業」ではない。また、この除外条件はREADMEに現行仕様として明記済み。名称衝突の使い勝手改善は別課題にできる。

## Implemented fix

- `XlsxPerformanceReader`: 見出し行がないシートを入力エラーへ変更。
- `XlsxPerformanceReaderValidationTest`: 演目データが存在しても見出しがなければ全体読込を失敗させる回帰テストを追加。

## Verification

- 対象テスト: 5件成功。
- `mvn clean test`: 49件成功、失敗0件。
- Windows配布版: 作成、GUI起動、正常終了に成功。
