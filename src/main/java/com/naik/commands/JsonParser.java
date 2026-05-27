package com.naik.commands;

public class JsonParser {
    private JsonParser() {
    }

    public static String getJsonStringValue(String json, String key) {
        if (json == null || key == null || json.isEmpty()) {
            return null;
        }

        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) {
            return null;
        }

        // Find the colon after key
        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx == -1) {
            return null;
        }

        // Find the opening quote of the value
        int startQuote = json.indexOf("\"", colonIdx);
        if (startQuote == -1) {
            return null;
        }

        // Find the closing quote, skipping escaped quotes
        int endQuote = startQuote + 1;
        while (endQuote < json.length()) {
            char c = json.charAt(endQuote);
            if (c == '\\') {
                endQuote += 2; // skip escaped character
            } else if (c == '"') {
                break;
            } else {
                endQuote++;
            }
        }

        if (endQuote >= json.length()) {
            return null;
        }

        String val = json.substring(startQuote + 1, endQuote);
        return unescapeJson(val);
    }

    private static String unescapeJson(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 5 < input.length()) {
                            String hex = input.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
