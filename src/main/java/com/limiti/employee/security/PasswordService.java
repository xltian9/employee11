package com.limiti.employee.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码散列服务
 *
 * <p>新密码一律使用 BCrypt（自带盐、可调强度），同时对历史上用无盐 MD5 存的数据保持兼容：
 * 校验通过后由调用方触发一次“升级重存”，数据就会平滑迁移到 BCrypt。</p>
 *
 * @author Limiti
 */
@Component
public class PasswordService {

    /** BCrypt 编码器（强度 10，Spring 默认） */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 生成密码散列
     */
    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * 校验密码。stored 为 BCrypt 时走 BCrypt，为 32 位十六进制时按历史 MD5 兼容
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }
        String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return md5.equalsIgnoreCase(storedPassword);
    }

    /**
     * 该散列是否需要升级为 BCrypt
     */
    public boolean needsUpgrade(String storedPassword) {
        return storedPassword == null || storedPassword.isEmpty() || !isBcrypt(storedPassword);
    }

    private boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
