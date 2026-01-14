import java.io.*;
import java.util.*;

public class jsonToDSL {

    private static int dslCounter = 1;
    private static final List<String> generatedDsls = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToPactDsl input.json");
            return;
        }

        String json = readFile(args[0]).trim();
        String rootDsl = generateObjectDsl("body", json);

        // Print DSL fragments
        for (String dsl : generatedDsls) {
            System.out.println(dsl);
            System.out.println();
        }

        System.out.println("// Root DSL");
        System.out.println("PactDslJsonBody body = " + rootDsl + ";");
    }

    // ================= OBJECT =================

    static String generateObjectDsl(String name, String json) {
        String varName = name + "Dsl" + (dslCounter++);
        StringBuilder sb = new StringBuilder();

        sb.append("PactDslJsonBody ").append(varName)
                .append(" = new PactDslJsonBody()\n");

        json = trim(json, '{', '}');
        for (String token : splitTopLevel(json)) {
            String[] kv = token.split(":", 2);
            if (kv.length != 2) continue;

            String key = stripQuotes(kv[0].trim());
            String value = kv[1].trim();

            if (value.equals("null")) {
                sb.append("    .nullValue(\"").append(key).append("\")\n");
            }
            else if (value.startsWith("{")) {
                String childDsl = generateObjectDsl(key, value);
                sb.append("    .object(\"").append(key).append("\")\n")
                        .append("        .addObject(").append(childDsl).append(")\n")
                        .append("    .closeObject()\n");
            }
            else if (value.startsWith("[")) {
                sb.append(generateArrayDsl(key, value));
            }
            else {
                sb.append("    ").append(primitiveDsl(key, value)).append("\n");
            }
        }

        sb.append(";");
        generatedDsls.add(sb.toString());
        return varName;
    }

    // ================= ARRAY =================

    static String generateArrayDsl(String key, String json) {
        json = trim(json, '[', ']');
        String[] values = splitTopLevel(json);

        StringBuilder sb = new StringBuilder();
        sb.append("    .minArrayLike(\"").append(key).append("\", 1)\n");

        if (values.length > 0 && values[0].trim().startsWith("{")) {
            String itemDsl = generateObjectDsl(key + "Item", values[0]);
            sb.append("        .addObject(").append(itemDsl).append(")\n");
        } else {
            for (String v : values) {
                sb.append("        ").append(primitiveDsl(null, v.trim())).append("\n");
            }
        }

        sb.append("    .closeArray()\n");
        return sb.toString();
    }

    // ================= PRIMITIVES =================

    static String primitiveDsl(String key, String value) {
        String field = key == null ? "" : "\"" + key + "\", ";

        // String
        if (value.startsWith("\"")) {
            String val = stripQuotes(value);

            if (isEnum(val)) {
                return ".stringValue(" + field + "\"" + val + "\")";
            }
            if (isEmail(val)) {
                return ".stringMatcher(" + field +
                        "\"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}\", \"" + val + "\")";
            }
            return ".stringType(" + field + "\"" + val + "\")";
        }

        // Boolean
        if (value.equals("true") || value.equals("false")) {
            return ".booleanType(" + field + value + ")";
        }

        // Number
        return ".numberType(" + field + value + ")";
    }

    // ================= HELPERS =================

    static boolean isEnum(String s) {
        return s.matches("^[A-Z0-9_]+$");
    }

    static boolean isEmail(String s) {
        return s.contains("@");
    }

    static String[] splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder buf = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (c == '{' || c == '[') depth++;
            if (c == '}' || c == ']') depth--;
            if (c == ',' && depth == 0) {
                parts.add(buf.toString());
                buf.setLength(0);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) parts.add(buf.toString());
        return parts.toArray(new String[0]);
    }

    static String trim(String s, char open, char close) {
        s = s.trim();
        if (s.charAt(0) == open) s = s.substring(1);
        if (s.charAt(s.length() - 1) == close) s = s.substring(0, s.length() - 1);
        return s;
    }

    static String stripQuotes(String s) {
        return s.replaceAll("^\"|\"$", "");
    }

    static String readFile(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();
        return sb.toString();
    }

}
