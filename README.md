private static void generateCode(JsonNode node, String indent) {
    String tab = indent.repeat(4);
    
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String key = field.getKey();
        JsonNode value = field.getValue();

        if (value.isTextual()) {
            System.out.printf("%sstringType(\"%s\", \"%s\")\n",
                tab, key, escapeJson(value.asText()));
        } else if (value.isNumber()) {
            System.out.printf("%snumberType(\"%s\", %dL)\n",
                tab, key, value.asLong());
        } else if (value.isBoolean()) {
            System.out.printf("%sbooleanType(\"%s\", %b)\n",
                tab, key, value.asBoolean());
        } else if (value.isNull()) {
            System.out.printf("%snullValue(\"%s\")\n",
                tab, key);
        } else if (value.isObject()) {
            System.out.printf("%sobject(\"%s\")\n", tab, key);
            generateCode(value, indent + "    ");
            System.out.printf("%scloseObject()\n", indent);
        } else if (value.isArray()) {
            handleArray(key, value, indent);
        }
    }
}

private static void handleArray(String key, JsonNode array, String indent) {
    String tab = indent.repeat(4);
    
    if (array.size() == 0) {
        System.out.printf("%seachLike(\"%s\").closeArray()\n", tab, key);
        return;
    }

    JsonNode first = array.get(0);
    if (first.isObject()) {
        System.out.printf("%seachLike(\"%s\")\n", tab, key);
        System.out.println(tab + "object()");
        generateCode(first, indent + "    ");
        System.out.printf("%scloseObject()\n", indent);
        System.out.printf("%scloseArray()\n", indent);
    } else {
        System.out.printf("%seachLike(\"%s\", PactDslJsonRootValue.stringType(\"%s\")).closeArray()\n",
            tab, key, first.asText());
    }
}
