public class JsonToLambdaDsl {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_LINES_PER_CHUNK = 8;

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        JsonNode root = mapper.readTree(file);

        System.out.println("// === AUTO-GENERATED MODULAR DSL ===\n");
        
        // Generate small reusable chunks
        Map<String, String> chunks = generateChunks(root);
        
        // Generate main body
        generateMainBody(root, chunks);
    }

    private static Map<String, String> generateChunks(JsonNode root) {
        Map<String, String> chunks = new LinkedHashMap<>();
        List<JsonNode> chunkCandidates = findChunkCandidates(root);
        
        for (JsonNode candidate : chunkCandidates) {
            String chunkName = createChunkName(candidate);
            String dslCode = generateChunkDsl(candidate, 2);
            chunks.put(chunkName, dslCode);
            
            System.out.println("// === " + chunkName.toUpperCase() + " ===\n");
            System.out.println("private static PactDslJsonBody " + chunkName(chunkName) + "() {");
            System.out.println("  return PactDslJsonBody.object()");
            System.out.print(dslCode);
            System.out.println("    .closeObject();");
            System.out.println("}");
            System.out.println();
        }
        
        return chunks;
    }

    private static String generateChunkDsl(JsonNode node, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        String indent = "    ".repeat(indentLevel);
        
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                sb.append(generateFieldDsl(field.getKey(), field.getValue(), indent));
            }
        }
        return sb.toString();
    }

    private static String generateFieldDsl(String key, JsonNode value, String indent) {
        if (value.isTextual()) {
            return String.format("%s.stringType(\"%s\", \"%s\")\n", indent, key, escape(value.asText()));
        } else if (value.isNumber()) {
            if (value.isIntegralNumber()) {
                return String.format("%s.numberType(\"%s\", %dL)\n", indent, key, value.asLong());
            } else {
                return String.format("%s.numberType(\"%s\", %f)\n", indent, key, value.asDouble());
            }
        } else if (value.isBoolean()) {
            return String.format("%s.booleanType(\"%s\", %b)\n", indent, key, value.asBoolean());
        } else if (value.isObject()) {
            return String.format("%s.object(\"%s\")\n", indent, key) +
                   generateChunkDsl(value, 3).replaceAll("    ", "      ") +
                   String.format("%s.closeObject()\n", indent);
        } else if (value.isArray()) {
            return handleArrayDsl(key, value, indent);
        }
        return "";
    }

    private static String handleArrayDsl(String key, JsonNode array, String indent) {
        if (array.size() == 0) {
            return String.format("%s.eachLike(\"%s\").closeArray()\n", indent, key);
        }
        
        JsonNode first = array.get(0);
        if (first.isObject()) {
            return String.format("%s.eachLike(\"%s\")\n", indent, key) +
                   "      .object()\n" +
                   generateChunkDsl(first, 4).replaceAll("    ", "        ") +
                   String.format("    %s.closeObject().closeArray()\n", indent.replaceAll("    ", ""));
        } else {
            return String.format("%s.eachLike(\"%s\", PactDslJsonRootValue.stringType(\"%s\")).closeArray()\n",
                indent, key, first.asText());
        }
    }

    private static String generateMainBody(JsonNode root, Map<String, String> chunks) {
        System.out.println("// === MAIN BODY ===\n");
        System.out.println("PactDslJsonBody body = PactDslJsonBody.object()");
        
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String chunkName = camelCase(field.getKey());
            if (chunks.containsKey(chunkName)) {
                System.out.println("    ." + referenceChunk(chunkName) + ";");
            } else {
                System.out.print(generateFieldDsl(field.getKey(), field.getValue(), "  "));
            }
        }
        
        System.out.println("    .asBody();");
    }

    private static String referenceChunk(String chunkName) {
        return switch (chunkName) {
            case "user" -> "object(\"user\", userDsl())";
            case "items" -> "eachLike(\"items\", itemsDsl())";
            case "address" -> "object(\"address\", addressDsl())";
            default -> "object(\"" + chunkName + "\", " + chunkName(chunkName) + "())";
        };
    }

    private static String camelCase(String s) { return Character.toLowerCase(s.charAt(0)) + s.substring(1); }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
