package org.takesome.launcher.gui;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * A robust Swing component for playing GIF animations, designed to fix common rendering issues.
 * This player correctly handles all frame disposal methods, accounts for individual frame
 * offsets and dimensions, and scales the output to fit the component size. It accepts a
 * GIF source from either a {@link File} or an {@link InputStream}. It loads the GIF in a
 * background thread to keep the UI responsive and uses a thread-safe animation loop.
 */
public class GifPlayerSwing extends JPanel {

    /**
     * A structure to hold a fully rendered animation frame and its delay.
     */
    private static class AnimationFrame {
        final BufferedImage image;
        /** Delay in milliseconds. */
        final int delay;

        AnimationFrame(BufferedImage image, int delay) {
            this.image = image;
            this.delay = delay;
        }
    }

    private final List<AnimationFrame> animationFrames = Collections.synchronizedList(new ArrayList<>());
    private volatile int currentFrameIndex = 0;
    private volatile double speedFactor = 1.0;
    private volatile boolean isRunning = false;
    private ScheduledExecutorService executor;
    private final Object gifInputSource;
    private volatile BufferedImage displayImage;

    /**
     * Creates a GIF player from a {@link File}.
     * @param gifFile The GIF file to load.
     * @throws FileNotFoundException if the file does not exist, is a directory, or cannot be read.
     * @throws IllegalArgumentException if gifFile is null.
     */
    // ИСПРАВЛЕНО: Добавлена проверка файла и изменен Javadoc
    public GifPlayerSwing(File gifFile) throws FileNotFoundException {
        if (gifFile == null) {
            throw new IllegalArgumentException("GIF file cannot be null.");
        }
        if (!gifFile.exists()) {
            throw new FileNotFoundException("The specified file does not exist: " + gifFile.getAbsolutePath());
        }
        if (!gifFile.isFile()) {
            throw new FileNotFoundException("The specified path is a directory, not a file: " + gifFile.getAbsolutePath());
        }
        if (!gifFile.canRead()) {
            throw new FileNotFoundException("The application cannot read the specified file: " + gifFile.getAbsolutePath());
        }
        this.gifInputSource = gifFile;
        commonInit();
    }

