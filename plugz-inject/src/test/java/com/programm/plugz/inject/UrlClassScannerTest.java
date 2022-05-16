package com.programm.plugz.inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlClassScannerTest {

    public static class A {}

    public static class B extends A {}

    public static class C extends A {}

    public static class D extends B {}

    @Test
    @DisplayName("Scan-Classes [Simple]")
    public void testSimpleClassScan(){
        URL url = UrlClassScanner.class.getResource("/");
        assertNotNull(url);

        UrlClassScanner scanner = new UrlClassScanner();

        assertDoesNotThrow(() -> {
            List<Class<?>> classesImplementingA = new ArrayList<>();

            ScanCriteria criteria = ScanCriteria.createOnSuccessCollect("Implementing A.class", classesImplementingA)
                    .classImplements(A.class);

            scanner.forUrls(url)
                    .withCriteria(criteria)
                    .scan();

            assertEquals(3, classesImplementingA.size());
            assertTrue(classesImplementingA.contains(B.class));
            assertTrue(classesImplementingA.contains(C.class));
            assertTrue(classesImplementingA.contains(D.class));
        });
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationA {}

    @AnnotationA
    public static class E {}

    @AnnotationA
    public static class F {}

    @Test
    @DisplayName("Scan-Annotations [Simple]")
    public void testSimpleAnnotationScan(){
        UrlClassScanner scanner = new UrlClassScanner();

        URL url = UrlClassScanner.class.getResource("/");
        assertNotNull(url);

        assertDoesNotThrow(() -> {
            List<Class<?>> classesAnnotatedWithA = new ArrayList<>();

            scanner.forUrls(url)
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Annotated with AnnotationA.class", classesAnnotatedWithA)
                            .classAnnotatedWith(AnnotationA.class))
                    .scan();

            assertEquals(2, classesAnnotatedWithA.size());
            assertTrue(classesAnnotatedWithA.contains(E.class));
            assertTrue(classesAnnotatedWithA.contains(F.class));
        });
    }

}