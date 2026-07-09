package jp.ac.u_tokai.cc.javaadvanced;

/**
 * セットリスト内で演目を固定する位置です。
 */
public enum FixedPosition {
    /** 特定の位置に固定しません。 */
    NONE("通常"),
    /** 公演の先頭に固定します。 */
    OPENING("オープニング"),
    /** 公演の末尾に固定します。 */
    CLOSING("トリ"),
    /** 指定したインデックスに固定します。 */
    INDEX("位置固定");

    private final String displayName;

    FixedPosition(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
