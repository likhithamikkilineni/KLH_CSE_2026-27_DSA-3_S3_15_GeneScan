import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeneScanServer {

    private static final int PORT = 8080;


    public static void main(String[] args) throws Exception {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );


        server.createContext(
                "/api/analyze",
                GeneScanServer::handleAnalyze
        );


        server.createContext(
                "/api/health",
                GeneScanServer::handleHealth
        );


        server.setExecutor(null);

        server.start();


        System.out.println();
        System.out.println(
                "=============================================="
        );

        System.out.println(
                "          GENESCAN JAVA API SERVER"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Server running on:"
        );

        System.out.println(
                "http://localhost:" + PORT
        );

        System.out.println();

        System.out.println(
                "POST /api/analyze"
        );

        System.out.println(
                "GET  /api/health"
        );

        System.out.println(
                "=============================================="
        );
    }


    // =========================================================
    // HEALTH CHECK
    // =========================================================

    private static void handleHealth(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );

            return;
        }


        String response =
                "{"
                        +
                        "\"status\":\"online\","
                        +
                        "\"service\":\"GeneScan\","
                        +
                        "\"algorithm\":\"Boyer-Moore\""
                        +
                        "}";


        sendResponse(
                exchange,
                200,
                response
        );
    }


    // =========================================================
    // ANALYZE
    // =========================================================

    private static void handleAnalyze(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "{\"error\":\"Use POST for analysis\"}"
            );

            return;
        }


        try {

            String requestBody =
                    readRequestBody(exchange);


            String dna =
                    extractJsonValue(
                            requestBody,
                            "sequence"
                    );


            String motif =
                    extractJsonValue(
                            requestBody,
                            "motif"
                    );


            // -------------------------------------------------
            // VALIDATION
            // -------------------------------------------------

            if (
                    dna == null
                            ||
                    dna.trim().isEmpty()
            ) {

                sendResponse(
                        exchange,
                        400,
                        "{\"error\":\"DNA sequence is required\"}"
                );

                return;
            }


            if (
                    motif == null
                            ||
                    motif.trim().isEmpty()
            ) {

                sendResponse(
                        exchange,
                        400,
                        "{\"error\":\"DNA motif is required\"}"
                );

                return;
            }


            dna =
                    dna
                            .replaceAll(
                                    "\\s+",
                                    ""
                            )
                            .toUpperCase();


            motif =
                    motif
                            .replaceAll(
                                    "\\s+",
                                    ""
                            )
                            .toUpperCase();


            String validationError =
                    validateDNA(
                            dna
                    );


            if (
                    validationError != null
            ) {

                sendResponse(
                        exchange,
                        400,
                        "{"
                                +
                                "\"error\":\""
                                +
                                escapeJson(
                                        validationError
                                )
                                +
                                "\""
                                +
                                "}"
                );

                return;
            }


            String motifValidationError =
                    validateDNA(
                            motif
                    );


            if (
                    motifValidationError != null
            ) {

                sendResponse(
                        exchange,
                        400,
                        "{"
                                +
                                "\"error\":\""
                                +
                                escapeJson(
                                        motifValidationError
                                )
                                +
                                "\""
                                +
                                "}"
                );

                return;
            }


            // -------------------------------------------------
            // BOYER-MOORE
            // -------------------------------------------------

            BoyerMoore boyerMoore =
                    new BoyerMoore(
                            motif
                    );


            BoyerMoore.SearchResult result =
                    boyerMoore.search(
                            dna
                    );


            // -------------------------------------------------
            // JSON RESPONSE
            // -------------------------------------------------

            String response =
                    buildAnalysisJson(
                            dna,
                            motif,
                            boyerMoore,
                            result
                    );


            sendResponse(
                    exchange,
                    200,
                    response
            );


        } catch (Exception e) {

            String errorResponse =
                    "{"
                            +
                            "\"error\":\""
                            +
                            escapeJson(
                                    e.getMessage()
                            )
                            +
                            "\""
                            +
                            "}";


            sendResponse(
                    exchange,
                    500,
                    errorResponse
            );
        }
    }


    // =========================================================
    // BUILD JSON
    // =========================================================

    private static String buildAnalysisJson(
            String dna,
            String motif,
            BoyerMoore boyerMoore,
            BoyerMoore.SearchResult result
    ) {

        StringBuilder json =
                new StringBuilder();


        json.append("{");


        // -----------------------------------------------------
        // BASIC INFORMATION
        // -----------------------------------------------------

        json.append(
                "\"sequenceLength\":"
        );

        json.append(
                dna.length()
        );

        json.append(",");


        json.append(
                "\"motif\":\""
        );

        json.append(
                escapeJson(motif)
        );

        json.append("\",");


        json.append(
                "\"algorithm\":\"Boyer-Moore\","
        );


        // -----------------------------------------------------
        // MATCHES
        // -----------------------------------------------------

        json.append(
                "\"matchCount\":"
        );

        json.append(
                result.getMatchCount()
        );

        json.append(",");


        json.append(
                "\"matchPositions\":"
        );

        appendIntegerList(
                json,
                result.getMatchPositions()
        );

        json.append(",");


        // -----------------------------------------------------
        // PERFORMANCE
        // -----------------------------------------------------

        json.append(
                "\"comparisons\":"
        );

        json.append(
                result.getComparisons()
        );

        json.append(",");


        json.append(
                "\"shifts\":"
        );

        json.append(
                result.getShifts()
        );

        json.append(",");


        json.append(
                "\"executionTimeNanoseconds\":"
        );

        json.append(
                result.getExecutionTimeNanoseconds()
        );

        json.append(",");


        json.append(
                "\"executionTimeMilliseconds\":"
        );

        json.append(
                String.format(
                        java.util.Locale.US,
                        "%.6f",
                        result.getExecutionTimeMilliseconds()
                )
        );

        json.append(",");


        // -----------------------------------------------------
        // BAD CHARACTER TABLE
        // -----------------------------------------------------

        json.append(
                "\"badCharacterTable\":{"
        );


        appendDNACharacter(
                json,
                boyerMoore,
                'A'
        );

        json.append(",");

        appendDNACharacter(
                json,
                boyerMoore,
                'C'
        );

        json.append(",");

        appendDNACharacter(
                json,
                boyerMoore,
                'G'
        );

        json.append(",");

        appendDNACharacter(
                json,
                boyerMoore,
                'T'
        );


        json.append("},");


        // -----------------------------------------------------
        // GOOD SUFFIX TABLE
        // -----------------------------------------------------

        json.append(
                "\"goodSuffixTable\":"
        );


        int[] goodSuffixTable =
                boyerMoore.getGoodSuffixTable();


        json.append("[");


        for (
                int i = 0;
                i < goodSuffixTable.length;
                i++
        ) {

            if (i > 0) {
                json.append(",");
            }

            json.append(
                    goodSuffixTable[i]
            );
        }


        json.append("],");


        // -----------------------------------------------------
        // EXECUTION TRACE
        // -----------------------------------------------------

        json.append(
                "\"trace\":"
        );


        appendTrace(
                json,
                result.getTraceSteps()
        );


        json.append("}");


        return json.toString();
    }


    // =========================================================
    // BAD CHARACTER ENTRY
    // =========================================================

    private static void appendDNACharacter(
            StringBuilder json,
            BoyerMoore boyerMoore,
            char character
    ) {

        int[] table =
                boyerMoore.getBadCharacterTable();


        json.append(
                "\""
        );

        json.append(
                character
        );

        json.append(
                "\":"
        );

        json.append(
                table[character]
        );
    }


    // =========================================================
    // TRACE JSON
    // =========================================================

    private static void appendTrace(
            StringBuilder json,
            List<BoyerMoore.TraceStep> trace
    ) {

        json.append("[");


        for (
                int i = 0;
                i < trace.size();
                i++
        ) {

            if (i > 0) {
                json.append(",");
            }


            BoyerMoore.TraceStep step =
                    trace.get(i);


            json.append("{");


            json.append(
                    "\"step\":"
            );

            json.append(
                    step.getStepNumber()
            );

            json.append(",");


            json.append(
                    "\"alignment\":"
            );

            json.append(
                    step.getAlignment()
            );

            json.append(",");


            json.append(
                    "\"matchFound\":"
            );

            json.append(
                    step.isMatchFound()
            );

            json.append(",");


            json.append(
                    "\"badCharacter\":"
            );


            if (
                    step.getBadCharacter()
                            == null
            ) {

                json.append(
                        "null"
                );

            } else {

                json.append("\"");

                json.append(
                        step.getBadCharacter()
                );

                json.append("\"");
            }


            json.append(",");


            json.append(
                    "\"badCharacterShift\":"
            );

            json.append(
                    step.getBadCharacterShift()
            );

            json.append(",");


            json.append(
                    "\"goodSuffixShift\":"
            );

            json.append(
                    step.getGoodSuffixShift()
            );

            json.append(",");


            json.append(
                    "\"appliedShift\":"
            );

            json.append(
                    step.getAppliedShift()
            );

            json.append(",");


            // ---------------------------------------------
            // COMPARISONS
            // ---------------------------------------------

            json.append(
                    "\"comparisons\":["
            );


            List<BoyerMoore.ComparisonDetail>
                    comparisons =
                    step.getComparisons();


            for (
                    int j = 0;
                    j < comparisons.size();
                    j++
            ) {

                if (j > 0) {
                    json.append(",");
                }


                BoyerMoore.ComparisonDetail comparison =
                        comparisons.get(j);


                json.append("{");


                json.append(
                        "\"patternIndex\":"
                );

                json.append(
                        comparison.getPatternIndex()
                );

                json.append(",");


                json.append(
                        "\"textIndex\":"
                );

                json.append(
                        comparison.getTextIndex()
                );

                json.append(",");


                json.append(
                        "\"patternCharacter\":\""
                );

                json.append(
                        comparison.getPatternCharacter()
                );

                json.append("\",");


                json.append(
                        "\"textCharacter\":\""
                );

                json.append(
                        comparison.getTextCharacter()
                );

                json.append("\",");


                json.append(
                        "\"matched\":"
                );

                json.append(
                        comparison.isMatched()
                );


                json.append("}");
            }


            json.append("]");


            json.append("}");
        }


        json.append("]");
    }


    // =========================================================
    // READ REQUEST
    // =========================================================

    private static String readRequestBody(
            HttpExchange exchange
    ) throws IOException {

        InputStream input =
                exchange.getRequestBody();


        return new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
        );
    }


    // =========================================================
    // SIMPLE JSON VALUE EXTRACTION
    // =========================================================

    private static String extractJsonValue(
            String json,
            String key
    ) {

        String searchKey =
                "\"" + key + "\"";


        int keyPosition =
                json.indexOf(searchKey);


        if (keyPosition < 0) {
            return null;
        }


        int colonPosition =
                json.indexOf(
                        ':',
                        keyPosition
                );


        if (colonPosition < 0) {
            return null;
        }


        int start =
                json.indexOf(
                        '"',
                        colonPosition + 1
                );


        if (start < 0) {
            return null;
        }


        StringBuilder value =
                new StringBuilder();


        boolean escaped =
                false;


        for (
                int i = start + 1;
                i < json.length();
                i++
        ) {

            char c =
                    json.charAt(i);


            if (escaped) {

                value.append(c);

                escaped = false;

                continue;
            }


            if (c == '\\') {

                escaped = true;

                continue;
            }


            if (c == '"') {

                break;
            }


            value.append(c);
        }


        return value.toString();
    }


    // =========================================================
    // DNA VALIDATION
    // =========================================================

    private static String validateDNA(
            String sequence
    ) {

        for (
                int i = 0;
                i < sequence.length();
                i++
        ) {

            char c =
                    sequence.charAt(i);


            if (
                    c != 'A'
                            &&
                    c != 'C'
                            &&
                    c != 'G'
                            &&
                    c != 'T'
            ) {

                return
                        "Invalid DNA character '"
                                +
                        c
                                +
                        "' at position "
                                +
                        i
                                +
                        ". Allowed characters are A, C, G and T.";
            }
        }


        return null;
    }


    // =========================================================
    // INTEGER LIST JSON
    // =========================================================

    private static void appendIntegerList(
            StringBuilder json,
            List<Integer> values
    ) {

        json.append("[");


        for (
                int i = 0;
                i < values.size();
                i++
        ) {

            if (i > 0) {
                json.append(",");
            }

            json.append(
                    values.get(i)
            );
        }


        json.append("]");
    }


    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private static String escapeJson(
            String value
    ) {

        if (value == null) {
            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }


    // =========================================================
    // SEND RESPONSE
    // =========================================================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response
    ) throws IOException {

        byte[] bytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );


        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Origin",
                        "*"
                );


        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Methods",
                        "GET, POST, OPTIONS"
                );


        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Headers",
                        "Content-Type"
                );


        exchange.sendResponseHeaders(
                statusCode,
                bytes.length
        );


        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }
}