import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonToPactDsl {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static DslPart fromFile(String path) throws IOException {
        JsonNode root = mapper.readTree(new File(path));
        return buildDsl(root);
    }

    private static DslPart buildDsl(JsonNode node) {
        return LambdaDsl.newJsonBody(o -> populate(o, node)).build();
    }

    private static void populate(LambdaDsl.ObjectBuilder o, JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                o.stringType(name, value.asText());
            } else if (value.isInt() || value.isLong()) {
                o.numberType(name, value.asLong());
            } else if (value.isFloatingPointNumber()) {
                o.numberType(name, value.asDouble());
            } else if (value.isBoolean()) {
                o.booleanType(name, value.asBoolean());
            } else if (value.isObject()) {
                o.object(name, obj -> populate(obj, value));
            } else if (value.isArray()) {
                o.array(name, arr -> {
                    if (value.size() > 0) {
                        JsonNode first = value.get(0);
                        if (first.isObject()) {
                            arr.object(obj -> populate(obj, first));
                        } else if (first.isTextual()) {
                            arr.stringType(first.asText());
                        } else if (first.isNumber()) {
                            arr.numberType(first.numberValue());
                        }
                    }
                });
            }
        }
    }
}
