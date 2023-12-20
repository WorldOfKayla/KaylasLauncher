package org.foxesworld.engine.utils.Crypt;

import org.foxesworld.engine.Engine;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CryptUtils {

    private final CryptHelper cryptHelper;
    private final Engine engine;

    public CryptUtils(Engine engine) {
        this.cryptHelper = new CryptHelper();
        this.engine = engine;
    }

    public String decrypt(String input, String key) {
        byte[] output = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, skey);
            output = cipher.doFinal(cryptHelper.getDecoder().decode(input));
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            this.engine.getLOGGER().error("Key is not valid for: " + input);
        }
        assert output != null;
        return new String(output);
    }

    public String encrypt(String input, String key) {
        byte[] crypted = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(1, skey);
            crypted = cipher.doFinal(input.getBytes());
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            this.engine.getLOGGER().error("Key must be 16 symbols!", 0, true);
        }
        assert crypted != null;
        return new String(cryptHelper.getEncoder().encode(crypted));
    }


}
