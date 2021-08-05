package com.ftseoul.visitor.encrypt;

import com.ftseoul.visitor.config.EncryptConfig;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class Seed {

    @Value("${encrypt.seed}")
    private String key;

    @Value("${encrypt.seed.init}")
    private String IV;

    private final EncryptConfig encryptConfig;

    private byte[] pbszUserKey;
    private byte[] pbszIV;

    public Seed(EncryptConfig encryptConfig) {
        this.encryptConfig = encryptConfig;
        pbszUserKey = encryptConfig.getKey().getBytes(StandardCharsets.UTF_8);
        pbszIV = encryptConfig.getIV().getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String rawMsg) {
        pbszUserKey = key.getBytes(StandardCharsets.UTF_8);
        pbszIV = IV.getBytes(StandardCharsets.UTF_8);
        byte[] message = rawMsg.getBytes();
        byte[] encryptedMsg = KISA_SEED_CBC.SEED_CBC_Encrypt(pbszUserKey, pbszIV,
                message, 0, message.length);

        return Base64.getEncoder()
                .encodeToString(SerializationUtils.serialize(encryptedMsg));
    }

    private <T> T deserialize(String code, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getDecoder().decode(code)
        ));
    }

    public String decrypt(String encodeMsg) {
        byte[] encryptedMsg = deserialize(encodeMsg, byte[].class);
        byte[] decryptedMsg = KISA_SEED_CBC.SEED_CBC_Decrypt(pbszUserKey, pbszIV,
                encryptedMsg, 0, encryptedMsg.length);
        return new String(decryptedMsg, StandardCharsets.UTF_8);
    }

}