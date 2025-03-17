package org.foxesworld.test.patch;

import java.lang.instrument.Instrumentation;

public class PanelAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("PanelAgent запущен.");
        if (inst.isRetransformClassesSupported()) {
            //inst.addTransformer(new PanelTransformer(), true);
        } else {
            System.out.println("Retransform не поддерживается. Регистрируем трансформер без retransformable.");
            //inst.addTransformer(new PanelTransformer(), false);
        }
    }
}
