import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class JsonToLambdaDsl {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        JsonNode root = mapper.readTree(file);

        System.out.println("newJsonBody()");
        generateDsl(root, "");
        System.out.println("    .asBody();");
    }

    private static void generateDsl(JsonNode node, String indent) {
        String tab = indent.repeat(4);
        
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isTextual()) {
                System.out.printf("    %s.stringType(\"%s\", \"%s\")\n",
                    tab, key, escape(value.asText()));
            } else if (value.isNumber()) {
                if (value.isIntegralNumber()) {
                    System.out.printf("    %s.numberType(\"%s\", %dL)\n",
                        tab, key, value.asLong());
                } else {
                    System.out.printf("    %s.numberType(\"%s\", %f)\n",
                        tab, key, value.asDouble());
                }
            } else if (value.isBoolean()) {
                System.out.printf("    %s.booleanType(\"%s\", %b)\n",
                    tab, key, value.asBoolean());
            } else if (value.isObject()) {
                System.out.printf("    %s.object(\"%s\")\n", tab, key);
                generateDsl(value, indent + "    ");
                System.out.printf("    %s.closeObject()\n", indent);
            } else if (value.isArray()) {
                handleArray(key, value, indent);
            }
        }
    }

    private static void handleArray(String key, JsonNode array, String indent) {
        String tab = indent.repeat(4);
        if (array.size() == 0) {
            System.out.printf("    %s.eachLike(\"%s\").closeArray()\n", tab, key);
            return;
        }
        
        JsonNode first = array.get(0);
        if (first.isObject()) {
            System.out.printf("    %s.eachLike(\"%s\")\n", tab, key);
            System.out.printf("    %s.object()\n", tab);
            generateDsl(first, indent + "    ");
            System.out.printf("    %s.closeObject()\n", indent);
            System.out.printf("    %s.closeArray()\n", indent);
        } else {
            System.out.printf("    %s.eachLike(\"%s\", PactDslJsonRootValue.stringType(\"%s\")).closeArray()\n",
                tab, key, first.asText());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
