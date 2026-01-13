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
        System.out.println
