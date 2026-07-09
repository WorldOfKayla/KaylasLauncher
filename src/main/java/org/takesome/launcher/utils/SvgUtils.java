package org.takesome.launcher.utils;


import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Утилита для работы с SVG, использующая FlatSVGIcon из библиотеки FlatLaf.
 * Позволяет рендерить SVG из URL в BufferedImage.
 */
public final class SvgUtils {

    // Приватный конструктор, чтобы предотвратить создание экземпляров
    private SvgUtils() {}

    /**
     * Асинхронно загружает и рендерит SVG из URL в BufferedImage.
     * Размер изображения определяется автоматически на основе размеров иконки.
     *
     * @param url URL SVG-файла.
     * @return CompletableFuture, который завершится с отрендеренным BufferedImage.
     */
    public static CompletableFuture<BufferedImage> renderToImageAsync(URL url) {
        return CompletableFuture.supplyAsync(() -> renderToImage(url));
    }

    /**
     * Асинхронно загружает и рендерит SVG из URL в BufferedImage с указанными размерами.
     *
     * @param url URL SVG-файла.
     * @param width Ширина итогового изображения.
     * @param height Высота итогового изображения.
     * @return CompletableFuture, который завершится с отрендеренным BufferedImage.
     */
    public static CompletableFuture<BufferedImage> renderToImageAsync(URL url, int width, int height) {
        return CompletableFuture.supplyAsync(() -> renderToImage(url, width, height));
    }

    /**
     * Синхронно загружает и рендерит SVG из URL в BufferedImage.
     * Размер изображения определяется автоматически на основе размеров иконки.
     *
     * @param url URL SVG-файла.
     * @return Отрендеренный BufferedImage.
     */
    public static BufferedImage renderToImage(URL url) {
        FlatSVGIcon icon = new FlatSVGIcon(url);
        return render(icon);
    }

    /**
     * Синхронно загружает и рендерит SVG из URL в BufferedImage с указанными размерами.
     *
     * @param url URL SVG-файла.
     * @param width Ширина итогового изображения.
     * @param height Высота итогового изображения.
     * @return Отрендеренный BufferedImage.
     */
    public static BufferedImage renderToImage(URL url, int width, int height) {
        FlatSVGIcon icon = new FlatSVGIcon(url).derive(width, height);
        return render(icon);
    }

    /**
     * Вспомогательный метод для рендеринга FlatSVGIcon на BufferedImage.
     */
    private static BufferedImage render(FlatSVGIcon icon) {
        // Создаем пустой холст (BufferedImage) с размерами иконки
        // Используем TYPE_INT_ARGB для поддержки прозрачности
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

        // Получаем его графический контекст
        Graphics2D g2d = image.createGraphics();

        try {
            // "Рисуем" иконку на нашем холсте в координатах (0, 0)
            icon.paintIcon(null, g2d, 0, 0);
        } finally {
            // Освобождаем ресурсы графического контекста
            g2d.dispose();
        }

        return image;
    }
}