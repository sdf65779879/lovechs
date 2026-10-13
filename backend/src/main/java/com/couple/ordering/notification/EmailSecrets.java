package com.couple.ordering.notification;

import com.couple.ordering.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailSecrets {
    private final String configuredKey;

    public EmailSecrets(@Value("${couple.email.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length != 32) throw new IllegalArgumentException();
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException e) {
            throw new BusinessException("请在部署环境设置 MAIL_ENCRYPTION_KEY（32 字节随机密钥的 Base64）");
        }
    }

    public String encrypt(String password) {
        SecretKeySpec key = key();
        try {
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return "v1:" + Base64.getEncoder().encodeToString(nonce) + ":"
                    + Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("邮箱授权码加密失败，请检查部署配置");
        }
    }

    public String decrypt(String encrypted) {
        SecretKeySpec key = key();
        try {
            String[] parts = encrypted.split(":", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(128, Base64.getDecoder().decode(parts[1])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("邮箱授权码无法解密，请核对 MAIL_ENCRYPTION_KEY 或重新保存授权码");
        }
    }
}
