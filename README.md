import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class JsonToPactStandalone {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java JsonToPactStandalone <json-file>");
            System.exit(1);
        }

        File jsonFile = new File(args[0]);
        JsonNode root = mapper.readTree(jsonFile);

        DslPart pactBody = LambdaDsl.newJsonBody(o -> populate(o, root)).build();

        // Print the generated Pact-compatible JSON
        System.out.println("=== Pact DSL JSON ===");
        System.out.println(pactBody.toString());
    }

    private static void populate(LambdaDsl.ObjectBuilder o, JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                o.stringType(key, value.asText());
            } else if (value.isInt() || value.isLong()) {
                o.numberType(key, value.asLong());
            } else if (value.isFloatingPointNumber()) {
                o.numberType(key, value.asDouble());
            } else if (value.isBoolean()) {
                o.booleanType(key, value.asBoolean());
            } else if (value.isObject()) {
                o.object(key, obj -> populate(obj, value));
            } else if (value.isArray()) {
                o.array(key, arr -> {
                    if (value.size() > 0) {
                        JsonNode first = value.get(0);
                        if (first.isTextual()) {
                            arr.stringType(first.asText());
                        } else if (first.isNumber()) {
                            arr.numberType(first.numberValue());
                        } else if (first.isObject()) {
                            arr.object(obj -> populate(obj, first));
                        }
                    }
                });
            }
        }
    }
}
