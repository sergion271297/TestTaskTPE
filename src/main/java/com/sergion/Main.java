package com.sergion;

import org.apache.commons.cli.*;

import java.io.File;
import java.util.Stack;
import java.util.concurrent.*;

public class Main {
    private static final int MAX_RESULTS = 100;
    private static BlockingQueue<String> results = new LinkedBlockingQueue<>();
    public static void main(String[] args) throws InterruptedException {

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

        //Threads

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> searchFuture = executor.submit(() -> searchFiles(rootPath, depth, mask));
        Future<?> printFuture = executor.submit(Main::printResults);

        try {
            searchFuture.get();
            printFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdown();

    }

    private static void searchFiles(String rootPath, int depth, String mask) {
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
                    try {
                        results.put(currentDir.getAbsolutePath());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private static void printResults() {
        int count = 0;
        while (count < MAX_RESULTS) {
            try {
                String result = results.take();
                System.out.println(result);
                count++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int getDepth(String rootPath, File file) {
        String[] rootPathComponents = rootPath.split(File.separator);
        String[] filePathComponents = file.getAbsolutePath().split(File.separator);
        return filePathComponents.length - rootPathComponents.length;
    }

}