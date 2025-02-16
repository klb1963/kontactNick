package kontactNick.security.util;

import com.google.gson.Gson;
import java.util.Base64;
import java.util.Map;

public class JwtUtils {
    public static String extractEmailFromIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid ID Token");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Преобразуем JSON в Map и получаем email
            Map<String, Object> payloadMap = new Gson().fromJson(payloadJson, Map.class);
            return (String) payloadMap.get("email");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
