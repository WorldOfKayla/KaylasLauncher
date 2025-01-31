package org.foxesworld.launcher.auth;

import java.util.Map;

public interface AuthListener {
    void onLogin(Map<String, Object> authCredentials); // Уже существует
    void onLoad(Auth auth, Map<String, Object> authCredentials); // Уже существует

    // Новые методы
    void onAuthSuccess(Object data); // Вызывается при успешной авторизации
    void onAuthFailure(Object data); // Вызывается при неудачной авторизации
    void onAuthError(Object data);   // Вызывается при ошибке авторизации
    void onLogOut(Object data);      // Вызывается при выходе из системы
}
