package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
public final class md5Func {
    public static String md5(String filename) {
        if (new File(filename).isDirectory()) {
            Engine.LOGGER.warn("RUNNING IN IDE!!!");
            return "IDE";
        }
        FileInputStream fis = null;
        FilterInputStream dis = null;
        BufferedInputStream bis = null;
        Formatter formatter = null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(filename);

            bis = new BufferedInputStream(fis);
            dis = new DigestInputStream(bis, messagedigest);
            while ((dis).read() != -1) {
            }
            byte[] abyte0 = messagedigest.digest();
            formatter = new Formatter();
            for (byte byte0 : abyte0) {
                formatter.format("%02x", byte0);
            }
            return formatter.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            return "0";
        } finally {
            try {
                assert fis != null;
                fis.close();
            } catch (IOException ignored) {
            }
            try {
                assert dis != null;
                dis.close();
            } catch (IOException ignored) {
            }
            try {
                bis.close();
            } catch (IOException ignored) {
            }
            try {
                assert formatter != null;
                formatter.close();
            } catch (Exception ignored) {
            }
        }
    }
}

