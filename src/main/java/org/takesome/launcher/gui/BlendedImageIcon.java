package org.takesome.launcher.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BlendedImageIcon extends ImageIcon {

    /**
     * Конструктор, объединяющий два изображения с заданной степенью прозрачности.
     *
     * @param img1  Первое изображение
     * @param img2  Второе изображение
     * @param alpha Коэффициент прозрачности для второго изображения (0 - полностью прозрачное, 1 - полностью непрозрачное)
     */
    public BlendedImageIcon(Image img1, Image img2, float alpha) {
        super(blendImages(img1, img2, alpha));
    }

    /**
     * Метод для объединения двух изображений.
     *
     * @param img1  Первое изображение
     * @param img2  Второе изображение
     * @param alpha Коэффициент прозрачности для второго изображения
     * @return Объединённое изображение типа Image
     */
    private static Image blendImages(Image img1, Image img2, float alpha) {
        BufferedImage bImg1 = toBufferedImage(img1);
        BufferedImage bImg2 = toBufferedImage(img2);

        // Определение размеров итогового изображения
        int width = Math.max(bImg1.getWidth(), bImg2.getWidth());
        int height = Math.max(bImg1.getHeight(), bImg2.getHeight());

        // Создание пустого изображения с поддержкой альфа-канала
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        // Настройка рендеринга для повышения качества
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Центрирование и отрисовка первого изображения с композитом (1 - alpha)
        int x1 = (width - bImg1.getWidth()) / 2;
        int y1 = (height - bImg1.getHeight()) / 2;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha));
        g.drawImage(bImg1, x1, y1, null);

        // Центрирование и отрисовка второго изображения с композитом alpha
        int x2 = (width - bImg2.getWidth()) / 2;
        int y2 = (height - bImg2.getHeight()) / 2;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(bImg2, x2, y2, null);

        // Освобождение ресурсов Graphics2D
        g.dispose();
        return combined;
    }

    /**
     * Преобразует объект Image в BufferedImage.
     *
     * @param img Входное изображение
     * @return BufferedImage, представляющее исходное изображение
     */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Создание BufferedImage с размерами исходного изображения и поддержкой прозрачности
        BufferedImage bImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bImage;
    }
}
