# Setlist defect audit

## Goal

既知Issue完了後のSetlist Studioを実使用の観点で独立監査し、再現可能で影響の大きい欠陥を1件以上特定する。

## Success Criteria

- 現在の`main`を対象に、推測ではなくコード上の証拠と再現手順を示す。
- 重大度、実使用への影響、対象ファイル・行、推奨修正、必要なテストを報告する。
- XLSXワークシート境界を侵害する提案を採用しない。
- 主担当が結果を再検証し、最優先の欠陥を修正できる状態にする。

## Current Context

- GitHubの既知Issueはすべて完了済み。
- Java 21 / Maven / Swing / FlatLaf / Apache POIを使用するWindowsデスクトップアプリ。
- CSV対応は廃止済みで、XLSXのみを扱う。

## Constraints

- 各XLSXワークシートは独立した公演であり、読込・生成・再生成・保存・再読込で跨がせない。
- 既存クラスを命名規則だけで再命名しない。
- 監査サブエージェントは読み取り専用とし、コードを変更しない。
- 既存テストが通るという理由だけで実使用上の欠陥を除外しない。

## Risks

- 推測上の問題を欠陥と誤認すること。
- シート境界など既存の絶対条件を見落とすこと。
- GUIまたはファイル操作の状態遷移を静的解析だけで誤判定すること。

## Approval Required

読み取り専用監査は追加承認不要。外部公開、破壊的操作、広範な変更は行わない。

## Work Packets

- `audit-01`: 実使用上の欠陥監査。重要度順に最大5件、うち最優先1件を明示する。

## Integration Policy

主担当がコードとテストで再確認できた指摘だけを採用する。再現不能、仕様違反、重複指摘は棄却する。

## Verification

- 指摘箇所の局所コード確認
- 必要に応じた再現テスト追加
- `mvn clean test`
- 影響がGUIに及ぶ場合はWindows配布版で自動GUI確認

## Reusable Artifacts

監査計画・結果・統合判断を`.workflow/setlist-defect-audit/`へ保存する。
