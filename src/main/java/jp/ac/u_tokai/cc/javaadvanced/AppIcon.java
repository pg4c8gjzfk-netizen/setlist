package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Setlist Studioのウィンドウ用アイコンを解像度に応じて描画します。 */
public final class AppIcon {

    private AppIcon() {
    }

    /** 青い角丸面に香盤表の行を描いたアイコンを生成します。 */
    public static BufferedImage create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int inset = Math.max(2, size / 16);
            int arc = Math.max(8, size / 3);
            graphics.setPaint(new GradientPaint(
                    0, inset, new Color(0x1A8CFF), size, size, new Color(0x0066CC)));
            graphics.fillRoundRect(inset, inset, size - inset * 2, size - inset * 2, arc, arc);

            int dotSize = Math.max(3, size / 10);
            int lineHeight = Math.max(2, size / 14);
            int left = size / 4;
            int lineLeft = left + dotSize + size / 12;
            int lineWidth = size - lineLeft - size / 5;
            graphics.setColor(Color.WHITE);
            for (int row = 0; row < 3; row++) {
                int y = size / 4 + row * size / 5;
                graphics.fillOval(left, y, dotSize, dotSize);
                graphics.fillRoundRect(
                        lineLeft, y + (dotSize - lineHeight) / 2,
                        lineWidth, lineHeight, lineHeight, lineHeight);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
