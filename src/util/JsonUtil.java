package util;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * JsonUtil provides JSON serialization and parsing utilities.
 * 
 * Includes:
 * - Serialization of primitives, strings, numbers, booleans
 * - Object serialization using reflection
 * - Collection and array serialization
 * - Map serialization
 * - Date/Timestamp formatting
 * - JSON string escaping
 * - JSON value extraction for simple parsing
 */
public class JsonUtil {
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Serializes any Java object to JSON string.
     *
     * @param obj the object to serialize
     * @return JSON string representation
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        if (obj instanceof Timestamp) {
            return "\"" + dateFormat.format((Timestamp) obj) + "\"";
        }
        
        if (obj instanceof Date) {
            return "\"" + dateFormat.format((Date) obj) + "\"";
        }
        
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }
        
        if (obj.getClass().isArray()) {
            return arrayToJson((Object[]) obj);
        }
        
        return objectToJson(obj);
    }
    
    /**
     * Serializes a Java object by reflecting its fields.
     *
     * @param obj the object to serialize
     * @return JSON string representation
     */
    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append("\"").append(field.getName()).append("\":");
                    sb.append(toJson(value));
                    first = false;
                }
            } catch (IllegalAccessException e) {
                // Skip
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Serializes a Map to JSON.
     *
     * @param map the map to serialize
     * @return JSON string representation
     */
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Serializes a Collection to JSON array.
     *
     * @param collection the collection to serialize
     * @return JSON string representation
     */
    private static String collectionToJson(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(",");
            }
            sb.append(toJson(item));
            first = false;
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Serializes an array to JSON array.
     *
     * @param array the array to serialize
     * @return JSON string representation
     */
    private static String arrayToJson(Object[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(array[i]));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Escapes special characters in JSON string.
     *
     * @param s the string to escape
     * @return escaped string
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Extracts value for a key from JSON string using simple parsing.
     * Supports quoted string values and unquoted numeric/boolean values.
     *
     * @param json the JSON string
     * @param key  the key whose value to extract
     * @return the extracted value, or null if key not found or parsing fails
     */
    public static String getString(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int start = colonIndex + 1;
        while (start < json.length()) {
            char c = json.charAt(start);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
            start++;
        }
        
        if (start >= json.length()) return null;
        
        char firstChar = json.charAt(start);
        int end;
        
        if (firstChar == '"') {
            start++;
            end = json.indexOf("\"", start);
            if (end == -1) return null;
            return json.substring(start, end);
        } else {
            end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}' || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    break;
                }
                end++;
            }
            return json.substring(start, end).trim();
        }
    }

    /**
     * Extracts array value for a key from JSON string.
     *
     * @param json the JSON string
     * @param key  the key whose array value to extract
     * @return the extracted array string (including brackets), or null if not found
     */
    public static String getArrayString(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int startBracket = json.indexOf("[", colonIndex);
        if (startBracket == -1) return null;
        
        int endBracket = json.indexOf("]", startBracket);
        if (endBracket == -1) return null;
        
        return json.substring(startBracket, endBracket + 1);
    }
}