    /**
     * Creates a GIF player from an {@link InputStream}.
     * This constructor reads the entire stream into memory to ensure stable processing,
     * as not all streams support seeking. The provided stream is not closed by this method.
     * @param inputStream The stream containing the GIF data.
     * @throws IOException if an I/O error occurs while reading the stream.
     * @throws IllegalArgumentException if the inputStream is null or empty.
     */
    // ИСПРАВЛЕНО: Добавлена проверка потока и изменен Javadoc
    public GifPlayerSwing(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("GIF input stream cannot be null.");
        }
        byte[] data = inputStream.readAllBytes();
        if (data.length == 0) {
            throw new IllegalArgumentException("The provided InputStream is empty.");
        }
        this.gifInputSource = data;
        commonInit();
    }

    /**
     * Performs common initialization tasks for all constructors.
     */
    private void commonInit() {
        setOpaque(false);
        loadInBackground();
    }

    private void loadInBackground() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Теперь нет необходимости в проверке stream == null, так как источник уже проверен в конструкторе.
                try (ImageInputStream stream = ImageIO.createImageInputStream(gifInputSource)) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                    if (!readers.hasNext()) {
                        throw new IOException("No GIF reader found for the provided input source.");
                    }
                    ImageReader reader = readers.next();
                    reader.setInput(stream);

                    GifUtil.FrameMetadata firstFrameMeta = GifUtil.getFrameMetadata(reader, 0);
                    int logicalScreenWidth = firstFrameMeta.logicalScreenWidth;
                    int logicalScreenHeight = firstFrameMeta.logicalScreenHeight;

                    BufferedImage master = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D masterGraphics = master.createGraphics();

                    BufferedImage previousFrameState = null;

                    int numFrames = reader.getNumImages(true);
                    for (int i = 0; i < numFrames; i++) {
                        GifUtil.FrameMetadata metadata = GifUtil.getFrameMetadata(reader, i);
                        BufferedImage frameImage = reader.read(i);

                        if (metadata.disposalMethod == 3) {
                            previousFrameState = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
                        }

                        masterGraphics.drawImage(frameImage, metadata.x, metadata.y, null);

                        BufferedImage finalFrame = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
                        animationFrames.add(new AnimationFrame(finalFrame, metadata.delay));

                        switch (metadata.disposalMethod) {
                            case 2: // restoreToBackgroundColor
                                masterGraphics.setComposite(AlphaComposite.Clear);
                                masterGraphics.fillRect(metadata.x, metadata.y, metadata.width, metadata.height);
                                masterGraphics.setComposite(AlphaComposite.SrcOver);
                                break;
                            case 3: // restoreToPrevious
                                if (previousFrameState != null) {
                                    master.setData(previousFrameState.getData());
                                }
                                break;
                        }

                        if (i == 0) {
                            SwingUtilities.invokeLater(() -> {
                                displayImage = finalFrame;
                                revalidate();
                                repaint();
                                start();
                            });
                        }
                    }

                    reader.dispose();
                    masterGraphics.dispose();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    e.printStackTrace();
                    // Здесь можно отобразить визуальное сообщение об ошибке, если это все еще необходимо
                }
            }
        }.execute();
    }

    private void scheduleNextFrame() {
        if (!isRunning || animationFrames.isEmpty()) return;
        int delay = (int) (animationFrames.get(currentFrameIndex).delay / speedFactor);
        executor.schedule(this::updateFrame, Math.max(10, delay), TimeUnit.MILLISECONDS);
    }

    private void updateFrame() {
        currentFrameIndex = (currentFrameIndex + 1) % animationFrames.size();
        SwingUtilities.invokeLater(() -> {
            displayImage = animationFrames.get(currentFrameIndex).image;
            repaint();
            scheduleNextFrame();
        });
    }

    public synchronized void start() {
        if (isRunning || animationFrames.isEmpty()) return;
        isRunning = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GifPlayerSwing-AnimationThread");
            t.setDaemon(true);
            return t;
        });
        scheduleNextFrame();
    }

    public synchronized void stop() {
        isRunning = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public void setSpeedFactor(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Speed factor must be positive.");
        }
        this.speedFactor = factor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (displayImage != null) {
            g.drawImage(displayImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (displayImage != null) {
            return new Dimension(displayImage.getWidth(), displayImage.getHeight());
        }
        return new Dimension(100, 100);
    }
}


/**
 * A utility class for extracting metadata from GIF frames.
 * This class manually traverses the metadata tree to ensure
 * compatibility across different Java environments.
 */
class GifUtil {
    // ... Код GifUtil остается без изменений ...
    static class FrameMetadata {
        final int delay;
        final int disposalMethod;
        final int x, y, width, height;
        final int logicalScreenWidth, logicalScreenHeight;

        FrameMetadata(int delay, int disposal, int x, int y, int w, int h, int lsw, int lsh) {
            this.delay = delay;
            this.disposalMethod = disposal;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.logicalScreenWidth = lsw;
            this.logicalScreenHeight = lsh;
        }
    }

    private static Node findNode(Node parentNode, String nodeName) {
        if (parentNode.getNodeName().equals(nodeName)) {
            return parentNode;
        }
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            Node found = findNode(child, nodeName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static FrameMetadata getFrameMetadata(ImageReader reader, int frameIndex) throws IOException {
        IIOMetadata metadata = reader.getImageMetadata(frameIndex);
        String formatName = metadata.getNativeMetadataFormatName();
        Node root = metadata.getAsTree(formatName);

        Node graphicControlExtensionNode = findNode(root, "GraphicControlExtension");
        Node imageDescriptorNode = findNode(root, "ImageDescriptor");
        Node logicalScreenDescriptorNode = findNode(root, "LogicalScreenDescriptor");

        int logicalScreenWidth = 0;
        int logicalScreenHeight = 0;
        if (logicalScreenDescriptorNode != null) {
            NamedNodeMap attributes = logicalScreenDescriptorNode.getAttributes();
            logicalScreenWidth = Integer.parseInt(attributes.getNamedItem("logicalScreenWidth").getNodeValue());
            logicalScreenHeight = Integer.parseInt(attributes.getNamedItem("logicalScreenHeight").getNodeValue());
        }

        int delay = 100;
        int disposal = 0;
        if (graphicControlExtensionNode != null) {
            NamedNodeMap attributes = graphicControlExtensionNode.getAttributes();
            delay = Integer.parseInt(attributes.getNamedItem("delayTime").getNodeValue()) * 10;
            String disposalMethodStr = attributes.getNamedItem("disposalMethod").getNodeValue();
            disposal = switch (disposalMethodStr) {
                case "restoreToBackgroundColor" -> 2;
                case "restoreToPrevious" -> 3;
                case "doNotDispose" -> 1;
                default -> 0; // unspecified
            };
        }

        int x = 0, y = 0, width = 0, height = 0;
        if (imageDescriptorNode != null) {
            NamedNodeMap attributes = imageDescriptorNode.getAttributes();
            x = Integer.parseInt(attributes.getNamedItem("imageLeftPosition").getNodeValue());
            y = Integer.parseInt(attributes.getNamedItem("imageTopPosition").getNodeValue());
            width = Integer.parseInt(attributes.getNamedItem("imageWidth").getNodeValue());
            height = Integer.parseInt(attributes.getNamedItem("imageHeight").getNodeValue());
        }

        if (logicalScreenWidth == 0) logicalScreenWidth = width;
        if (logicalScreenHeight == 0) logicalScreenHeight = height;

        return new FrameMetadata(delay, disposal, x, y, width, height, logicalScreenWidth, logicalScreenHeight);
    }
}