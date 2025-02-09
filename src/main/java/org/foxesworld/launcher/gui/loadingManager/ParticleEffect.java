package org.foxesworld.launcher.gui.loadingManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class ParticleEffect {
    private ArrayList<Particle> particles;
    private Random random;

    public ParticleEffect() {
        particles = new ArrayList<>();
        random = new Random();
    }

    public void createParticles(int x, int y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, random.nextInt(5) + 1, random.nextFloat() * 360));
        }
    }

    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (p.update()) {
                particles.remove(i);
            }
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        for (Particle p : particles) {
            p.draw(g2d);
        }
        g2d.dispose();
    }

    private static class Particle {
        float x, y;
        float speed;
        float angle;
        int life;

        Particle(int x, int y, int speed, float angle) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.angle = angle;
            this.life = 30;
        }

        boolean update() {
            x += Math.cos(Math.toRadians(angle)) * speed;
            y += Math.sin(Math.toRadians(angle)) * speed;
            life--;
            return life <= 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, life * 255 / 30));
            g2d.fillOval((int) x, (int) y, 4, 4);
        }
    }
}