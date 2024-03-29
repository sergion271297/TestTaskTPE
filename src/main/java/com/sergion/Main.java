package com.sergion;

import org.apache.commons.cli.*;

import java.io.File;
import java.util.Stack;

public class Main {
    public static void main(String[] args) {

        //Argument processing

        Options options = new Options();

        Option rootPathOption = new Option("p", "rootPath", true, "Root path of the directory");
        rootPathOption.setRequired(true);
        options.addOption(rootPathOption);

        Option depthOption = new Option("d", "depth", true, "Depth of search (non-negative integer)");
        depthOption.setRequired(true);
        options.addOption(depthOption);

        Option maskOption = new Option("m", "mask", true, "String mask to search");
        maskOption.setRequired(true);
        options.addOption(maskOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("FileSearch", options);
            System.exit(1);
            return;
        }

        String rootPath = cmd.getOptionValue("rootPath");
        int depth = Integer.parseInt(cmd.getOptionValue("depth"));
        String mask = cmd.getOptionValue("mask");

        //File search

        Stack<File> stack = new Stack<>();
        stack.push(new File(rootPath));

        while (!stack.isEmpty()) {
            File currentDir = stack.pop();
            if (currentDir.isDirectory()) {
                if (getDepth(rootPath, currentDir) <= depth) {
                    File[] files = currentDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            stack.push(file);
                        }
                    }
                }
            } else {
                if (currentDir.getName().contains(mask)) {
                    System.out.println(currentDir.getAbsolutePath());
                }
            }
        }

    }

    private static int getDepth(String rootPath, File file) {
        String[] rootPathComponents = rootPath.split(File.separator);
        String[] filePathComponents = file.getAbsolutePath().split(File.separator);
        return filePathComponents.length - rootPathComponents.length;
    }

}