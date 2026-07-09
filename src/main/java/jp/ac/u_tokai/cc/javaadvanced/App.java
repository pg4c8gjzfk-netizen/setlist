package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

/**
 * セットリスト自動生成アプリケーションの起動クラス。
 */
public class App {
    private App() {
        // 起動クラスのためインスタンス化しません。
    }

    /**
     * アプリケーションを起動します。
     * 通常は画面版を起動し、--consoleを指定した場合はコンソール版を起動します。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless() || hasConsoleOption(args)) {
            new ConsoleSetlistApplication().run();
            return;
        }

        SwingUtilities.invokeLater(() -> new SetlistFrame().showScreen());
    }

    /**
     * コンソール版の起動オプションが指定されているか判定します。
     *
     * @param args コマンドライン引数
     * @return --consoleが指定されている場合はtrue
     */
    private static boolean hasConsoleOption(String[] args) {
        return args != null && args.length > 0 && "--console".equals(args[0]);
    }
}
