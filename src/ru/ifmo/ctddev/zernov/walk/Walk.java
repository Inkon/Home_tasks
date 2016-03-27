package ru.ifmo.ctddev.zernov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

public class Walk {

    private static void walk(String pathFiles, String pathOut) {
        try (BufferedReader input = Files.newBufferedReader(Paths.get(pathFiles));
             BufferedWriter output = Files.newBufferedWriter(Paths.get(pathOut))) {
            String path = input.readLine();
            while (path != null) {
                directoryWalk(Paths.get(path), output);
                path = input.readLine();
            }
        } catch (IOException e) {
            System.out.println("Wrong path to input files");
        }
    }

    private static void directoryWalk(Path path, BufferedWriter output) {
        try {
            SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    output.write(fileHashCode(file)+" "+file+"\n");
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e)
                        throws IOException {
                    output.write("00000000000000000000000000000000"+" "+file+"\n");
                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(path,visitor);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Output error");
        }
    }

    private static String fileHashCode(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[512];
            MessageDigest hashBuffer = MessageDigest.getInstance("MD5");
            int bytesRead = 0;
            while (bytesRead != -1) {
                bytesRead = input.read(buffer);
                if (bytesRead > 0) {
                    hashBuffer.update(buffer, 0, bytesRead);
                }
            }
            byte[] b = hashBuffer.digest();
            String result = "";
            for (byte bi : b) {
                result += Integer.toString((bi & 0xff) + 0x100, 16).substring(1).toUpperCase();
            }
            return result;
        } catch (Exception e) {
        }
        return "00000000000000000000000000000000";
    }


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Must be paths to input and output files");
        } else {
            walk(args[0], args[1]);
        }
    }
}
