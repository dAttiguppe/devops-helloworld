import java.io.*;
import java.util.*;

public class JsonToLambdaDsl {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToLambdaDsl input.json");
            return;
        }
        
        String json = readFile(args[0]);
        JsonNode root = JsonNode.parse(json);
        
        System.out.println("// === AUTO-GENERATED MODULAR DSL ===\n");
        generateChunks(root);
        generateMainBody(root);
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
        Map<String, JsonNode> objects = new LinkedHashMap<>();
        List<JsonNode> arrays = new ArrayList<>();
        String stringVal;
        Long longVal;
        Double doubleVal;
        Boolean boolVal;
        
        static JsonNode parse(String json) {
            return new Parser(json).parseObject();
        }
    }

    static class Parser {
        String json;
        int pos = 0;
        
        Parser(String json) { this.json = json; }
        
        JsonNode parseObject() {
            skipWhitespace();
            if (pos >= json.length() || json.charAt(pos) != '{') throw new RuntimeException("Expected {");
            pos++;
            JsonNode obj = new JsonNode();
            skipWhitespace();
            
            while (pos < json.length() && json.charAt(pos) != '}') {
                String key = parseString();
                skipWhitespace();
                if (pos >= json.length() || json.charAt(pos) != ':') throw new RuntimeException("Expected :");
                pos++;
                skipWhitespace();
                JsonNode value = parseValue();
                obj.objects.put(key, value);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            pos++; // skip closing }
            return obj;
        }
        
        JsonNode parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseStringNode();
            if (c == 't' || c == 'f') return parseBoolean();
            if (Character.isDigit(c) || c == '-') return parseNumber();
            return null;
        }
        
        JsonNode parseArray() {
            pos++; // [
            JsonNode arr = new JsonNode();
            skipWhitespace();
            while (pos < json.length() && json.charAt(pos) != ']') {
                JsonNode item = parseValue();
                if (item != null) arr.arrays.add(item);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            pos++; // ]
            return arr;
        }
        
        JsonNode parseStringNode() {
            JsonNode n = new JsonNode();
            n.stringVal = parseString();
            return n;
        }
        
        String parseString() {
            if (pos >= json.length() || json.charAt(pos) != '"') return "";
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != '"') {
                char c = json.charAt(pos++);
                if (c == '\\' && pos < json.length()) {
                    c = json.charAt(pos++);
                    sb.append(c);
                } else {
                    sb.append(c);
                }
            }
            if (pos < json.length()) pos++; // skip "
            return sb.toString();
        }
        
        JsonNode parseNumber() {
            JsonNode n = new JsonNode();
            int start = pos;
            while (pos < json.length() && 
                   (Character.isDigit(json.charAt(pos)) || 
                    "+-eE.".indexOf(json.charAt(pos)) >= 0)) pos++;
            String num = json.substring(start, pos);
            try {
                n.longVal = Long.parseLong(num);
            } catch (Exception e) {
                try {
                    n.doubleVal = Double.parseDouble(num);
                } catch (Exception ex) {}
            }
            return n;
        }
        
        JsonNode parseBoolean() {
            JsonNode n = new JsonNode();
            if (pos + 4 <= json.length() && json.startsWith("true", pos)) {
                n.boolVal = true; pos += 4;
            } else if (pos + 5 <= json.length() && json.startsWith("false", pos)) {
                n.boolVal = false; pos += 5;
            }
            return n;
        }
        
        void skipWhitespace() {
            while (pos < json.length() && " \t\n\r".indexOf(json.charAt(pos)) >= 0) pos++;
        }
    }

    static void generateChunks(JsonNode root) {
        for (Map.Entry<String, JsonNode> entry : root.objects.entrySet()) {
            String key = entry.getKey();
            JsonNode node = entry.getValue();
            if (node != null && node.objects.size() > 1) {
                String name = camelCase(key);
                System.out.println("// === " + name.toUpperCase() + "DSL ===\n");
                System.out.println("private static PactDslJsonBody " + name + "Dsl() {");
                System.out.println("  return PactDslJsonBody.object()");
                generateDsl(node, "    ");
                System.out.println("    .closeObject();");
                System.out.println("}");
                System.out.println();
            }
        }
    }

    static void generateMainBody(JsonNode root) {
        System.out.println("// === MAIN BODY ===\n");
        System.out.println("PactDslJsonBody body = PactDslJsonBody.object()");
        generateDsl(root, "  ");
        System.out.println("    .asBody();");
    }

    static void generateDsl(JsonNode node, String indent) {
        for (Map.Entry<String, JsonNode> entry : node.objects.entrySet()) {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value == null) continue;
            
            if (value.stringVal != null && !value.stringVal.isEmpty()) {
                System.out.printf("%s.stringType(\"%s\", \"%s\")\n", indent, key, escape(value.stringVal));
            } else if (value.longVal != null) {
                System.out.printf("%s.numberType(\"%s\", %dL)\n", indent, key, value.longVal);
            } else if (value.doubleVal != null) {
                System.out.printf("%s.numberType(\"%s\", %f)\n", indent, key, value.doubleVal);
            } else if (value.boolVal != null) {
                System.out.printf("%s.booleanType(\"%s\", %b)\n", indent, key, value.boolVal);
            } else if (!value.objects.isEmpty()) {
                System.out.printf("%s.object(\"%s\")\n", indent, key);
                generateDsl(value, indent + "  ");
                System.out.printf("%s.closeObject()\n", indent);
            } else if (!value.arrays.isEmpty()) {
                handleArray(key, value, indent);
            }
        }
    }

    static void handleArray(String key, JsonNode arr, String indent) {
        System.out.printf("%s.eachLike(\"%s\")\n", indent, key);
        if (!arr.arrays.isEmpty()) {
            JsonNode first = arr.arrays.get(0);
            if (first != null && !first.objects.isEmpty()) {
                System.out.println(indent + "  .object()");
                generateDsl(first, indent + "    ");
                System.out.printf("%s  .closeObject()\n", indent);
            }
        }
        System.out.printf("%s.closeArray()\n", indent);
    }

    static String camelCase(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
