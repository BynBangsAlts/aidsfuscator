package dev.lvstrng.aids.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class AESUtil {
    public static SecretKey getKey() {
        try {
            var gen = KeyGenerator.getInstance("AES");
            gen.init(256, new SecureRandom());
            return gen.generateKey();
        } catch (NoSuchAlgorithmException _) {
            return null;
        }
    }

    public static byte[] getIv() {
        var iv = new byte[16];
        ThreadLocalRandom.current().nextBytes(iv);
        return iv;
    }

    public static String encrypt(String str, byte[] key, byte[] iv) {
        try {
            var c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return Base64.getEncoder().encodeToString(c.doFinal(str.getBytes()));
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static String decrypt(String str, byte[] key, byte[] iv) {
        try {
            var c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            var bytes = Base64.getDecoder().decode(str);

            return new String(c.doFinal(bytes));
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
