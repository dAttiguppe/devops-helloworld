import java.io.*;
import java.util.*;

public class JsonToPactDsl {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToPactDsl input.json");
            return;
        }
        
        try {
            String json = readFile(args[0]);
            JsonNode root = parseJson(json);
            
            System.out.println("📁 INPUT JSON:");
            System.out.println(json);
            System.out.println("\n" + "=".repeat(60));
            
            System.out.println("\n// === GENERATED PACT DSL ===\n");
            System.out.println("PactDslJsonBody body = PactDslJsonBody.object()");
            generateDsl(root, "    ");
            System.out.println("    .asBody();");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static String readFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
    
    static class JsonNode {
        Map<String, Object> fields = new LinkedHashMap<>();
        
        static JsonNode parse(String json) {
            JsonNode root = new JsonNode();
            
            // Extract "key": "value"
            String[] stringPairs = json.split("\"\\s*:\\s*\"");
            for (String pair : stringPairs) {
                if (pair.contains(":") && pair.contains("\"")) {
                    String[] parts = pair.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].replaceAll("[^a-zA-Z0-9_]", "");
                        String value = parts[1].replaceAll("[^a-zA-Z0-9_]", "");
                        if (!key.isEmpty() && !value.isEmpty()) {
                            root.fields.put(key, value);
                        }
                    }
                }
            }
            
            // Extract numbers
            String[] numberPairs = json.split("\"\\s*:\\s*");
            for (String pair : numberPairs) {
                if (pair.matches(".*[0-9].*")) {
                    String[] parts = pair.split(",", 2);
                    String key = parts[0].replaceAll("[^a-zA-Z0-9_]", "");
                    String value = parts[0].replaceAll("[^0-9.]", "");
                    try {
                        if (!key.isEmpty() && !value.isEmpty()) {
                            root.fields.put(key, Long.parseLong(value));
                        }
                    } catch (NumberFormatException e) {}
                }
            }
            
            return root;
        }
    }
    
    static void generateDsl(JsonNode node, String indent) {
        for (Map.Entry<String, Object> entry : node.fields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String strVal) {
                // Enum detection (UPPERCASE)
                if (strVal.matches("[A-Z_]+")) {
                    System.out.printf("%s.string(\"%s\", \"%s\")\n", indent, key, escape(strVal));
                } else {
                    System.out.printf("%s.stringType(\"%s\", \"%s\")\n", indent, key, escape(strVal));
                }
            } else if (value instanceof Long numVal) {
                System.out.printf("%s.numberType(\"%s\", %dL)\n", indent, key, numVal);
            } else if (value instanceof Boolean boolVal) {
                System.out.printf("%s.booleanType(\"%s\", %b)\n", indent, key, boolVal);
            }
        }
    }
    
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
