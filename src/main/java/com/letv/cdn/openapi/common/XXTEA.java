package com.letv.cdn.openapi.common;

import org.apache.commons.codec.binary.Base64;

/**
 * User: lichao
 * Date: 2010-11-24
 * Time: 11:43:55
 */
public class XXTEA {

    /**
     * XXXTEA加密
     */
    public static String encrypt(String str, String key) {
        byte[] k = key.getBytes();
        byte[] v = str.getBytes();
        byte[] e = Base64.encodeBase64(encrypt(v, k));
        return encryptBase64URL(new String(e));
    }



    /**
     * XXXTEA解密
     */
    public static String decrypt(String str, String key) {
        byte[] k = key.getBytes();
        byte[] v = decryptBase64URL(str).getBytes();
        byte[] e = decrypt(Base64.decodeBase64(v), k);
        return new String(e);
    }



    /**
     * 使Base64编码适应URL传输
     */
    private static String encryptBase64URL(String str) {
        return str.replace('+', '-').replace('/', '_').replace('=', '~');
    }



    /**
     * 还原从URL传输的Based64数据
     */
    private static String decryptBase64URL(String str) {
        return str.replace('-', '+').replace('_', '/')
            .replace('~', '=');  /*星号在做为文件名时有陷井，用~号替换*/
    }



    /**
     * Encrypt data with key.
     *
     * @param data
     * @param key
     * @return
     */
    private static byte[] encrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        return toByteArray(encrypt(toIntArray(data, true), toIntArray(key, false)), false);
    }



    /**
     * Decrypt data with key.
     *
     * @param data
     * @param key
     * @return
     */
    private static byte[] decrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        return toByteArray(decrypt(toIntArray(data, false), toIntArray(key, false)), true);
    }



    /**
     * Encrypt data with key.
     *
     * @param v
     * @param k
     * @return
     */
    private static int[] encrypt(int[] v, int[] k) {
        int n = v.length - 1;

        if (n < 1) {
            return v;
        }
        if (k.length < 4) {
            int[] key = new int[4];

            System.arraycopy(k, 0, key, 0, k.length);
            k = key;
        }
        int z = v[n], y = v[0], delta = 0x9E3779B9, sum = 0, e;
        int p, q = 6 + 52 / (n + 1);

        while (q-- > 0) {
            sum = sum + delta;
            e = sum >>> 2 & 3;
            for (p = 0; p < n; p++) {
                y = v[p + 1];
                z = v[p] += (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            }
            y = v[0];
            z = v[n] += (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
        }
        return v;
    }



    /**
     * Decrypt data with key.
     *
     * @param v
     * @param k
     * @return
     */
    private static int[] decrypt(int[] v, int[] k) {
        int n = v.length - 1;

        if (n < 1) {
            return v;
        }
        if (k.length < 4) {
            int[] key = new int[4];

            System.arraycopy(k, 0, key, 0, k.length);
            k = key;
        }
        int z = v[n], y = v[0], delta = 0x9E3779B9, sum, e;
        int p, q = 6 + 52 / (n + 1);

        sum = q * delta;
        while (sum != 0) {
            e = sum >>> 2 & 3;
            for (p = n; p > 0; p--) {
                z = v[p - 1];
                y = v[p] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            }
            z = v[n];
            y = v[0] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            sum = sum - delta;
        }
        return v;
    }



    /**
     * Convert byte array to int array.
     *
     * @param data
     * @param includeLength
     * @return
     */
    private static int[] toIntArray(byte[] data, boolean includeLength) {
        int n = (((data.length & 3) == 0) ? (data.length >>> 2) : ((data.length >>> 2) + 1));
        int[] result;

        if (includeLength) {
            result = new int[n + 1];
            result[n] = data.length;
        } else {
            result = new int[n];
        }
        n = data.length;
        for (int i = 0; i < n; i++) {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }



    /**
     * Convert int array to byte array.
     *
     * @param data
     * @param includeLength
     * @return
     */
    private static byte[] toByteArray(int[] data, boolean includeLength) {
        int n = data.length << 2;
        if (includeLength) {
            int m = data[data.length - 1];

            if (m > n) {
                return null;
            } else {
                n = m;
            }
        }
        byte[] result = new byte[n];

        for (int i = 0; i < n; i++) {
            result[i] = (byte) ((data[i >>> 2] >>> ((i & 3) << 3)) & 0xff);
        }
        return result;
    }



    public static void main(String[] args) {
        String source = "{\"id\":23,\"name\":\"abc123\"}";
        String abc123 = XXTEA.encrypt(source, "1234567890abcdef");
        System.out.println(source + " > " + abc123);
        String decrypt = XXTEA.decrypt(abc123, "1234567890abcdef");
        System.out.println(abc123 + " > " + decrypt);
        System.out.println("source.equals(decrypt) = " + source.equals(decrypt));
    }
}
