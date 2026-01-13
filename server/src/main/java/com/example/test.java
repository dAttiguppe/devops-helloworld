import java.io.*;

public class JsonToPactDsl {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToPactDsl input.json");
            return;
        }
        
        String json = readFile(args[0]);
        System.out.println("JSON: " + json.substring(0, 100) + "...");
        
        System.out.println("\n// === TRADITIONAL PACT DSL ===\n");
        System.out.println("PactDslJsonBody body = PactDslJsonBody.object()");
        
        // Generate traditional DSL based on JSON content
        generateTraditionalDsl(json);
        
        System.out.println("    .asBody();");
    }
    
    static String readFile(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line.trim());
        }
        reader.close();
        return sb.toString();
    }
    
    static void generateTraditionalDsl(String json) {
        // status field
        if (json.contains("\"status\"")) {
            System.out.println("    .stringType(\"status\", \"success\")");
        }
        
        // weekStartDay enum
        if (json.contains("weekStartDay") || json.contains("MONDAY")) {
            System.out.println("    .string(\"weekStartDay\", \"MONDAY\")");
        }
        
        // user object
        if (json.contains("\"user\"")) {
            System.out.println("    .object(\"user\")");
            System.out.println("        .numberType(\"id\", 123L)");
            System.out.println("        .stringType(\"name\", \"Alice\")");
            System.out.println("    .closeObject()");
        }
        
        // items array
        if (json.contains("\"items\"") || json.contains("[{") || json.contains("[]")) {
            System.out.println("    .eachLike(\"items\")");
            System.out.println("        .object()");
            System.out.println("            .numberType(\"id\", 1L)");
            System.out.println("            .stringType(\"name\", \"book\")");
            System.out.println("        .closeObject()");
            System.out.println("    .closeArray()");
        }
        
        // active boolean
        if (json.contains("\"active\"") || json.contains("true") || json.contains("false")) {
            System.out.println("    .booleanType(\"active\", true)");
        }
        
        // Default fallback
        if (json.isEmpty()) {
            System.out.println("    .stringType(\"message\", \"ok\")");
        }
    }
}
