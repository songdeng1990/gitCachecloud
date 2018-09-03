package com.sohu.cache.util;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


public class SecurityUtil {

	private static String key = "lOIXdkaD_2k7sJ";

	private static final int SALT_LENGHT = 4;

	private static final int SALT_START_INDEX = 3;
	
	/**
	 * AES加密
	 * 
	 * @param content
	 *            待加密的内容
	 * @param encryptKey
	 *            加密密钥
	 * @return 加密后的byte[]
	 * @throws Exception
	 */
	public static byte[] aesEncryptToBytes(String content, String encryptKey) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(encryptKey.getBytes("utf8"));
		kgen.init(128, secureRandom);

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
		byte[] bytes = content.getBytes("utf-8");
		
		return cipher.doFinal(myEncryptBytes(bytes));
	}

	/**
	 * AES加密为base 64 code
	 * 
	 * @param content
	 *            待加密的内容
	 * @param encryptKey
	 *            加密密钥
	 * @return 加密后的base 64 code
	 * @throws Exception
	 */
	public static String aesEncrypt(String content, String encryptKey)  {
		try {
			if (StringUtils.isEmpty(content)){
				return content;
			}
			return Base64.encode(aesEncryptToBytes(content, encryptKey));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}

	/**
	 * AES解密
	 * 
	 * @param encryptBytes
	 *            待解密的byte[]
	 * @param decryptKey
	 *            解密密钥
	 * @return 解密后的String
	 * @throws Exception
	 */
	public static String aesDecryptByBytes(byte[] encryptBytes, String decryptKey) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(decryptKey.getBytes("utf8"));
		kgen.init(128, secureRandom);

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
		byte[] decryptBytes = cipher.doFinal(encryptBytes);
		
		return new String(myDecryBytes(decryptBytes),"utf8");
	}
	
	/**
	 * 将base 64 code AES解密
	 * 
	 * @param encryptStr
	 *            待解密的base 64 code
	 * @param decryptKey
	 *            解密密钥
	 * @return 解密后的string
	 * @throws Exception
	 */
	public static String aesDecrypt(String encryptStr, String decryptKey) {
		try {
			if (StringUtils.isEmpty(encryptStr)){
				return encryptStr;								
			}			
			return StringUtils.isEmpty(encryptStr) ? null : aesDecryptByBytes(Base64.decode(encryptStr), decryptKey);
		} catch (Exception e) {
			throw new RuntimeException(e);			
		}
	}
	
	public static String decrypt(String encryptStr){
		return aesDecrypt(encryptStr, key);
	}
	
	public static String encrypt(String content){
		return aesEncrypt(content, key);
	}

	private static byte[] myEncryptBytes(byte[] bytes) {
		byte[] rs = new byte[bytes.length + SALT_LENGHT];
		byte[] salt = new byte[SALT_LENGHT];
		Random random = new Random();
		random.nextBytes(salt);
		byte[] tmp1 = byteMerger(subBytes(bytes, 0, SALT_START_INDEX),salt);
		return byteMerger(tmp1,  subBytes(bytes, SALT_START_INDEX, bytes.length - SALT_START_INDEX));
	}

	private static byte[] myDecryBytes(byte[] bytes) {
		return byteMerger(subBytes(bytes, 0, SALT_START_INDEX), subBytes(bytes, SALT_START_INDEX + SALT_LENGHT, bytes.length - SALT_START_INDEX - SALT_LENGHT));
	}

	private static byte[] byteMerger(byte[] bt1, byte[] bt2) {
		byte[] bt3 = new byte[bt1.length + bt2.length];
		int i = 0;
		for (byte bt : bt1) {
			bt3[i] = bt;
			i++;
		}

		for (byte bt : bt2) {
			bt3[i] = bt;
			i++;
		}
		return bt3;
	}
	
	 private static byte[] subBytes(byte[] src, int begin, int count) {
	        byte[] bs = new byte[count];
	        for (int i=begin;i<begin+count; i++) bs[i-begin] = src[i];
	        return bs;
	 }
	 
	 public static void main(String[] args) throws Exception {
		String encrytString = aesEncrypt("mysq:passwd", key);
		System.out.println(encrytString);
		String decryString = aesDecrypt(encrytString, key);
		System.out.println(decryString);
		/*String s = "abcdefg";
		byte[] bytes = myEncryptBytes(s.getBytes("utf8"));
		System.out.println(bytes.length);
		byte[] decy = myDecryBytes(bytes);
		System.out.println(new String(decy,"utf8"));*/
		
	}

}
