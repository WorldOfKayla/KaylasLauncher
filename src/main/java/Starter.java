import org.foxesworld.Launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.JOptionPane;


public class Starter {
    /*
    public static int memory = 0;
    public static void main(String[] args) throws Exception {
        try {
            String jarpath = Starter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            ArrayList<String> params = new ArrayList<String>();
            params.add(System.getProperty("java.home") + "/bin/java");
            if (System.getProperty("os.arch").contains("64")
                    && System.getProperty("sun.arch.data.model").equals("32")) {
                JOptionPane.showMessageDialog(null,"Рекомендуется использовать\njava 64 bit", "Предупреждение!",
                        javax.swing.JOptionPane.ERROR_MESSAGE, null);
            }

            params.add("-Xmx" + memory + "m");
            params.add("-XX:MaxPermSize=128m");
            if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
                params.add("-Xdock:name=Minecraft");
                params.add("-Xdock:icon=" +"assets/ui/icons/logo.png");
            }
            params.add("-classpath");
            params.add(jarpath);
            params.add(Launcher.class.getCanonicalName());
            params.add("true");

            ProcessBuilder pb = new ProcessBuilder(params);
            //pb.directory(new File(BaseUtils.getAssetsDir().toString()));
            Process process = pb.start();
            if (process == null)
                throw new Exception("Launcher can't be started!");
            //new ProcessUtils(process).print();
        } catch (Exception e) {
            //JOptionPane.showMessageDialog(Frame.main, e, "Ошибка запуска", javax.swing.JOptionPane.ERROR_MESSAGE, null);
            try {
                Class<?> af = Class.forName("java.lang.Shutdown");
                Method m = af.getDeclaredMethod("halt0", int.class);
                m.setAccessible(true);
                m.invoke(null, 1);
            } catch (Exception x) {
            }
        }
    }
    */
}