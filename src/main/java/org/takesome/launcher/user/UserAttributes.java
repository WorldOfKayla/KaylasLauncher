package org.takesome.launcher.user;

import java.lang.reflect.Field;
import java.util.Map;

public class UserAttributes {

    @SuppressWarnings("unused")
    String login, password, token, uuid, colorScheme, userAction, groupName, userFullName;
    Object group;
    boolean rememberMe;

    UserAttributes(User user) {
        refreshFromMap(user.getAuth().getAuthCredentials());
    }

    void refreshFromMap(Map<String, Object> sourceMap) {
        if (sourceMap == null) {
            return;
        }
        populateFieldsFromMap(sourceMap);
    }

    private void populateFieldsFromMap(Map<String, Object> sourceMap) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (sourceMap.containsKey(fieldName)) {
                    Object value = sourceMap.get(fieldName);
                    Object convertedValue = convertValue(field.getType(), value);
                    field.set(this, convertedValue);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error setting field values", e);
        }
    }

    private Object convertValue(Class<?> targetType, Object value) {
        if (value == null) {
            return targetType == boolean.class ? false : null;
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        return targetType.cast(value);
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public String getUuid() {
        return uuid;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public String getUserAction() {
        return userAction;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public Object getGroup() {
        return group;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }
}
