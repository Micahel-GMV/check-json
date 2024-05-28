package org.micahel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import java.nio.file.*;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App
{

    private static final String INPUT_FOLDER = "./in";
    private static final String OUTPUT_FOLDER = "./out";

    private static String readFile(File file) {
        String content = null;
        try {
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            System.out.println("Can`t read the file " + file.getAbsolutePath());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return content;
    }

    public static String getJsonFromString(String content) {
        char[] contentChars = content.toCharArray();
        int beginIndex = 0;
        while (contentChars[beginIndex] != '{' && contentChars[beginIndex] != '[') {
            beginIndex++;
            if (beginIndex == contentChars.length) {
                System.out.println("Can`t find opening brackets. Returning input.");
                return content;
            }
        };
        int endIndex = contentChars.length - 1;
        while (contentChars[endIndex] != '}' && contentChars[endIndex] != ']') {
            endIndex--;
            if (endIndex == -1) {
                System.out.println("Can`t find closing brackets. Returning input.");
                return content;
            }
        };
        return content.substring(beginIndex, endIndex+1);
    }

    private static void copyFile(File file, File outputDir) throws IOException {
        Path destination = outputDir.toPath().resolve(file.getName()).normalize();
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static JsonNode assertJson(String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            System.out.println("JSON parsing error:" + e.getMessage());
            return null;
        }
    }

    private static List<String> assertContains(String content, String...strings) {
        List<String> notFound = new ArrayList<>();
        for (String str : strings) {
            if (!content.toLowerCase().contains(str)) {
                notFound.add(str);
            }
        }
        return notFound;
    }

    public static boolean assertAtLeastOne(String content, String...strings) {
        return assertContains(content, strings).size() < strings.length;
    }

    public static int linesCount(String content) {
        return content.split("\r\n|\r|\n").length;
    }

    public static int assertLinesCount(String content, int minLinesCount, int maxLinesCount) {
        int length = linesCount(content);
        if (length >= minLinesCount && length <= maxLinesCount) {
            return 0;
        }
        return length > maxLinesCount ? length - maxLinesCount : length - minLinesCount;
    }

    private static boolean askUserToAccept() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Do you want to accept this file? (y/n)");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y");
    }

    public static String getListAsString (List<String> strings) {
        return String.join(",", strings);
    }

    public static void main( String[] args ) throws IOException {
        Path inputPath = Paths.get(INPUT_FOLDER).toAbsolutePath().normalize();
        Path outputPath = Paths.get(OUTPUT_FOLDER).toAbsolutePath().normalize();

        File inputDir = inputPath.toFile(), outputDir = outputPath.toFile();
        List<String> mandatoryStrings = List.of(
                "200",
                "400",
                "/greet",
                "precondition",
                "missing or invalid parameter: name",
                "action",
                "result",
                "localhost:8085",
                "character");

        List<String> oneOfCornercase = List.of(
               "character", "edge", "corner");


        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File[] files = inputDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files != null) {
            int inputFilesCount = files.length;
            System.out.println(inputFilesCount + " files to check.");
            int fileIndex = 1;
            for (File file : files) {
                String content = readFile(file);
                content = getJsonFromString(content);
                System.out.println("Input file: " + file.getName() + " ********************************************** ");
                System.out.println(content);
                System.out.println(" ********************************************** \n Checking the file "
                        + fileIndex++ + " of " + inputFilesCount + ": " + file.getName());

                boolean fileIsOk = true;

                System.out.print("JSON mapping: ");
                JsonNode jsonNode = assertJson(content);
                if (jsonNode != null) {
                    System.out.println("OK. Size:" + jsonNode.size());
                } else {
                    System.out.println("NOK. *****");
                    fileIsOk = false;
                }

                System.out.print("Line count: " + linesCount(content) + ":");
                int linesNumberProblems = assertLinesCount(content, 100, 500);
                if (linesNumberProblems == 0) {
                    System.out.println("OK.");
                } else {
                    System.out.println("NOK. *****: " + linesNumberProblems);
                    fileIsOk = false;
                }

                System.out.print("Mandatory strings: ");
                List<String> stringsNotFound = assertContains(content, mandatoryStrings.toArray(new String[0]));
                if (stringsNotFound.isEmpty()) {
                    System.out.println("OK");
                } else {
                    System.out.println("NOK. *****: " + getListAsString(stringsNotFound));
                    fileIsOk = false;
                }

                System.out.print("Must be present one of the strings " + getListAsString(oneOfCornercase) + ":");
                if (assertAtLeastOne(content, oneOfCornercase.toArray(oneOfCornercase.toArray(new String[0])))) {
                    System.out.println("OK");
                } else {
                    System.out.println("NOK! None of strings found.");
                    fileIsOk = false;
                }

                if (fileIsOk) {
                    System.out.println("***** File is OK!");
                } else {
                    System.out.println("***** File has errors!");
                }
                if (fileIsOk && askUserToAccept()) {
                    copyFile(file, outputDir);
                    System.out.println("File " + file.getName() + " copied to output directory!");
                } else {
                    System.out.println("File " + file.getName() + " copying declined!");
                }
            }
        } else {
            System.out.println("No txt files in the input folder.");
        }
    }
}