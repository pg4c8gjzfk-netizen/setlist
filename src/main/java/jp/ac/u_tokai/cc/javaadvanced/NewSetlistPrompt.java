package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.Window;
import java.util.Optional;

/** 新規作成前の必須設定を取得します。 */
@FunctionalInterface
interface NewSetlistPrompt {

    Optional<NewSetlistSettings> ask(Window parent);

    static NewSetlistPrompt swingDialog() {
        return parent -> new NewSetlistDialog(parent).showDialog();
    }
}
