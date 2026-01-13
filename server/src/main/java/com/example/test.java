import java.io.*;
import java.util.*;

public class JsonToPactDsl {

    private static int indent = 1;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JsonToPactDsl input.json");
            return;
        }

        String json = readFile(args[0]).trim();

        System.out.println("PactDslJsonBody body = new PactDslJsonBody()");
        parseObject(json);
        System.out.println(";");
    }

    // ================= PARSER =================

    static void parseObject(String json) {
        json = trim(json, '{', '}');
        for (String token : splitTopLevel(json)) {
            String[] kv = token.split(":", 2);
            if (kv.length != 2) continue;

            String key = stripQuotes(kv[0].trim());
            String value = kv[1].trim();

            if (value.startsWith("{")) {
                print(".object(\"" + key + "\")");
                indent++;
                parseObject(value);
                indent--;
                print(".closeObject()");
            }
            else if (value.startsWith("[")) {
                print(".array(\"" + key + "\")");
                indent++;
                parseArray(value);
                indent--;
                print(".closeArray()");
            }
            else {
                printPrimitive(key, value);
            }
        }
    }

    static void parseArray(String json) {
        json = trim(json, '[', ']');
        for (String value : splitTopLevel(json)) {
            value = value.trim();

            if (value.startsWith("{")) {
                print(".object()");
                indent++;
                parseObject(value);
                indent--;
                print(".closeObject()");
            }
            else {
                printPrimitive(null, value);
            }
        }
    }

    static void printPrimitive(String key, String value) {
        String field = key == null ? "" : "\"" + key + "\", ";

        if (value.startsWith("\"")) {
            print(".stringType(" + field + "\"" + stripQuotes(value) + "\")");
        }
        else if (value.equals("true") || value.equals("false")) {
            print(".booleanType(" + field + value + ")");
        }
        else {
            print(".numberType(" + field + value + ")");
        }
    }

    // ================= HELPERS =================

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

    static void print(String line) {
        System.out.println("    ".repeat(indent) + line);
    }

    static String readFile(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
