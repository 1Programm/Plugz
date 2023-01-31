package com.programm.plugz.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

class FileManager {

    private static List<File> tempFiles = new ArrayList<>();
    private static boolean shutdownhook = true;

    public static boolean recursiveDelete(File file){
        if(file.isDirectory()){
            boolean allDeleted = true;
            File[] children = file.listFiles();
            if(children != null){
                for(File child : children){
                    if(!recursiveDelete(child)){
                        allDeleted = false;
                    }
                }
            }

            return allDeleted && file.delete();
        }
        else {
            return file.delete();
        }
    }

    public static void registerFileToBeDeleted(File file){
        tempFiles.add(file);

        if(shutdownhook){
            shutdownhook = false;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                System.out.println("On Shutdown");
                for(File toDelete : tempFiles) {
                    if(!file.exists()) continue;
                    if(!recursiveDelete(toDelete)) {
                        System.err.println("Failed to delete file: [" + toDelete.getAbsolutePath() + "]!");
                    }
                }
            }));
        }
    }

    public static File createTmpDirectory(String path) throws IOException {
        File dir = Files.createTempDirectory(path).toFile();
        registerFileToBeDeleted(dir);

        return dir;
    }

//    public static File createTmpFile(String prefix, String suffix) throws IOException {
//        File dir = Files.createTempFile(prefix, suffix).toFile();
//        registerFileToBeDeleted(dir);
//
//        return dir;
//    }

    public static File createTmpFile(String prefix, String suffix, File parentDirectory) throws IOException {
        File dir = File.createTempFile(prefix, suffix, parentDirectory);
        registerFileToBeDeleted(dir);

        return dir;
    }

}
