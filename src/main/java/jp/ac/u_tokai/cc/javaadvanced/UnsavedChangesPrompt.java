package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.Component;
import javax.swing.JOptionPane;

/** 未保存の編集内容を置き換える前に、利用者の意思を確認します。 */
@FunctionalInterface
interface UnsavedChangesPrompt {

    /** 未保存内容の扱いです。 */
    enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }

    /**
     * 指定操作の前に、未保存内容をどう扱うか確認します。
     *
     * @param parent ダイアログの親
     * @param actionName 続けようとしている操作
     * @return 利用者が選択した扱い
     */
    Decision ask(Component parent, String actionName);

    /** Swingの標準確認ダイアログを使用します。 */
    static UnsavedChangesPrompt swingDialog() {
        return (parent, actionName) -> {
            Object[] options = {"保存して続ける", "保存せず続ける", "キャンセル"};
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    "未保存の変更があります。\n" + actionName + "前に、編集状態を保存しますか？",
                    "未保存の変更",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            return switch (choice) {
                case 0 -> Decision.SAVE;
                case 1 -> Decision.DISCARD;
                default -> Decision.CANCEL;
            };
        };
    }
}
