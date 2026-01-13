import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import java.util.*;

public class JsonToPactDslStandalone {
    
    public static void main(String[] args) throws Exception {
        // Read JSON from file or stdin
        String json = readJsonInput();
        
        PactDslJsonBody body = parseJsonToDsl(json);
        System.out.println("SUCCESS! Generated Pact DSL:");
        System.out.println(body.getBody());
    }
    
    public static String readJsonInput() throws Exception {
        // Try command line arg first
        if (args.length > 0) {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(args[0]));
        }
        
        // Try input.json file
        if (java.nio.file.Files.exists(java.nio.file.Paths.get("input.json"))) {
            return java.nio.file.Files.readString(java.nio.file.Paths.get("input.json"));
        }
        
        // Default example
        return """
            {
              "status": "ON",
              "items": [{"id": 1, "name": "A"}, {"id": 2, "name": "B"}],
              "code": 200,
              "tags": ["java", "pact"]
            }
            """;
    }
    
    public static PactDslJsonBody parseJsonToDsl(String json) {
        // Simple manual JSON parser (no external libs)
        Map<String, Object> jsonMap = parseJson(json);
        PactDslJsonBody body = new PactDslJsonBody();
        buildDsl(body, jsonMap);
        return body;
    }
    
    @SuppressWarnings("unchecked")
    private static void buildDsl(PactDslJsonBody body, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                PactDslJsonBody child = body.object(key);
                buildDsl(child, (Map<String, Object>) value);
                child.closeObject();
            } else if (value instanceof List) {
                handleArray(body, key, (List<Object>) value);
            } else {
                handlePrimitive(body, key, value);
            }
        }
    }
    
    private static void handleArray(PactDslJsonBody body, String name, List<Object> array) {
        if (array.isEmpty()) {
            body.eachLike(name, PactDslJsonRootValue.stringType("")).closeArray();
            return;
        }
        Object first = array.get(0);
        if (first instanceof Map) {
            PactDslJsonBody elem = body.eachLike(name);
            buildDsl(elem, (Map<String, Object>) first);
            elem.closeObject().closeArray();
        } else {
            if (first instanceof String) {
                body.eachLike(name, PactDslJsonRootValue.stringType((String)first)).closeArray();
            } else if (first instanceof Number) {
                body.eachLike(name, PactDslJsonRootValue.numberType(((Number)first).longValue())).closeArray();
            } else {
                body.eachLike(name, PactDslJsonRootValue.stringType(first.toString())).closeArray();
            }
        }
    }
    
    private static void handlePrimitive(PactDslJsonBody body, String name, Object value) {
        if (value instanceof String) body.stringType(name, (String)value);
        else if (value instanceof Number) body.numberType(name, ((Number)value).longValue());
        else if (value instanceof Boolean) body.booleanType(name, (Boolean)value);
        else body.stringType(name, value.toString());
    }
    
    // CRUDE JSON PARSER - handles basic cases
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        json = json.trim();
        if (!json.startsWith("{")) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        int braceCount = 0;
        
        for (int i = 1; i < json.length() - 1; i++) {
            char c = json.charAt(i);
            
            if (c == '"' && json.charAt(i-1) != '\\') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == ':' && inKey) {
                    inKey = false;
                    continue;
                }
                if (c == ',' || c == '}') {
                    Object val = parseValue(value.toString().trim());
                    result.put(key.toString().trim().replace("\"", ""), val);
                    key = new StringBuilder();
                    value = new StringBuilder();
                    inKey = true;
                    if (c == '}') break;
                    continue;
                }
            }
            
            if (inKey) key.append(c);
            else value.append(c);
        }
        return result;
    }
    
    private static Object parseValue(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length()-1);
        }
        if ("true".equals(s)) return true;
        if ("false".equals(s)) return false;
        if ("null".equals(s)) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e2) {
                return s; // string
            }
        }
    }
}
