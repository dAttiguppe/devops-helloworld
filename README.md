import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonToPactDslStandalone {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        String json = """
            {
              "status": "ON",
              "items": [
                {"id": 1, "name": "A"}, 
                {"id": 2, "name": "B"}
              ],
              "code": 200,
              "tags": ["java", "pact"],
              "active": true
            }
            """;

        PactDslJsonBody body = fromJson(json);
        System.out.println("Generated Pact body:");
        System.out.println(body.getBody());
    }

    public static PactDslJsonBody fromJson(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        if (!root.isObject()) {
            throw new IllegalArgumentException("Root JSON must be an object");
        }
        PactDslJsonBody body = new PactDslJsonBody();
        fillObject(body, root);
        return body;
    }

    private static void fillObject(PactDslJsonBody body, JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode value = field.getValue();

            if (value.isObject()) {
                PactDslJsonBody child = body.object(name);
                fillObject(child, value);
                child.closeObject();
            } else if (value.isArray()) {
                handleArray(body, name, value);
            } else {
                addPrimitive(body, name, value);
            }
        }
    }

    private static void handleArray(PactDslJsonBody body, String name, JsonNode arrayNode) {
        if (arrayNode.size() == 0) {
            body.eachLike(name, PactDslJsonRootValue.stringType("example")).closeArray();
            return;
        }

        JsonNode first = arrayNode.get(0);

        if (first.isObject()) {
            PactDslJsonBody element = body.eachLike(name);
            fillObject(element, first);
            element.closeObject().closeArray();
        } else if (first.isTextual()) {
            body.eachLike(name, PactDslJsonRootValue.stringType(first.asText())).closeArray();
        } else if (first.isNumber()) {
            body.eachLike(name, PactDslJsonRootValue.numberType(first.numberValue())).closeArray();
        } else if (first.isBoolean()) {
            body.eachLike(name, PactDslJsonRootValue.booleanType(first.booleanValue())).closeArray();
        } else {
            body.eachLike(name, PactDslJsonRootValue.stringType(first.asText())).closeArray();
        }
    }

    private static void addPrimitive(PactDslJsonBody body, String name, JsonNode value) {
        if (value.isTextual()) {
            body.stringType(name, value.asText());
        } else if (value.isNumber()) {
            body.numberType(name, value.numberValue());
        } else if (value.isBoolean()) {
            body.booleanType(name, value.booleanValue());
        } else if (value.isNull()) {
            body.nullValue(name);
        } else {
            body.stringType(name, value.asText());
        }
    }
}


javac -cp "pact-jvm-consumer-*.jar:jackson-databind-*.jar" JsonToPactDslStandalone.java
java -cp ".:pact-jvm-consumer-*.jar:jackson-databind-*.jar" JsonToPactDslStandalone



