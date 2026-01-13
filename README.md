package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class JsonToLambdaDslCode {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java JsonToLambdaDslCode input.json");
            System.exit(1);
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            System.exit(1);
        }

        JsonNode root = mapper.readTree(file);
        if (!root.isObject()) {
            System.err.println("Root must be JSON object");
            System.exit(1);
        }

        System.out.println("// Generated Pact Lambda DSL from JSON");
        System.out.println("LambdaDsl.newJsonBody(o -> {");
        generateCode(root, "o", 1);
        System.out.println("}).build();");
    }

    private static void generateCode(JsonNode node, String builderName, int indent) {
        String tab = "    ".repeat(indent);
        
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                System.out.printf("%s%s.stringType(\"%s\", %s);\n",
                    tab, builderName, escapeJson(key), escapeJson(value.asText()));
            } else if (value.isNumber()) {
                if (value.isIntegralNumber()) {
                    System.out.printf("%s%s.numberType(\"%s\", %dL);\n",
                        tab, builderName, escapeJson(key), value.asLong());
                } else {
                    System.out.printf("%s%s.numberType(\"%s\", %f);\n",
                        tab, builderName, escapeJson(key), value.asDouble());
                }
            } else if (value.isBoolean()) {
                System.out.printf("%s%s.booleanType(\"%s\", %b);\n",
                    tab, builderName, escapeJson(key), value.asBoolean());
            } else if (value.isNull()) {
                System.out.printf("%s%s.nullValue(\"%s\");\n",
                    tab, builderName, escapeJson(key));
            } else if (value.isObject()) {
                System.out.printf("%s%s.object(\"%s\", o1 -> {\n",
                    tab, builderName, escapeJson(key));
                generateCode(value, "o1", indent + 1);
                System.out.printf("%s});\n", tab);
            } else if (value.isArray()) {
                handleArray(builderName, key, value, tab, indent);
            }
        }
    }

    private static void handleArray(String builderName, String key, JsonNode array, 
                                 String tab, int indent) {
        if (array.size() == 0) {
            System.out.printf("%s%s.eachLike(\"%s\", PactDslJsonRootValue.stringType(\"\"));\n",
                tab, builderName, escapeJson(key));
            return;
        }

        JsonNode first = array.get(0);
        if (first.isObject()) {
            System.out.printf("%s%s.eachLike(\"%s\", o1 -> {\n",
                tab, builderName, escapeJson(key));
            generateCode(first, "o1", indent + 1);
            System.out.printf("%s});\n", tab);
        } else {
            // Primitive array
            if (first.isTextual()) {
                System.out.printf("%s%s.eachLike(\"%s\", PactDslJsonRootValue.stringType(%s));\n",
                    tab, builderName, escapeJson(key), escapeJson(first.asText()));
            } else if (first.isNumber()) {
                System.out.printf("%s%s.eachLike(\"%s\", PactDslJsonRootValue.numberType(%dL));\n",
                    tab, builderName, escapeJson(key), first.asLong());
            } else if (first.isBoolean()) {
                System.out.printf("%s%s.eachLike(\"%s\", PactDslJsonRootValue.booleanType(%b));\n",
                    tab, builderName, escapeJson(key), first.asBoolean());
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}





java -cp ".:jackson-databind-2.15.2.jar" com.example.JsonToLambdaDslCode test.json


# Linux/Mac
javac -cp ".:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar:jackson-databind-2.15.2.jar" com/example/JsonToLambdaDslCode.java
java -cp ".:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar:jackson-databind-2.15.2.jar" com.example.JsonToLambdaDslCode test.json

# Windows
javac -cp ".;jackson-core-2.15.2.jar;jackson-annotations-2.15.2.jar;jackson-databind-2.15.2.jar" com\example\JsonToLambdaDslCode.java
java -cp ".;jackson-core-2.15.2.jar;jackson-annotations-2.15.2.jar;jackson-databind-2.15.2.jar" com.example.JsonToLambdaDslCode test.json





