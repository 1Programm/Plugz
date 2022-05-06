package com.programm.plugz.cls.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClassAnalyzerTest {

    private static Class<?> unwrapParameterizedType(AnalyzedPropertyClass analyzedClass, String name){
        AnalyzedParameterizedType analyzedParameterizedType = analyzedClass.getParameterizedType(name);
        return analyzedParameterizedType == null ? null : analyzedParameterizedType.getType();
    }

    private static Class<?> unwrapParameterizedType(PropertyEntry propertyEntry, String name){
        AnalyzedParameterizedType analyzedParameterizedType = propertyEntry.getParameterizedType(name);
        return analyzedParameterizedType == null ? null : analyzedParameterizedType.getType();
    }

    private static Class<?> unwrapParameterizedType(AnalyzedParameterizedType analyzedParameterizedType, String name){
        AnalyzedParameterizedType subType = analyzedParameterizedType.getParameterizedType(name);
        return subType == null ? null : subType.getType();
    }

    @Test
    @DisplayName("Type Analyze [Simple]")
    void analyzeParameterizedTypeSimple() {
        ClassAnalyzer analyzer = new ClassAnalyzer(false, true);
        analyzer.doDeepAnalyze();

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType intType = analyzer.analyzeParameterizedCls(Integer.class);
            assertEquals(Integer.class, intType.getType());
            assertEquals(0, intType.getParameterizedTypeMap().size());
            assertNotNull(intType.getParent());
            assertEquals(Number.class, intType.getParent().getType());
            assertEquals(0, intType.getParent().getParameterizedTypeMap().size());
            assertNull(intType.getParent().getParent());

            //Caching
            AnalyzedParameterizedType intType2 = analyzer.analyzeParameterizedCls(Integer.class);
            assertSame(intType, intType2);

            AnalyzedParameterizedType strType = analyzer.analyzeParameterizedCls(String.class);
            assertEquals(String.class, strType.getType());
            assertEquals(0, strType.getParameterizedTypeMap().size());
            assertNull(strType.getParent());
        });
    }

    private static class SimplePrivateClass {}

    @Test
    @DisplayName("Analyze Classes [Simple]")
    void analyzePropertySimple() {
        ClassAnalyzer analyzer = new ClassAnalyzer(false, true);

        class SimpleClass {}

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleClass.class);

            assertEquals(SimpleClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(0, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());
        });


        final class SimpleFinalClass {}

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleFinalClass.class);

            assertEquals(SimpleFinalClass.class, analyzedClass.getType());
            assertEquals(Modifier.FINAL, analyzedClass.getClassModifiers());
            assertEquals(0, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());
        });


        //Should throw an exception as private classes cannot be analyzed!
        assertThrows(ClassAnalyzeException.class, () -> analyzer.analyzeProperty(SimplePrivateClass.class));


        class SimplePropertyClass {
            public int number;
            public double anotherNumber;
            public final String SOME_NAME_IN_UPPER_CASE = "Simple Test Name";
        }

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimplePropertyClass.class);

            assertEquals(SimplePropertyClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(3, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());

            PropertyEntry numberEntry = analyzedClass.getFieldEntryMap().get("number");
            assertNotNull(numberEntry);
            assertEquals(Integer.TYPE, numberEntry.getType());
            assertNotNull(numberEntry.getGetter());
            assertNotNull(numberEntry.getSetter());

            PropertyEntry anotherNumberEntry = analyzedClass.getFieldEntryMap().get("another_number");
            assertNotNull(anotherNumberEntry);
            assertEquals(Double.TYPE, anotherNumberEntry.getType());
            assertNotNull(anotherNumberEntry.getGetter());
            assertNotNull(anotherNumberEntry.getSetter());

            PropertyEntry upperNameEntry = analyzedClass.getFieldEntryMap().get("some_name_in_upper_case");
            assertNotNull(upperNameEntry);
            assertEquals(String.class, upperNameEntry.getType());
            assertNotNull(upperNameEntry.getGetter());
            assertNull(upperNameEntry.getSetter());
        });


        class SimpleGetterSetterClass {
            private String name;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void setAge(int age){
                //Test
            }
        }

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleGetterSetterClass.class);

            assertEquals(SimpleGetterSetterClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(2, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());

            PropertyEntry nameEntry = analyzedClass.getFieldEntryMap().get("name");
            assertNotNull(nameEntry);
            assertEquals(String.class, nameEntry.getType());
            assertNotNull(nameEntry.getGetter());
            assertNotNull(nameEntry.getSetter());

            PropertyEntry ageEntry = analyzedClass.getFieldEntryMap().get("age");
            assertNotNull(ageEntry);
            assertEquals(Integer.TYPE, ageEntry.getType());
            assertNull(ageEntry.getGetter());
            assertNotNull(ageEntry.getSetter());
        });
    }

    @Test
    @DisplayName("Analyze Classes [with caching]")
    void analyzePropertyWithCaching(){
        ClassAnalyzer analyzer = new ClassAnalyzer(true, true);

        class SimpleClass {}

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass1 = analyzer.analyzeProperty(SimpleClass.class);
            assertEquals(SimpleClass.class, analyzedClass1.getType());

            AnalyzedPropertyClass analyzedClass2 = analyzer.analyzeProperty(SimpleClass.class);
            assertEquals(SimpleClass.class, analyzedClass2.getType());

            assertEquals(analyzedClass1, analyzedClass2);
        });


        class GenericClass <T> {
            public T data;
        }

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType intType = analyzer.analyzeParameterizedCls(Integer.class);
            AnalyzedParameterizedType strType = analyzer.analyzeParameterizedCls(String.class);

            //Class 1 should be analyzed for the GenericClass with an Integer as data.
            AnalyzedPropertyClass analyzedClass1 = analyzer.analyzeProperty(GenericClass.class, Map.of("T", intType));
            assertEquals(GenericClass.class, analyzedClass1.getType());
            assertEquals(1, analyzedClass1.getParameterizedTypeMap().size());
            assertEquals(Integer.class, unwrapParameterizedType(analyzedClass1, "T"));

            PropertyEntry dataEntry1 = analyzedClass1.getFieldEntryMap().get("data");
            assertNotNull(dataEntry1);
            assertEquals(Integer.class, dataEntry1.getType());

            //Class 2 should be analyzed for the GenericClass with an String as data.
            AnalyzedPropertyClass analyzedClass2 = analyzer.analyzeProperty(GenericClass.class, Map.of("T", strType));
            assertEquals(GenericClass.class, analyzedClass2.getType());
            assertEquals(1, analyzedClass2.getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(analyzedClass2, "T"));

            PropertyEntry dataEntry2 = analyzedClass2.getFieldEntryMap().get("data");
            assertNotNull(dataEntry2);
            assertEquals(String.class, dataEntry2.getType());

            //Should not equal as the generic info is different!
            assertNotEquals(analyzedClass1, analyzedClass2);
        });
    }

    @Test
    @DisplayName("Analyze Classes [with ignoring]")
    void analyzePropertyWithIgnoring() {
        ClassAnalyzer analyzer = new ClassAnalyzer(false, true);
        analyzer.ignorePropertyField(field -> field.getName().startsWith("ignore"));
        analyzer.ignorePropertyMethod(method -> method.getReturnType() == String.class);

        class SimpleClass {
            int a;
            int ignoreB;
            int ignoreC;
        }

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleClass.class);

            assertEquals(SimpleClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(1, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());
        });

        class MethodClass {
            int age;
            String name;

            public int getAge(){
                return age;
            }

            public String getName(){
                return name;
            }
        }

        assertDoesNotThrow(() -> {
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(MethodClass.class);

            assertEquals(MethodClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(2, analyzedClass.getFieldEntryMap().size());
            assertEquals(0, analyzedClass.getParameterizedTypeMap().size());

            PropertyEntry ageEntry = analyzedClass.getFieldEntryMap().get("age");
            assertNotNull(ageEntry);
            assertEquals(Integer.TYPE, ageEntry.getType());
            assertEquals("com.programm.plugz.cls.analyzer.ClassAnalyzer$MethodPropertyGetter", ageEntry.getter.getClass().getName());
            assertEquals("com.programm.plugz.cls.analyzer.ClassAnalyzer$FieldPropertySetter", ageEntry.setter.getClass().getName());

            PropertyEntry nameEntry = analyzedClass.getFieldEntryMap().get("name");
            assertNotNull(nameEntry);
            assertEquals(String.class, nameEntry.getType());
            assertEquals("com.programm.plugz.cls.analyzer.ClassAnalyzer$FieldPropertyGetter", nameEntry.getter.getClass().getName());
            assertEquals("com.programm.plugz.cls.analyzer.ClassAnalyzer$FieldPropertySetter", nameEntry.setter.getClass().getName());
        });
    }

    @Test
    @DisplayName("Analyze Classes [Advanced Generics]")
    void analyzePropertyAdvanced() {
        ClassAnalyzer analyzer = new ClassAnalyzer(false, true);
        analyzer.doDeepAnalyze();


        class SimpleGenericClass <T> {}

        assertDoesNotThrow(() -> {
            //Not providing the analyzer with any info about the generics will result in them mapping to Object.class
            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleGenericClass.class);

            assertEquals(SimpleGenericClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(0, analyzedClass.getFieldEntryMap().size());
            assertEquals(1, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(Object.class, unwrapParameterizedType(analyzedClass, "T"));
        });

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType booleanType = analyzer.analyzeParameterizedCls(Boolean.class);

            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleGenericClass.class, Map.of("T", booleanType));

            assertEquals(SimpleGenericClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(0, analyzedClass.getFieldEntryMap().size());
            assertEquals(1, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(Boolean.class, unwrapParameterizedType(analyzedClass, "T"));
        });


        class SimpleGenericClassWithData <Data> {
            Data data;
        }

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType strType = analyzer.analyzeParameterizedCls(String.class);

            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimpleGenericClassWithData.class, Map.of("Data", strType));

            assertEquals(SimpleGenericClassWithData.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(1, analyzedClass.getFieldEntryMap().size());
            assertEquals(1, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(analyzedClass, "Data"));

            PropertyEntry dataEntry = analyzedClass.getFieldEntryMap().get("data");
            assertNotNull(dataEntry);
            assertEquals(String.class, dataEntry.getType());
        });


        class MultiGenericClassWithData <A, B, C> {
            A aValue;
            B b;
            C theCValue;
        }

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType strType = analyzer.analyzeParameterizedCls(String.class);
            AnalyzedParameterizedType charType = analyzer.analyzeParameterizedCls(Character.class);
            AnalyzedParameterizedType floatType = analyzer.analyzeParameterizedCls(Float.class);

            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(MultiGenericClassWithData.class, Map.of("A", strType, "B", charType, "C", floatType));

            assertEquals(MultiGenericClassWithData.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(3, analyzedClass.getFieldEntryMap().size());
            assertEquals(3, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(analyzedClass, "A"));
            assertEquals(Character.class, unwrapParameterizedType(analyzedClass, "B"));
            assertEquals(Float.class, unwrapParameterizedType(analyzedClass, "C"));

            PropertyEntry aEntry = analyzedClass.getFieldEntryMap().get("a_value");
            assertNotNull(aEntry);
            assertEquals(String.class, aEntry.getType());

            PropertyEntry bEntry = analyzedClass.getFieldEntryMap().get("b");
            assertNotNull(bEntry);
            assertEquals(Character.class, bEntry.getType());

            PropertyEntry cEntry = analyzedClass.getFieldEntryMap().get("the_c_value");
            assertNotNull(cEntry);
            assertEquals(Float.class, cEntry.getType());
        });


        class SimplePassedGenericsClass <T> {
            List<T> theList;
        }

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType longType = analyzer.analyzeParameterizedCls(Long.class);

            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(SimplePassedGenericsClass.class, Map.of("T", longType));

            assertEquals(SimplePassedGenericsClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(1, analyzedClass.getFieldEntryMap().size());
            assertEquals(1, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(Long.class, unwrapParameterizedType(analyzedClass, "T"));

            PropertyEntry listEntry = analyzedClass.getFieldEntryMap().get("the_list");
            assertNotNull(listEntry);
            assertEquals(List.class, listEntry.getType());
            assertEquals(1, listEntry.getParameterizedTypeMap().size());
            assertEquals(Long.class, unwrapParameterizedType(listEntry, "E"));
        });


        class DeepPassedGenericsClass <T> {
            Map<String, List<T>> testMap;
        }

        assertDoesNotThrow(() -> {
            AnalyzedParameterizedType strType = analyzer.analyzeParameterizedCls(String.class);

            AnalyzedPropertyClass analyzedClass = analyzer.analyzeProperty(DeepPassedGenericsClass.class, Map.of("T", strType));

            assertEquals(DeepPassedGenericsClass.class, analyzedClass.getType());
            assertEquals(0, analyzedClass.getClassModifiers());
            assertEquals(1, analyzedClass.getFieldEntryMap().size());
            assertEquals(1, analyzedClass.getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(analyzedClass, "T"));

            PropertyEntry mapEntry = analyzedClass.getFieldEntryMap().get("test_map");
            assertNotNull(mapEntry);
            assertEquals(Map.class, mapEntry.getType());
            assertEquals(2, mapEntry.getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(mapEntry, "K"));
            assertEquals(List.class, unwrapParameterizedType(mapEntry, "V"));
            assertEquals(1, mapEntry.getParameterizedType("V").getParameterizedTypeMap().size());
            assertEquals(String.class, unwrapParameterizedType(mapEntry.getParameterizedType("V"), "E"));
        });
    }
}