package com.example.sca.ui.cloud.utils.encryptalgorithm.utils;

import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;

public class KeyEncryptedUtil{

    // tuning parameters
    // these sizes are relatively arbitrary
    private static final int hashBytes = 16; // AES加密密钥要求16位

    // increase iterations as high as your performance can tolerate
    // since this increases computational cost of password guessing
    // which should help security
    private static final int iterations = 1000;



    public static byte[] keysalt(String key, byte[] salt) { // 输入密钥 生成加盐密钥

        PKCS5S2ParametersGenerator kdf = new PKCS5S2ParametersGenerator();

        kdf.init(key.getBytes(StandardCharsets.UTF_8), salt, iterations);

        byte[] hash = ((KeyParameter) kdf.generateDerivedMacParameters(8*hashBytes)).getKey();

        return hash;
    }




}
