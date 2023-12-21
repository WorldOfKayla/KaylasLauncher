package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.locale.LanguageProvider;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringTokenizer;

public class ServerInfo {
    private final LanguageProvider lang;
    private final BufferedImage serverStatusImg;
    public int servtype = 2;

    public ServerInfo(Engine engine) {
        this.lang = engine.getLANG();
        serverStatusImg = ImageUtils.getLocalImage("assets/ui/icons/status.png");
    }


    public String[] pollServer(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(6000);
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(18);
            socket.connect(new InetSocketAddress(ip, port), 6000);

            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                dos.write(254);

                if (dis.read() != 255) {
                    throw new IOException("Bad message");
                }

                String servc = readString(dis, 256);
                servc = servc.substring(3);
                servtype = servc.startsWith("§1") ? 1 : 2;

                String delimiter = "§";
                return splitString(servc, delimiter);
            }
        } catch (Exception e) {
             //e.printStackTrace();
            return new String[] { null, null, null };
        }
    }

    private String[] splitString(String input, String delimiter) {
        StringTokenizer tokenizer = new StringTokenizer(input, delimiter);
        String[] resultArray = new String[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            resultArray[index] = tokenizer.nextToken();
            index++;
        }
        return resultArray;
    }

    public String readString(DataInputStream is, int d) throws IOException {
        short word = is.readShort();
        if (word > d)
            throw new IOException();
        if (word < 0)
            throw new IOException();
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < word; i++) {
            res.append(is.readChar());
        }
        return res.toString();
    }

    public String genServerStatus(String[] args) {
        if (servtype == 1) {
            if (args[0] == null && args[1] == null && args[2] == null)
                return lang.getString("server.serverOff");
            if (args[4] != null && args[5] != null) {
                if (args[4].equals(args[5]))
                    return lang.getString("server.serverOff").replace("%%", args[4]);
                return lang.getString("server.serverOn").replace("%%", args[4]).replace("##", args[5]);
            }
        } else if (servtype == 2) {

            if (args[0] == null && args[1] == null && args[2] == null)
                return lang.getString("server.serverOff");
            if (args[1] != null && args[2] != null) {
                int i = args.length;
                if (args[i - 2].equals(args[i - 1]))
                    return lang.getString("server.serverOff").replace("%%", args[i - 1]);
                return lang.getString("server.serverOn").replace("%%", args[i - 2]).replace("##", args[i - 1]);
            }
        }
        return lang.getString("server.serverErr");
    }

    public BufferedImage genServerIcon(String[] args) {
        if (args[0] == null && args[1] == null && args[2] == null)
            return serverStatusImg.getSubimage(0, 0, serverStatusImg.getHeight(), serverStatusImg.getHeight());
        if (args[1] != null && args[2] != null) {
            if (args[1].equals(args[2]))
                return serverStatusImg.getSubimage(serverStatusImg.getHeight(), 0, serverStatusImg.getHeight(), serverStatusImg.getHeight());
            return serverStatusImg.getSubimage(serverStatusImg.getHeight() * 2, 0, serverStatusImg.getHeight(), serverStatusImg.getHeight());
        }
        return serverStatusImg.getSubimage(serverStatusImg.getHeight() * 3, 0, serverStatusImg.getHeight(), serverStatusImg.getHeight());
    }
}
