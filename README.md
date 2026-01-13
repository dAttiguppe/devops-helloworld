# 1. Save as JsonToLambdaDsl.java
# 2. Create input## **Standalone JsonToLambdaDsl - No External Dependencies!**

**Single file with embedded Jackson alternative using `javax.json`:**

```java
import javax.json.*;
import javax.json.stream.JsonParser;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class JsonToLambdaDsl {
    private static final int MAX_LINES_PER_CHUNK = 8;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToLambdaDsl <input.json>");
            return;
        }

        String jsonContent = Files.readString(new File(args).toPath());
        JsonStructure root = parseJson(jsonContent);

        System.out.println("// === AUTO-GENERATED MODULAR DSL ===\n");
        
        // Generate chunks + main body
        Map<String, String> chunks = generateChunks(root);
        generateMainBody(root, chunks);
    }

    private static Map<String, String> generateChunks(JsonStructure root) {
        Map<String, String> chunks = new LinkedHashMap<>();
        List<JsonStructure> candidates = findChunkCandidates(root);
        
        for (JsonStructure candidate : candidates) {
            String chunkName = createChunkName(candidate);
            String dslCode = generateChunkDsl(candidate, 2);
            chunks.put(chunkName, dslCode);
            
            printChunk(chunkName, dslCode);
        }
        return chunks;
    }

    private static void printChunk(String chunkName, String dslCode) {
        System.out.println("// === " + chunkName.toUpperCase() + " ===\n");
        System.out.println("private static PactDslJsonBody " + chunkName + "() {");
        System.out.println("  return PactDslJsonBody.object()");
        System.out.print(dslCode);
        System.out.println("    .closeObject();");
        System.out.println("}");
        System.out.println();
    }

    private static List<JsonStructure> findChunkCandidates(JsonStructure root) {
        List<JsonStructure> candidates = new ArrayList<>();
        traverse(root, 0, node -> {
            if (shouldChunk(node)) {
                candidates.add(node);
            }
        });
        return candidates;
    }

    private static boolean shouldChunk(JsonStructure node) {
        if (!(node instanceof JsonObject) && !(node instanceof JsonArray)) return false;
        // Chunk complex structures
        return node.asJsonObject().size() > 3 || node instanceof JsonArray;
    }

    private static String generateChunkDsl(JsonStructure node, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        String indent = "    ".repeat(indentLevel);
        
        if (node instanceof JsonObject obj) {
            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                sb.append(generateFieldDsl(entry.getKey(), entry.getValue(), indent));
            }
        }
        return sb.toString();
    }

    private static String generateFieldDsl(String key, JsonValue value, String indent) {
        return switch (value.getValueType()) {
            case STRING -> String.format("%s.stringType(\"%s\", \"%s\")\n", 
                indent, key, escape(value.asJsonString().getString()));
            case NUMBER -> {
                if (value.asJsonNumber().isIntegral()) {
                    yield String.format("%s.numberType(\"%s\", %dL)\n", 
                        indent, key, value.asJsonNumber().longValue());
                } else {
                    yield String.format("%s.numberType(\"%s\", %f)\n", 
                        indent, key, value.asJsonNumber().doubleValue());
                }
            }
            case TRUE, FALSE -> String.format("%s.booleanType(\"%s\", %b)\n", 
                indent, key, value.asJsonBoolean().getBoolean());
            case OBJECT -> String.format("%s.object(\"%s\")\n", indent, key) +
                generateChunkDsl(value.asJsonObject(), 3).replaceAll("    ", "      ") +
                String.format("%s.closeObject()\n", indent);
            case ARRAY -> handleArrayDsl(key, (JsonArray)value, indent);
            default -> "";
        };
    }

    private static String handleArrayDsl(String key, JsonArray array, String indent) {
        if (array.isEmpty()) {
            return String.format("%s.eachLike(\"%s\").closeArray()\n", indent, key);
        }
        
        JsonValue first = array.get(0);
        if (first instanceof JsonObject) {
            return String.format("%s.eachLike(\"%s\")\n      .object()\n", indent, key) +
                generateChunkDsl(first.asJsonObject(), 4).replaceAll("    ", "        ") +
                String.format("%s.closeObject().closeArray()\n", indent);
        } else {
            return String.format("%s.eachLike(\"%s\", PactDslJsonRootValue.stringType(\"%s\")).closeArray()\n",
                indent, key, first.asJsonString().getString());
        }
    }

    private static void generateMainBody(JsonStructure root, Map<String, String> chunks) {
        System.out.println("// === MAIN BODY ===\n");
        System.out.println("PactDslJsonBody body = PactDslJsonBody.object()");
        
        if (root instanceof JsonObject obj) {
            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                String chunkName = camelCase(entry.getKey());
                if (chunks.containsKey(chunkName)) {
                    System.out.println("    ." + referenceChunk(chunkName) + ";");
                } else {
                    System.out.print(generateFieldDsl(entry.getKey(), entry.getValue(), "  "));
                }
            }
        }
        
        System.out.println("    .asBody();");
    }

    private static String referenceChunk(String chunkName) {
        return switch (chunkName) {
            case "user" -> "object(\"user\", userDsl())";
            case "items" -> "eachLike(\"items\", itemsDsl())";
            default -> "object(\"" + chunkName + "\", " + chunkName + "())";
        };
    }

    private static JsonStructure parseJson(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.read();
        }
    }

    private static void traverse(JsonStructure node, int depth, Consumer<JsonStructure> visitor) {
        visitor.accept(node);
        if (node instanceof JsonObject obj) {
            for (JsonValue value : obj.values()) {
                traverse(value.asJsonObject(), depth + 1, visitor);
            }
        } else if (node instanceof JsonArray arr) {
            for (JsonValue value : arr) {
                traverse(value.asJsonObject(), depth + 1, visitor);
            }
        }
    }

    private static String camelCase(String s) { 
        return Character.toLowerCase(s.charAt(0)) + s.substring(1); 
    }
    
    private static String escape(String s) { 
        return s.replace("\\", "\\\\").replace("\"", "\\\""); 
    }

    @FunctionalInterface
    interface Consumer<T> {
        void accept(T t);
    }
}
