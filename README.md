import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class JsonToPactDslStandalone {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        String json = """
            {
              "status": "ON",
              "items": [{"id": 1, "name": "A"}, {"id": 2, "name": "B"}],
              "code": 200
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

    // [All three methods above go here]
}
