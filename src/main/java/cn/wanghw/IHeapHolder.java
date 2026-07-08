package cn.wanghw;


import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public interface IHeapHolder {
    Object findClass(String var1);

    Iterator getClasses();

    boolean isInstanceOf(Object javaClass, String className);

    boolean isArray(Object javaClass);

    Object[] getSubClasses(Object javaClass);

    List getInstances(Object javaClass);

    List getFields(Object javaClass);

    String getClassName(Object javaClass);

    Object getSuperClass(Object javaClass);

    String getFieldName(Object field);

    Object getFieldClass(Object field);

    Object findThing(Long objectId);

    Object getValueOfField(Object instance, String fieldName);

    HashMap<String, String> getFieldsByNameList(Object instance, HashMap<String, String> fieldList);

    HashMap<String, String> arrayDump(Object instance);

    Object[] getArrayItems(Object instance);

    String getFieldStringValue(Object instance, String fieldName);

    Object getFieldValue(Object instance, String fieldName);

    boolean isMap(Object instance);

    Object getMap(Object instance);

    String toString(Object instance);

    byte[] toByteArray(Object _instance);

    // Enhanced methods for v2.0
    List<String> searchStrings(Pattern pattern);

    List<String> searchAllTexts(Pattern pattern);

    /**
     * Scan raw memory bytes in the heap dump file (similar to 'strings' command).
     * This can find strings in dead objects, memory fragments, and native memory
     * that are not reachable through normal Java heap traversal.
     * 
     * @param pattern regex pattern to match
     * @return list of unique matching strings found in raw memory
     */
    List<String> searchRawMemory(Pattern pattern);

    /**
     * Unified scan: Java object strings + raw memory, deduplicated.
     * Replaces the need for callers to choose between searchStrings and searchRawMemory.
     */
    List<String> searchAll(Pattern pattern);

    List<Object> findClassesByPattern(String pattern);

    HashMap<String, String> getAllFieldValues(Object instance);

    List<Object> traverseLinkedList(Object instance);
}
