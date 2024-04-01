package com.sergion;

import java.io.*;
import java.net.*;
import java.util.Stack;
import java.util.concurrent.*;

public class Main {
    private static final int MAX_CLIENTS = 6;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: FileSearchTelnetServer <serverPort> <rootPath>");
            System.exit(1);
        }

        int serverPort = Integer.parseInt(args[0]);
        String rootPath = args[1];

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Telnet server is running on port " + serverPort);

            ExecutorService executor = Executors.newFixedThreadPool(MAX_CLIENTS);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress() + " with name: user" + clientSocket.getInetAddress().hashCode());

                ClientHandler clientHandler = new ClientHandler(clientSocket, rootPath);
                executor.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String rootPath;
        private final Object lock = new Object();

        ClientHandler(Socket clientSocket, String rootPath) {
            this.clientSocket = clientSocket;
            this.rootPath = rootPath;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    if (inputLine.equalsIgnoreCase("quit")) {
                        break;
                    }

                    String[] tokens = inputLine.split(" ");
                    if (tokens.length != 2) {
                        writer.println("Invalid command. Please specify depth and mask separated by space.");
                        continue;
                    }

                    int depth = Integer.parseInt(tokens[0]);
                    String mask = tokens[1];

                    String searchResult = performFileSearch(depth, mask);
                    writer.println(searchResult);

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String performFileSearch(int depth, String mask) throws InterruptedException {
            synchronized (lock) {
                StringBuilder result = new StringBuilder();
                Stack<File> stack = new Stack<>();
                stack.push(new File(rootPath));

                result.append("Performing search in directory: ").append(rootPath).append("\n");
                result.append("Depth: ").append(depth).append("\n");
                result.append("Mask: ").append(mask).append("\n");

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
                            result.append(currentDir.getAbsolutePath()).append("\n");
                        }
                    }
                }
                return result.toString();
            }
        }
    }

    private static int getDepth(String rootPath, File file) {
        String[] rootPathComponents = rootPath.split(File.separator);
        String[] filePathComponents = file.getAbsolutePath().split(File.separator);
        return filePathComponents.length - rootPathComponents.length;
    }

}