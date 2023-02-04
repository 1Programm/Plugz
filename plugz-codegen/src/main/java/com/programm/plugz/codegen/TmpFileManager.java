package com.programm.plugz.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TmpFileManager {

    private static final List<File> tempFiles = new ArrayList<>();
    private static boolean shutdownhook = true;

    private static final Map<String, File> cachedNameToGeneratedFileMap = new HashMap<>();

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
                for(File toDelete : tempFiles) {
                    System.out.println("File to Delete: " + toDelete);
                    if(!file.exists()) continue;
                    if(!recursiveDelete(toDelete)) {
                        System.err.println("Failed to delete file: [" + toDelete.getAbsolutePath() + "]!");
                    }
                }
            }));
        }
    }

    public static File createTmpDirectory(String path, boolean caching) throws IOException {
        File dir = caching ? cachedNameToGeneratedFileMap.get(path) : null;

        if(dir == null) {
            dir = Files.createTempDirectory(path).toFile();
            if(caching) cachedNameToGeneratedFileMap.put(path, dir);
        }

        registerFileToBeDeleted(dir);

        return dir;
    }

    public static File createTmpFile(String prefix, String suffix, File parentDirectory, boolean caching) throws IOException {
        String path = "";
        if(parentDirectory != null) {
            path = parentDirectory.getAbsolutePath();
            if (!path.endsWith("/")) path += "/";
        }
        path += prefix + suffix;

        File file = caching ? cachedNameToGeneratedFileMap.get(path) : null;
        if(file == null) {
            file = File.createTempFile(prefix, suffix, parentDirectory);
            if(caching) cachedNameToGeneratedFileMap.put(path, file);
        }

        registerFileToBeDeleted(file);

        return file;
    }

}
