package org.foxesworld.test.patch;

/*
import javassist.*;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;

public class PanelTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!"javax/swing/JPanel".equals(className)) {
            return null;
        }
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new LoaderClassPath(loader));
            CtClass cc = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
            if (cc.isFrozen()) {
                cc.defrost();
            }
            // Добавляем поле alpha с модификатором volatile, если его ещё нет
            try {
                cc.getDeclaredField("alpha");
            } catch (NotFoundException e) {
                CtField alphaField = new CtField(CtClass.floatType, "alpha", cc);
                alphaField.setModifiers(Modifier.PRIVATE | Modifier.VOLATILE);
                cc.addField(alphaField, "1.0f");
            }
            // Добавляем геттер getAlpha(), если отсутствует
            try {
                cc.getDeclaredMethod("getAlpha");
            } catch (NotFoundException e) {
                CtMethod getAlpha = CtNewMethod.make(
                        "public float getAlpha() { return this.alpha; }", cc);
                cc.addMethod(getAlpha);
            }
            // Добавляем сеттер setAlpha(float) с проверкой EDT и переключением режима непрозрачности
            try {
                cc.getDeclaredMethod("setAlpha", new CtClass[]{CtClass.floatType});
            } catch (NotFoundException e) {
                String setAlphaMethod =
                        "public void setAlpha(float a) {" +
                                "  if (!javax.swing.SwingUtilities.isEventDispatchThread()) {" +
                                "    org.foxesworld.test.patch.PanelTransformer.scheduleSetAlpha(this, a);" +
                                "    return;" +
                                "  }" +
                                "  if(a < 0f) { a = 0f; } else if(a > 1f) { a = 1f; }" +
                                "  this.alpha = a;" +
                                "  this.setOpaque(a >= 1.0f);" +
                                "  this.repaint();" +
                                "}";
                CtMethod setAlpha = CtNewMethod.make(setAlphaMethod, cc);
                cc.addMethod(setAlpha);
            }
            // Модифицируем или добавляем метод paintComponent(Graphics)
            CtMethod paintMethod = null;
            try {
                paintMethod = cc.getDeclaredMethod("paintComponent", new CtClass[]{pool.get("java.awt.Graphics")});
            } catch (NotFoundException e) {
                String newMethod =
                        "protected void paintComponent(java.awt.Graphics g) {" +
                                "  java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();" +
                                "  try {" +
                                "    if (this.alpha < 1.0f) {" +
                                "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.CLEAR));" +
                                "      g2d.fillRect(0, 0, this.getWidth(), this.getHeight());" +
                                "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, this.alpha));" +
                                "    } else if (this.isOpaque()) {" +
                                "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC));" +
                                "      g2d.setColor(this.getBackground());" +
                                "      g2d.fillRect(0, 0, this.getWidth(), this.getHeight());" +
                                "    }" +
                                "    super.paintComponent(g2d);" +
                                "  } finally {" +
                                "    g2d.dispose();" +
                                "  }" +
                                "}";
                paintMethod = CtNewMethod.make(newMethod, cc);
                cc.addMethod(paintMethod);
            }
            // Если метод paintComponent найден, заменяем его тело на новое
            String newBody =
                    "{ " +
                            "  java.awt.Graphics2D g2d = (java.awt.Graphics2D)$1.create();" +
                            "  try {" +
                            "    if (this.alpha < 1.0f) {" +
                            "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.CLEAR));" +
                            "      g2d.fillRect(0, 0, this.getWidth(), this.getHeight());" +
                            "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, this.alpha));" +
                            "    } else if (this.isOpaque()) {" +
                            "      g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC));" +
                            "      g2d.setColor(this.getBackground());" +
                            "      g2d.fillRect(0, 0, this.getWidth(), this.getHeight());" +
                            "    }" +
                            "    super.paintComponent(g2d);" +
                            "  } finally {" +
                            "    g2d.dispose();" +
                            "  }" +
                            "}";
            paintMethod.setBody(newBody);

            byte[] byteCode = cc.toBytecode();
            cc.detach();
            System.out.println("JPanel успешно модифицирован для поддержки прозрачности с alpha.");
            return byteCode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Статический метод-помощник для вызова setAlpha через reflection,
    // если метод был вызван не в потоке EDT.
    public static void scheduleSetAlpha(JPanel panel, float newAlpha) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    java.lang.reflect.Method m = panel.getClass().getMethod("setAlpha", float.class);
                    m.invoke(panel, newAlpha);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
*/