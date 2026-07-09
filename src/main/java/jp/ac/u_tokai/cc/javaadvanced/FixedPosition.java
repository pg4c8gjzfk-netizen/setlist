package jp.ac.u_tokai.cc.javaadvanced;

/**
 * セットリスト内で演目を固定する位置です。
 */
public enum FixedPosition {
    /** 特定の位置に固定しません。 */
    NONE,
    /** 公演の先頭に固定します。 */
    OPENING,
    /** 公演の末尾に固定します。 */
    CLOSING,
    /** 指定したインデックスに固定します。 */
    INDEX
}
