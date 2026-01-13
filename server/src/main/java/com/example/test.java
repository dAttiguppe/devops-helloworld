import java.io.*;
import java.util.*;

public class JsonToLambdaDsl {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToLambdaDsl input.json");
            return;
        }
        
        String json = Files.readString(new File(args[0]).toPath());
        JsonNode root = parseJson(json);
        
        System.out.println("// === AUTO-GENERATED MODULAR DSL ===\n");
        generateChunks(root);
        generateMainBody(root);
    }

    static class JsonNode {
        Map<String, JsonNode> objects = new LinkedHashMap<>();
        List<JsonNode> arrays = null;
        String stringVal = null;
        Long longVal = null;
        Double doubleVal = null;
        Boolean boolVal = null;
        String key = null;
        
        static JsonNode parse(String json) {
            return new Parser(json.replaceAll("\\s+", "")).parse();
        }
    }

    static class Parser {
        String json;
        int pos = 0;
        
        Parser(String json) { this.json = json; }
        
        JsonNode parse() {
            expect('{');
            JsonNode obj = new JsonNode();
            while (!match('}')) {
                String key = parseString();
                expect(':');
                JsonNode value = parseValue();
                value.key = key;
                obj.objects.put(key, value);
                if (!match(',')) break;
            }
            expect('}');
            return obj;
        }
        
        JsonNode parseValue() {
            if (match('{')) return parse();
            if (match('[')) return parseArray();
            if (match('"')) return parseStringNode();
            if (match('t') || match('f')) return parseBoolean();
            if (isDigit()) return parseNumber();
            throw new RuntimeException("Invalid JSON at " + pos);
        }
        
        JsonNode parseArray() {
            JsonNode arr = new JsonNode();
            arr.arrays = new ArrayList<>();
            while (!match(']')) {
                arr.arrays.add(parseValue());
                if (!match(',')) break;
            }
            return arr;
        }
        
        JsonNode parseStringNode() {
            JsonNode n = new JsonNode();
            n.stringVal = parseString();
            return n;
        }
        
        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!match('"')) {
                char c = json.charAt(pos++);
                if (c == '\\') {
                    c = json.charAt(pos++);
                    if (c == '"' || c == '\\') sb.append(c);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        
        JsonNode parseNumber() {
            JsonNode n = new JsonNode();
            int start = pos;
            while (pos < json.length() && (isDigit() || json.charAt(pos) == '.' || json.charAt(pos) == 'e' || json.charAt(pos) == 'E' || json.charAt(pos) == '-' || json.charAt(pos) == '+')) {
                pos++;
            }
            String num = json.substring(start, pos);
            try {
                n.longVal = Long.parseLong(num);
            } catch (NumberFormatException e) {
                n.doubleVal = Double.parseDouble(num);
            }
            return n;
        }
        
        JsonNode parseBoolean() {
            JsonNode n = new JsonNode();
            if (matchString("true")) n.boolVal = true;
            else if (matchString("false")) n.boolVal = false;
            return n;
        }
        
        boolean match(char c) {
            if (pos < json.length() && json.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }
        
        void expect(char c) {
            if (!match(c)) throw new RuntimeException("Expected " + c + " at " + pos);
        }
        
        boolean matchString(String s) {
            if (json.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            return false;
        }
        
        boolean isDigit() { return pos < json.length() && Character.isDigit(json.charAt(pos)); }
    }

    static void generateChunks(JsonNode root) {
        List<JsonNode> candidates = findChunkCandidates(root);
        for (JsonNode node : candidates) {
            String name = camelCase(node.key);
            System.out.println("// === " + name.toUpperCase() + "DSL ===\n");
            System.out.println("private static PactDslJsonBody " + name + "Dsl() {");
            System.out.println("  return PactDslJsonBody.object()");
            generateDsl(node, "    ");
            System.out.println("    .closeObject();");
            System.out.println("}");
            System.out.println();
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
            
            if (value.stringVal != null) {
                System.out.printf("%s.stringType(\"%s\", \"%s\")\n", indent, key, escape(value.stringVal));
            } else if (value.longVal != null) {
                System.out.printf("%s.numberType(\"%s\", %dL)\n", indent, key, value.longVal);
            } else if (value.doubleVal != null) {
                System.out.printf("%s.numberType(\"%s\", %.2f)\n", indent, key, value.doubleVal);
            } else if (value.boolVal != null) {
                System.out.printf("%s.booleanType(\"%s\", %b)\n", indent, key, value.boolVal);
            } else if (!value.objects.isEmpty()) {
                System.out.printf("%s.object(\"%s\")\n", indent, key);
                generateDsl(value, indent + "  ");
                System.out.printf("%s.closeObject()\n", indent);
            } else if (value.arrays != null && !value.arrays.isEmpty()) {
                handleArray(key, value, indent);
            }
        }
    }

    static void handleArray(String key, JsonNode arr, String indent) {
        System.out.printf("%s.eachLike(\"%s\")\n", indent, key);
        if (!arr.arrays.isEmpty()) {
            JsonNode first = arr.arrays.get(0);
            if (!first.objects.isEmpty()) {
                System.out.println(indent + "  .object()");
                generateDsl(first, indent + "    ");
                System.out.printf("%s  .closeObject()\n", indent);
            }
        }
        System.out.printf("%s.closeArray()\n", indent);
    }

    static List<JsonNode> findChunkCandidates(JsonNode node) {
        List<JsonNode> candidates = new ArrayList<>();
        findChunks(node, candidates);
        return candidates;
    }

    static void findChunks(JsonNode node, List<JsonNode> candidates) {
        if (node.objects.size() > 2) candidates.add(node);
        for (JsonNode child : node.objects.values()) {
            findChunks(child, candidates);
        }
    }

    static String camelCase(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
