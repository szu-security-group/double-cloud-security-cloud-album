package com.example.sca.ui.cloud.utils;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class FileHashUtil {
    /**
     * 计算文件hash值
     */
    private static final String HMAC_KEY = "MyHMACPassword";


    public static String hmacFiletoString(String path) throws Exception {
        File file = new File(path);
        FileInputStream fis = null;
        String hmac = null;
        try {
            SecretKey secretKey = new SecretKeySpec(HMAC_KEY.getBytes(),"HmacMD5");
            fis = new FileInputStream(file);
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(secretKey);
            byte buffer[] = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                mac.update(buffer, 0, length);
            }
            byte[] digest = mac.doFinal();
            hmac = byte2hexLower(digest);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("计算文件hash值错误");
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hmac;
    }

    public static byte[] hmacFiletoByte(String path) throws Exception {
        File file = new File(path);
        FileInputStream fis = null;
        byte[] digest = null;
        try {
            SecretKey secretKey = new SecretKeySpec(HMAC_KEY.getBytes(),"HmacMD5");
            fis = new FileInputStream(file);
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(secretKey);
            byte buffer[] = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                mac.update(buffer, 0, length);
            }
            digest = mac.doFinal();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("计算文件hash值错误");
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return digest;
    }



    public static String byte2hexLower(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int i = 0; i < b.length; i++) {
            stmp = Integer.toHexString(b[i] & 0XFF);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs;
    }



    public static String getFileName(String pathandname){
        int  start=pathandname.lastIndexOf( "/" );
        int  end=pathandname.lastIndexOf( "." );
        if  (start!=- 1  && end!=- 1 ) {
            return  pathandname.substring(start+ 1 , end);
        }
        else  {
            return  null ;
        }
    }

    /**
     * 获取文件名及后缀
     */
    public static String getFileNameWithSuffix(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        int start = path.lastIndexOf("/");
        if (start != -1) {
            return path.substring(start + 1);
        } else {
            return "";
        }
    }
}
