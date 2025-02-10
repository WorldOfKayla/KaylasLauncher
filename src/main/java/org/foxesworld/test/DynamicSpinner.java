package org.foxesworld.test;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DynamicSpinner extends Application {
    private double angle = 0;
    private double arcLength = 120; // Начальная длина дуги
    private double arcSpeed = 2; // Базовая скорость изменения дуги
    private double rotationSpeed = 5; // Базовая скорость вращения
    private boolean expanding = true; // Флаг увеличения/уменьшения дуги

    @Override
    public void start(Stage primaryStage) {
        int size = 100;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > 1_000_000) { // 60 FPS
                    drawSpinner(gc, size);
                    angle += rotationSpeed; // Постоянное вращение

                    // Изменение длины дуги + ускорение при разрыве
                    if (expanding) {
                        arcLength += arcSpeed;
                        rotationSpeed = 3; // Нормальная скорость вращения
                        if (arcLength >= 270) {
                            expanding = false;
                        }
                    } else {
                        arcLength -= arcSpeed * 1.5; // Ускоренное сжатие
                        rotationSpeed = 6; // Ускоренное вращение
                        if (arcLength <= 90) {
                            expanding = true;
                        }
                    }

                    lastUpdate = now;
                }
            }
        };
        timer.start();

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #222222;"); // Тёмный фон

        Scene scene = new Scene(root, 200, 200);
        primaryStage.setTitle("Smooth Dynamic Spinner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void drawSpinner(GraphicsContext gc, int size) {
        gc.clearRect(0, 0, size, size);
        gc.setStroke(Color.LIMEGREEN);
        gc.setLineWidth(6);

        double centerX = size / 2.0;
        double centerY = size / 2.0;
        double radius = size / 3.0;

        // Рисуем дугу с динамически изменяющейся длиной
        gc.strokeArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius,
                angle, arcLength, javafx.scene.shape.ArcType.OPEN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
