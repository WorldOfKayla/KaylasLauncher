package org.foxesworld.newengine.utils;

public class LogUtils {

    public static void send(String msg, Integer tabs, boolean error) {
        String tab = "\t";
        StringBuffer sb = new StringBuffer();
        String sendText = "empty";
        switch (tabs) {
            case 0: {
                break;
            }
            default: {
                Integer i = 0;
                while (i < tabs) {
                    sb.append(tab);
                    Integer n = i;
                    Integer n2 = i = Integer.valueOf(i + 1);
                }
                break;
            }
        }
        String prefix = error ? "ERROR" : "Info";
        sendText = tabs == 0 ? "[FoxesLauncher/" + prefix + "]: " + msg : sb + " - [" + prefix + "]" + msg;
        System.out.println(sendText);
    }
}
