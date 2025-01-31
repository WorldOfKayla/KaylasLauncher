package org.foxesworld.launcher.user;

import java.lang.reflect.Field;
import java.util.Map;

class UserAttributes {

    String login, password, token, uuid, colorScheme, userAction, groupName, userFullName;
    Object group;
    boolean rememberMe;

    UserAttributes(User user) {
        Map<String, Object> credentials = user.getAuth().getAuthCredentials();
        populateFieldsFromMap(credentials);
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
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            return value;
        }
        return targetType.cast(value);
    }

}
