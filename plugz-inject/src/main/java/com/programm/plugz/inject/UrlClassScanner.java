package com.programm.plugz.inject;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.ioutils.log.api.NullLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Logger("Plugz Scanner")
public class UrlClassScanner {

    private ILogger log = new NullLogger();
    private boolean autoResetConfig;
    private final Set<URL> searchUrls = new HashSet<>();
    private final Set<Class<?>> searchClasses = new HashSet<>();
    private final List<ScanCriteria> criteriaList = new ArrayList<>();
    private final Map<URL, String> entryPointMap = new HashMap<>();

    /**
     * Starts the actual scan.
     * Will use the specified criteria to find classes in the urls.
     * After the scan is finished the configs will be reset if {@link UrlClassScanner#autoResetConfig()} has been called.
     * @throws ScanException when some IOException or other exception is thrown while scanning.
     */
    public void scan() throws ScanException {
        try {
            if (searchUrls.isEmpty() && searchClasses.isEmpty()) {
                log.warn("# No urls or classes to search through!");
                return;
            }

            boolean init = false;
            for(URL url : searchUrls) {
                String urlFile = url.getFile();

                if (init && log.level() == ILogger.LEVEL_TRACE) log.trace("");
                init = true;

                if (urlFile.endsWith(".jar")) {
                    log.debug("# Scanning url as jar         : [{}]...", url);
                    searchInJar(url);
                }
                else if (url.getProtocol().equals("file")) {
                    log.debug("# Scanning url as class folder: [{}]...", url);
                    File file = new File(urlFile);
                    searchInFolder(url, file);
                }
                else {
                    throw new ScanException("Could not scan url: [" + url + "]: Invalid url type!");
                }
            }

            init = false;
            for(Class<?> cls : searchClasses){
                URL classOrigin = cls.getResource("/");

                if(init && log.level() == ILogger.LEVEL_TRACE) log.trace("");
                init = true;

                loadAndScanClassFromName(classOrigin, cls.getName(), cls);
            }
        }
        finally {
            if(autoResetConfig) clearConfig();
        }
    }




    private void searchInJar(URL url) throws ScanException {
        String pathRegix = entryPointMap.get(url);

        try (ZipInputStream zip = new ZipInputStream(url.openStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    if(pathRegix != null && !name.startsWith(pathRegix)){
                        continue;
                    }

                    if(!name.endsWith(".class")) continue;

                    name = name.substring(0, name.length() - ".class".length());
                    name = name.replaceAll("/", ".");

                    loadAndScanClassFromName(url, name, null);
                }
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }
    }

    private void searchInFolder(URL url, File file) throws ScanException {
        String pathRegix = entryPointMap.get(url);

        String rootFolderPath = file.getAbsolutePath();
        Queue<File> toBeScanned = new ArrayDeque<>();
        toBeScanned.add(file);

        while(!toBeScanned.isEmpty()){
            File curFile = toBeScanned.poll();

            File[] children = curFile.listFiles();
            if(children != null){
                Collections.addAll(toBeScanned, children);
            }
            else {
                String name = curFile.getName();
                if(name.endsWith(".class")){
                    String fullName = getFullNameFromAbsolutePath(curFile.getAbsolutePath(), rootFolderPath);
                    if(pathRegix != null && !fullName.startsWith(pathRegix)) continue;
                    loadAndScanClassFromName(url, fullName, null);
                }
            }
        }
    }

    private String getFullNameFromAbsolutePath(String path, String rootFolder){
        path = path.substring(rootFolder.length(), path.length() - ".class".length());
        if(path.startsWith("/")) path = path.substring(1);
        return path.replaceAll("/", ".");
    }

    private void loadAndScanClassFromName(URL url, String clsName, Class<?> cls){
        List<Boolean> criteriaNameTestResult = new ArrayList<>();

        boolean success = criteriaList.isEmpty();
        for(ScanCriteria criteria : criteriaList){
            if(criteria.testName(url, clsName)){
                criteriaNameTestResult.add(true);
                success = true;
            }
            else {
                criteriaNameTestResult.add(false);
            }
        }

        if(!success) return;

        if(cls == null){
            try {
                cls = Class.forName(clsName);
            }
            catch (ClassNotFoundException e){
                log.logException("# Something went wrong: Class [" + clsName + "] could not be found!", e);
                return;
            }
            catch (NoClassDefFoundError e){
                return;
            }
        }

        log.trace("### Found class: [{}]", cls);

        for(int i=0;i<criteriaList.size();i++){
            ScanCriteria criteria = criteriaList.get(i);
            if(criteria.testClass(url, cls)){
                if(criteriaNameTestResult.get(i)){
                    log.trace("###       {} matched the criteria [" + criteria.name + "]!", cls);
                    criteria.onSuccess(cls);
                }
            }
        }
    }

    public UrlClassScanner clearCriterias(){
        this.criteriaList.clear();
        return this;
    }

    public UrlClassScanner clearConfig(){
        this.searchUrls.clear();
        this.searchClasses.clear();
        this.criteriaList.clear();
        this.entryPointMap.clear();
        return this;
    }

    public UrlClassScanner forClasses(Collection<Class<?>> classes){
        this.searchClasses.addAll(classes);
        return this;
    }

    public UrlClassScanner forClasses(Set<Class<?>> classes){
        return forClasses((Collection<Class<?>>) classes);
    }

    public UrlClassScanner forClasses(Class<?>... classes){
        return forClasses(Set.of(classes));
    }

    public UrlClassScanner forUrls(Collection<URL> urls){
        this.searchUrls.addAll(urls);
        return this;
    }

    public UrlClassScanner forUrls(Set<URL> urls){
        return forUrls((Collection<URL>) urls);
    }

    public UrlClassScanner forUrls(URL... urls){
        return forUrls(Set.of(urls));
    }

    public UrlClassScanner entryPoint(URL url, String entryPoint){
        entryPointMap.put(url, entryPoint);
        return this;
    }

    public UrlClassScanner withCriteria(ScanCriteria criteria){
        criteriaList.add(criteria);
        return this;
    }

    public UrlClassScanner setLogger(ILogger log){
        this.log = log;
        return this;
    }

    public UrlClassScanner autoResetConfig(){
        this.autoResetConfig = true;
        return this;
    }

}
