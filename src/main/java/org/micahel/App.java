package org.micahel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import java.nio.file.*;
import java.util.List;

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

    private static void copyFile(File file, File outputDir) throws IOException {
        Path destination = outputDir.toPath().resolve(file.getName()).normalize();
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean assertJson(String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(content); // Will throw JsonProcessingException if content is not valid JSON
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static boolean assertContains(String content) {
        List<String> stringsToCheck = List.of("200",
                                                "400",
                                                "/greet",
                                                "preconditions",
                                                "missing or invalid parameter: name",
                                                "actions",
                                                "result");

        for (String str : stringsToCheck) {
            if (!content.toLowerCase().contains(str)) {
                System.out.println("String " + str + " not found.");
                return false;
            }
        }
        return true;
    }

    public static boolean assertLinesCount(String content, int minLinesCount, int maxLinesCount) {
        String[] lines = content.split("\r\n|\r|\n");
        if (lines.length >= minLinesCount && lines.length <= maxLinesCount) {
            return true;
        }
        System.out.println("Content lines count " + lines.length + " is out of boundaries from " + minLinesCount
                + " to " + maxLinesCount + ".");
        return false;
    }

    public static void main( String[] args )
    {
        Path inputPath = Paths.get(INPUT_FOLDER).toAbsolutePath().normalize();
        Path outputPath = Paths.get(OUTPUT_FOLDER).toAbsolutePath().normalize();

        File inputDir = inputPath.toFile(), outputDir = outputPath.toFile();

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File[] files = inputDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                String content = readFile(file);
                if (assertLinesCount(content, 30, 150) && assertContains(content) && assertJson(content)) {
                    try {
                        copyFile(file, outputDir);
                        System.out.println(file.getName() + " copied.");
                    } catch (IOException e) {
                        System.out.println("Can`t copy the file " + file.getAbsolutePath() + " to " + outputDir.getAbsolutePath());
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println(file.getName() + " didn`t pass the JSON mapping or didn`t contain mandatory string(s).");
                }
            });
        } else {
            System.out.println("No txt files in the input folder.");
        }
    }
}
