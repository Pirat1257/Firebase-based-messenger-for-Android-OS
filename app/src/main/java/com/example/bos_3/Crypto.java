package com.example.bos_3;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;


/*--------------------Класс для шифрования и дешифрования данных--------------------*/
/*--------------------Шифрование--------------------*/
public class Crypto {
    public Key publicKey = null;
    private Key privateKey = null;

    /*--------------------Генерация пары--------------------*/
    public String generate_keys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new String(Base64.encode(publicKey.getEncoded(), 0));
    }

    public static byte[] decryptBASE64(String key) throws Exception {
        return Base64.decode(key, Base64.DEFAULT);
    }

    /*--------------------Шифрование с помощью открытого ключа--------------------*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encryptByPublicKey(String str_key, String clear) throws Exception {
        // Необходимо использовать правильную кодировку
        byte[] keyBytes = decryptBASE64(str_key);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key publicKey = keyFactory.generatePublic(x509KeySpec);
        // Encode the original data with RSA public key
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, publicKey);
            encodedBytes = c.doFinal(clear.getBytes());
        } catch (Exception e) {

        }
        return java.util.Base64.getEncoder().encodeToString(encodedBytes);
    }

    /*--------------------Расшифрование с помощью секретного ключа--------------------*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String decryptByPrivateKey(String encrypted) throws Exception {
        // Необходимо использовать правильную кодировку
        byte[] encrypted_bytes = java.util.Base64.getDecoder().decode(encrypted);
        // Decode the encoded data with RSA public key
        byte[] decodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.DECRYPT_MODE, privateKey);
            decodedBytes = c.doFinal(encrypted_bytes);
        } catch (Exception e) {

        }
        return new String(decodedBytes);
    }

    /*--------------------Генерация сеансового ключа--------------------*/
    public String generate_key() {
        // Set up secret key spec for 128-bit AES encryption and decryption
        SecretKeySpec sks = null;
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed("any data used as random seed".getBytes());
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128, sr);
            sks = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {

        }
        return new String(Base64.encode(sks.getEncoded(), 0));
    }

    /*--------------------Шифрование с помощью сеансового ключа--------------------*/
    public static String encrypt(byte[] key, byte[] clear) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("md5"); // md5 - 128-битный алгоритм хеширования, один из серии алгоритмов по построению дайджеста сообщения
        byte[] digestOfPassword = md.digest(key); // Для массива создается дайджест сообщения MessageDigest.
        // Экземпляр секретного ключа SecretKeySpec создается для алгоритма "AES"
        SecretKeySpec skeySpec = new SecretKeySpec(digestOfPassword, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec); // Инициализация Cipher выполняется вызовом его метода init
        byte[] encrypted = cipher.doFinal(clear); // Для шифрования и дешифрования данных с помощью экземпляра Cipher, используется один из методов update() или doFinal().
        return Base64.encodeToString(encrypted,Base64.DEFAULT);
    }

    /*--------------------Расшифрование--------------------*/
    public static String decrypt(String key, byte[] encrypted) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("md5");
        byte[] digestOfPassword = md.digest(key.getBytes("UTF-16LE"));

        SecretKeySpec skeySpec = new SecretKeySpec(digestOfPassword, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-16LE");
    }
}