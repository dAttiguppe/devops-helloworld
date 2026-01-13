import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonToPactDslStandalone {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        // Your JSON input
        String json = """
            {
              "status": "ON",
              "items": [
                {"id": 1, "name": "A"},
                {"id": 2, "name": "B"}
              ],
              "code": 200
            }
            """;

        // Convert to Pact DSL
        PactDslJsonBody body = JsonToPactDsl.fromJson(json);
        
        // Print the generated Pact body (JSON with matchers)
        System.out.println("Generated Pact body:");
        System.out.println(body.getBody());
    }

    // [Include the JsonToPactDsl class methods from previous response here]
    public static PactDslJsonBody fromJson(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        if (!root.isObject()) {
            throw new IllegalArgumentException("Root JSON must be an object");
        }
        PactDslJsonBody body = new PactDslJsonBody();
        fillObject(body, root);
        return body;
    }

    // ... rest of the methods (fillObject, handleArray, addPrimitive)
}
