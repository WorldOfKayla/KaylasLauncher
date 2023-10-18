package org.foxesworld.engine.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;

public class ImageUtils {

    public static Map<String, BufferedImage> imgs = new HashMap<>();

    public static BufferedImage getLocalImage(String name) {
        try {
            if (imgs.containsKey(name)) {
                return imgs.get(name);
            }

            BufferedImage img = ImageIO.read(ImageUtils.class.getClassLoader().getResourceAsStream(name));
            imgs.put(name, img);
            return img;
        } catch (Exception e) {
            //APP.LOGGER.error("Failed to open local image: " + name);
            return new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static BufferedImage genButton(int w, int h, BufferedImage img) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width and height must be greater than zero.");
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage left = img.getSubimage(0, 0, img.getWidth() / 3, img.getHeight());
        BufferedImage center = img.getSubimage(img.getWidth() / 3, 0, img.getWidth() / 3, img.getHeight());
        BufferedImage right = img.getSubimage(img.getWidth() / 3 * 2, 0, img.getWidth() / 3, img.getHeight());

        Graphics2D g = res.createGraphics();

        // Draw the left part
        g.drawImage(left, 0, 0, left.getWidth(), h, null);

        // Draw the center part (stretch it to fit the width)
        g.drawImage(center, left.getWidth(), 0, w - left.getWidth() - right.getWidth(), h, null);

        // Draw the right part
        g.drawImage(right, w - right.getWidth(), 0, right.getWidth(), h, null);

        g.dispose();

        return res;
    }

    public static BufferedImage genPanel(int w, int h, BufferedImage img) {
        BufferedImage res = new BufferedImage(w, h, 2);
        int onew = img.getWidth() / 3;
        int oneh = img.getHeight() / 3;
        res.getGraphics().drawImage(img.getSubimage(0, 0, onew, oneh), 0, 0, onew, oneh, null);
        res.getGraphics().drawImage(img.getSubimage(onew * 2, 0, onew, oneh), w - onew, 0, onew, oneh, null);
        res.getGraphics().drawImage(img.getSubimage(0, oneh * 2, onew, oneh), 0, h - oneh, onew, oneh, null);
        res.getGraphics().drawImage(img.getSubimage(onew, oneh, onew * 2, oneh * 2), w - onew, h - oneh, onew, oneh, null);
        try {
            res.getGraphics().drawImage(ImageUtils.fill(img.getSubimage(onew, 0, onew, oneh), w - onew * 2, oneh), onew, 0, w - onew * 2, oneh, null);
        } catch (Exception exception) {
            // empty catch block
        }
        try {
            res.getGraphics().drawImage(ImageUtils.fill(img.getSubimage(0, oneh, onew, oneh), onew, h - oneh * 2), 0, oneh, onew, h - oneh * 2, null);
        } catch (Exception exception) {
            // empty catch block
        }
        try {
            res.getGraphics().drawImage(ImageUtils.fill(img.getSubimage(onew, oneh * 2, onew, oneh), w - onew * 2, oneh), onew, h - oneh, w - onew * 2, oneh, null);
        } catch (Exception exception) {
            // empty catch block
        }
        try {
            res.getGraphics().drawImage(ImageUtils.fill(img.getSubimage(onew * 2, oneh, onew, oneh), onew, h - oneh * 2), w - onew, oneh, onew, h - oneh * 2, null);
        } catch (Exception exception) {
            // empty catch block
        }
        try {
            res.getGraphics().drawImage(ImageUtils.fill(img.getSubimage(onew, oneh, onew, oneh), w - onew * 2, h - oneh * 2), onew, oneh, w - onew * 2, h - oneh * 2, null);
        } catch (Exception exception) {
            // empty catch block
        }
        return res;
    }

    public static BufferedImage fill(BufferedImage texture, int w, int h) {
        int sizex = texture.getWidth();
        int sizey = texture.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        for (int x2 = 0; x2 <= w / sizex; ++x2) {
            for (int y2 = 0; y2 <= h / sizey; ++y2) {
                g2d.drawImage(texture, x2 * sizex, y2 * sizey, null);
            }
        }

        g2d.dispose();
        return img;
    }


    public static BufferedImage fillHoriz(BufferedImage texture, int w, int h) {
        int sizex = texture.getWidth();
        BufferedImage img = new BufferedImage(w, h, 2);
        for (int x2 = 0; x2 <= w / sizex; ++x2) {
            img.getGraphics().drawImage(texture, x2 * sizex, 0, sizex, texture.getHeight(), null);
        }
        return img;
    }

    public static BufferedImage blurImage(BufferedImage image) {
        float ninth = 0.11111111f;
        float[] blurKernel = new float[]{ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth};
        HashMap<RenderingHints.Key, Object> map = new HashMap<RenderingHints.Key, Object>();
        map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RenderingHints hints = new RenderingHints(map);
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, blurKernel), 1, hints);
        return op.filter(image, null);
    }
    public static BufferedImage screenComponent(JComponent c) {
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), 2);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        return img;
    }
    public static Image getScaledImage(Image srcImg, int w, int h) {
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (w > srcImg.getWidth(null) || h > srcImg.getHeight(null)) {
            g2.drawImage(srcImg, 0, 0, w, h, 0, 0, srcImg.getWidth(null), srcImg.getHeight(null), null);
        } else {
            g2.drawImage(srcImg, 0, 0, w, h, null);
        }
        g2.dispose();
        return resizedImg;
    }

    public static BufferedImage getByIndex(BufferedImage all, int d, int i) {
        return all.getSubimage(d * i, 0, d, d);
    }

    public static BufferedImage getByIndexCR(BufferedImage all, int d, int row, int i) {
        return all.getSubimage(d * i, row * i, d, d);
    }

    public static BufferedImage[] spriteCollsRows(BufferedImage img, int colls, int rows, int width, int height) {
        BufferedImage[] spritesOut = new BufferedImage[rows * colls];
        int i = 0;
        int j = 0;
        for (i = 0; i < rows; ++i) {
            for (j = 0; j < colls; ++j) {
                spritesOut[i * colls + j] = img.getSubimage(j * width, i * height, width, height);
            }
        }
        return spritesOut;
    }
}

