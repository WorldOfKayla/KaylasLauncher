package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class md5Func {
    public static String md5(String filename) {
        if (new File(filename).isDirectory()) {
            Engine.LOGGER.info("RUNNING IN IDE!!!");
            return "IDE";
        }
        FileInputStream fis = null;
        FilterInputStream dis = null;
        BufferedInputStream bis = null;
        Formatter formatter = null;
        System.out.println(filename);
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
            String string = formatter.toString();
            return string;

        } catch (IOException | NoSuchAlgorithmException e) {
            String string = "0";
            return string;
        } finally {
            try {
                fis.close();
            } catch (IOException ignored) {
            }
            try {
                dis.close();
            } catch (IOException ignored) {
            }
            try {
                bis.close();
            } catch (IOException ignored) {
            }
            try {
                formatter.close();
            } catch (Exception ignored) {
            }
        }
    }
}

