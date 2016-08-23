package utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.UUID;

/**
 * Created by lambros on 2/4/14.
 */
public class LPUtils {

    // Used by the secure encryption/decryption algorithms
    private final static int SECURE_ITERATIONS = 1000;
    private final static int SECURE_KEY_LENGTH = 128;

    // used by the secure psedo random number generator
    private final static int PRNG_SEED = 16; // 256 best but very slow

    public static String getRandomUUID(){
        return UUID.randomUUID().toString();
    }

    /**
     * Return a new random string
     */
    public static String generateRandomToken(){
        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(secureRandom.generateSeed(PRNG_SEED));
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] dig = digest.digest((secureRandom.nextLong() + "").getBytes());
            return binaryToHex(dig);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hashStringBase64(String input){
        return new String (Base64.encodeBase64(hashString(input)));
    }

    public static String hashStringHex(String input){
        return binaryToHex(hashString(input));
    }

    public static byte[] hashString(String input){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            md.update( (input + salt).getBytes("UTF-8") );
            byte byteData[] = md.digest();
            return byteData;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Derives the key that will be used by the encryption and decryption
     * algorithms below.
     *
     * site: http://nelenkov.blogspot.ch/2012/04/using-password-based-encryption-on.html
     *
     * @param salt The salt that is being used
     * @param password The password of the encryption
     * @return The SecretKey for the encryption/decryption
     */
    public static SecretKey deriveKeyPbkdf2(byte[] salt, String password){
        int iterationCount = SECURE_ITERATIONS;
        int keyLength = SECURE_KEY_LENGTH;
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,iterationCount, keyLength);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Securely encrypts the plaintext using the password provided.
     *
     * The salt, IV and ciphertext are stored in the final result separated by dot.
     *
     * base64(salt).base64(iv).base64(ciphertext)
     *
     * @param password
     * @param plaintext
     * @return
     */
    public static String secureEncrypt(String password, String plaintext){
        try {
            int keyLength = SECURE_KEY_LENGTH;
            // same size as key output in bytes ( keyLength is in bits )
            int saltLength = keyLength >> 3;

            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[saltLength];
            random.nextBytes(salt);
            SecretKey key = deriveKeyPbkdf2(salt, password);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            //LPLogger.info("bytes: " + new String(salt) + "." + iv + "." + new String(ciphertext));
            String finalResult = new String (Base64.encodeBase64(salt)) + "." + new String (Base64.encodeBase64(iv)) + "." + new String (Base64.encodeBase64(ciphertext));
            //LPLogger.info("final: " + finalResult);
            return finalResult;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Securely decrypts the plaintext using the password provided.
     *
     * The salt, IV and ciphertext are stored in the ciphertext separated by dot.
     *
     * base64(salt).base64(iv).base64(ciphertext)
     *
     * @param password The password used to encrypt the text above
     * @param ciphertext The output of the secureEncrypt function above
     * @return
     */
    public static String secureDecrypt(String password, String ciphertext){
        try {
            String[] fields = ciphertext.split("[.]");
            byte[] salt = Base64.decodeBase64(fields[0].getBytes());
            byte[] iv = Base64.decodeBase64(fields[1].getBytes());
            byte[] cipherBytes = Base64.decodeBase64(fields[2].getBytes());
            //LPLogger.info( salt + "." + iv + "." + cipherBytes);
            SecretKey key = deriveKeyPbkdf2(salt, password);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
            byte[] plaintext = cipher.doFinal(cipherBytes);
            String plainrStr = new String(plaintext , "UTF-8");
            return plainrStr;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String binaryToHex(byte[] ba){
        if( ba == null || ba.length == 0 ){
            return null;
        }
        StringBuilder sb = new StringBuilder(ba.length * 2);
        String hexNumber;
        for( int x=0, sz=ba.length; x<sz; x++ ){
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
            sb.append(hexNumber.substring(hexNumber.length()-1));
        }
        return sb.toString();
    }

    public static byte[] hexToBinary(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }

        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer
                    .parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }

    public static String encodeBase64String(String s){
        try{
            byte[] binary = s.getBytes("UTF-8");
            return new String (Base64.encodeBase64(binary));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decodeBase64String(String sb64){
        try{
            byte[] binary = Base64.decodeBase64(sb64.getBytes());
            return new String(binary,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
