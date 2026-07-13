# Orchestration: Setlist defect audit

## Execution Rules

- Keep the original objective intact.
- Ask for approval before risky, expensive, external, or destructive actions.
- Keep immediate blocking work local.
- Delegate only bounded, disjoint, materially useful packets.
- Integrate packet results before final verification.

## Branching Rules

1. 再現可能な高重大度欠陥が見つかった場合、主担当が最優先1件を実装する。
2. 指摘が既存仕様またはシート境界ルールと衝突する場合は棄却する。
3. 高重大度がない場合は、中重大度のうちデータ損失・操作不能・誤出力に最も近いものを選ぶ。
4. すべて推測で再現不能なら、主担当が追加のGUI状態遷移監査を行う。

## Packet Prompts

### audit-01

現在の`main`を読み取り専用で監査する。GUI状態遷移、XLSX読込・保存・再読込、入力検証、エラー復旧、データ損失、配布環境を重点確認する。最大5件を重大度順に報告し、各件へ証拠となるファイル・行、具体的な再現手順、実使用への影響、最小修正案、回帰テスト案を付ける。既知の絶対条件であるワークシート境界を維持し、CSV対応や命名だけの変更は提案しない。コードは変更しない。

## Completion Audit

- サブエージェント結果が保存されている。
- 主担当の採用・棄却理由が記録されている。
- 採用した欠陥の修正と検証が完了している。
