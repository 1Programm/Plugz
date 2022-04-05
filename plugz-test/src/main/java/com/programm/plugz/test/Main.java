package com.programm.plugz.test;

import com.programm.plugz.api.Service;
import com.programm.plugz.magic.MagicEnvironment;

@Service
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

//    public static void main(String[] args) throws Exception {
//        MagicEnvironment.Start(args);
//
//        ConfLogger log = new ConfLogger("[%5<($LVL)] [%30>($LOG?{$CLS.$MET})]: $MSG", ILogger.LEVEL_TRACE);
//
//        PlugzUrlClassScanner scanner = new PlugzUrlClassScanner();
//        scanner.setLogger(log);
//        scanner.addSearchAnnotation(Service.class);
//        scanner.addSearchClass(ISubsystem.class);
//
//        List<URL> searchUrls = new ArrayList<>();
//        ClassLoader cl = Thread.currentThread().getContextClassLoader();
//        Enumeration<URL> urls = cl.getResources("");
//
//        while(urls.hasMoreElements()) {
//            URL url = urls.nextElement();
//            log.trace("# Found [{}].", url);
//            searchUrls.add(url);
//        }
//
//        scanner.scan(searchUrls, "com.programm.plugz");
//    }

}
