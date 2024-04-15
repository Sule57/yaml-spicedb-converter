package com.example;

import org.apache.commons.cli.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpicedbSchemaConverter {

    public static void main(String[] args) {
        // Setup command line options
        Options options = new Options();
        options.addOption(Option.builder("c").longOpt("context").hasArg().desc("Context for encoding").build());
        options.addOption(Option.builder("e").longOpt("encode").desc("Encode action").build());
        options.addOption(Option.builder("d").longOpt("decode").desc("Decode action").build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            boolean isEncode = cmd.hasOption("encode");
            boolean isDecode = cmd.hasOption("decode");
            String context = cmd.getOptionValue("context");
            String[] remainingArgs = cmd.getArgs();

            if (isEncode && remainingArgs.length > 0) {
                if (context == null) {
                    throw new ParseException("Context is required for encoding");
                }
                encode(remainingArgs[0], remainingArgs[0].replaceAll(".yaml", "_encoded.yaml"), context);
            } else if (isDecode && remainingArgs.length > 0) {
                decode(remainingArgs[0], remainingArgs[0].replaceAll(".yaml", "_decoded.yaml"));
            } else {
                throw new ParseException("Invalid operation. Please specify encode or decode.");
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("converter.jar", options);
            System.exit(1);
        }
    }

    private static void encode(String inputFileName, String outputFileName, String context) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(inputFileName));
            List<String> modifiedLines = lines.stream()
                    .map(line -> modifyLine(line, context))
                    .collect(Collectors.toList());
            applyClosingBraces(modifiedLines);
            Files.write(Paths.get(outputFileName), modifiedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decode(String inputFileName, String outputFileName) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(inputFileName));
            List<String> modifiedLines = new ArrayList<>();
            boolean insideBlock = false;

            for (String line : lines) {
                if (line.trim().endsWith("{")) { // Start of a block
                    insideBlock = true;
                    // Convert the line back to the original format without the brace
                    line = line.replace(" {", ":");
                } else if (line.trim().equals("}")) { // End of a block
                    insideBlock = false;
                    continue; // Skip adding this line to output
                }

                if (insideBlock) {
                    line = reverseModifyLine(line);
                }

                modifiedLines.add(line);
            }

            Files.write(Paths.get(outputFileName), modifiedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String modifyLine(String line, String context) {
        String indentedLine = line.replaceFirst("^  ", "    ");
        if (indentedLine.trim().startsWith("permission ")) {
            return indentedLine.replace(":", " =").replace("user", context + "/user")
                    .replace("group", context + "/group")
                    .replace("folder", context + "/folder");
        } else if (indentedLine.matches("^[a-z]+:")) {
            return "definition " + context + "/" + indentedLine.trim().replace(":", " {");
        } else {
            return indentedLine.replaceAll("user", context + "/user")
                               .replaceAll("group", context + "/group")
                               .replaceAll("folder", context + "/folder");
        }
    }

    private static String reverseModifyLine(String line) {
        String normalLine = line.replaceAll("definition file_system/", "").replaceAll(" = ", ": ")
                                .replaceAll("file_system/", "");
        if (normalLine.trim().endsWith("{")) {
            return normalLine.replace("{", ":");
        }
        return normalLine.replaceFirst("    ", "  ");
    }

    private static void applyClosingBraces(List<String> lines) {
        boolean isDefinitionBlock = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("definition ")) {
                if (isDefinitionBlock) {
                    lines.add(i, "}");
                    i++;
                }
                isDefinitionBlock = true;
            } else if (line.trim().isEmpty() && isDefinitionBlock) {
                lines.add(i, "}");
                isDefinitionBlock = false;
                i++;
            }
        }
        if (isDefinitionBlock) {
            lines.add("}");
        }
    }
}
