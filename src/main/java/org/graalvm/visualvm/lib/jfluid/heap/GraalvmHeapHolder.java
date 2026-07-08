package org.graalvm.visualvm.lib.jfluid.heap;

import cn.wanghw.IHeapHolder;
import cn.wanghw.utils._StringJoiner;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.impl.Snapshot;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

public class GraalvmHeapHolder implements IHeapHolder {
    final private AtomicBoolean cancelled = new AtomicBoolean(false);
    private Heap _heap;
    private Snapshot snapshot;
    private File heapFile;  // Store the original heap file for raw memory scanning
    private final java.util.Map<String, JavaClass> classCache = new java.util.concurrent.ConcurrentHashMap<>();

    public GraalvmHeapHolder(File heapfile) throws IOException {
        this.heapFile = heapfile;  // Store file reference
        _heap = HeapFactory.createHeap(heapfile);
        snapshot = new Snapshot(_heap, this);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancelQuery() {
        cancelled.set(true);
    }

    public JavaClass findClass(String var1) {
        return classCache.computeIfAbsent(var1, snapshot::findClass);
    }

    public boolean isArray(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).isArray();
        }
        return false;
    }

    public boolean isInstanceOf(Object javaClass, String className) {
        if (javaClass instanceof JavaClass) {
            JavaClass cls = (JavaClass) javaClass;
            for (; cls != null; cls = cls.getSuperClass())
                if (cls.getName().equals(className)) return true;
        }
        return false;
    }

    public JavaClass[] getSubClasses(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).getSubClasses().toArray(new JavaClass[0]);
        }
        return new JavaClass[0];
    }

    public Iterator getClasses() {
        return snapshot.getClasses();
    }

    public List getInstances(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).getInstances();
        }
        return new ArrayList();
    }

    public List getFields(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).getFields();
        }
        return new ArrayList();
    }

    public String getClassName(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).getName();
        }
        return null;
    }

    public Object getSuperClass(Object javaClass) {
        if (javaClass instanceof JavaClass) {
            return ((JavaClass) javaClass).getSuperClass();
        }
        return null;
    }

    public String getFieldName(Object field) {
        if (field instanceof Field) {
            return ((Field) field).getName();
        }
        return null;
    }

    public Object getFieldClass(Object field) {
        if (field instanceof Field) {
            return ((Field) field).getDeclaringClass();
        }
        return null;
    }

    public Object getValueOfField(Object instance, String fieldName) {
        if (instance instanceof Instance) {
            return ((Instance) instance).getValueOfField(fieldName);
        }
        return null;
    }

    public Object findThing(Long objectId) {
        return snapshot.findThing(objectId);
    }

    public HashMap<String, String> getFieldsByNameList(Object instance, HashMap<String, String> fieldList) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> fieldName : fieldList.entrySet()) {
            result.put(fieldName.getKey(), getFieldStringValue(instance, fieldName.getValue()));
        }
        return result;
    }

    public HashMap<String, String> arrayDump(Object instance) {
        HashMap<String, String> result = new HashMap<String, String>();
        if (instance instanceof ObjectArrayDump) {
            ObjectArrayDump arrayDump = (ObjectArrayDump) instance;
            for (Instance _entry : arrayDump.getValues()) {
                if (_entry == null) continue;
                result.put(getFieldStringValue(_entry, "key"), getFieldStringValue(_entry, "value"));
            }
        }
        return result;
    }

    public Instance[] getArrayItems(Object instance) {
        if (instance instanceof ObjectArrayDump) {
            ObjectArrayDump arrayDump = (ObjectArrayDump) instance;
            return arrayDump.getValues().toArray(new Instance[0]);
        }
        return new Instance[0];
    }

    public String getFieldStringValue(Object instance, String fieldName) {
        Object val = getFieldValue(instance, fieldName);
        if (val instanceof Instance) {
            return toString((Instance) val);
        } else if (val != null) {
            return String.valueOf(val);
        }
        return null;
    }

    public Object getFieldValue(Object _instance, String fieldName) {
        Instance instance = (Instance) _instance;
        if (fieldName.contains(".")) {
            Object fInstance = instance.getValueOfField(fieldName.substring(0, fieldName.indexOf(".")));
            if (fInstance != null) {
                return getFieldValue((Instance) fInstance, fieldName.substring(fieldName.indexOf(".") + 1));
            } else {
                return null;
            }
        } else {
            if (fieldName.equals("@ID"))
                return String.valueOf(instance.getInstanceId());
            Object fInstance = instance.getValueOfField(fieldName);
            if (fInstance != null) {
                if (fInstance instanceof Integer) {
                    return String.valueOf(fInstance);
                }
                return instance.getValueOfField(fieldName);
            } else {
                return null;
            }
        }
    }

    static final List<String> mapClassList = Arrays.asList(
            "java.util.HashMap",
            "java.util.Properties",
            "java.util.LinkedHashMap",
            "java.util.Collections$UnmodifiableMap"
    );

    public boolean isMap(Object _instance) {
        if (_instance != null) {
            Instance instance = (Instance) _instance;
            String className = instance.getJavaClass().getName();
            return mapClassList.contains(className);
        } else return false;
    }

    public Instance getMap(Object _instance) {
        if (_instance != null) {
            Instance instance = (Instance) _instance;
            Object table = instance.getValueOfField("table");
            if (table == null)
                table = getFieldValue(instance, "source.table");
            if (table != null) {
                return (Instance) table;
            } else {
                Object m1 = instance.getValueOfField("m");
                if (m1 != null) {
                    Object m2 = ((Instance) m1).getValueOfField("m");
                    if (m2 != null) {
                        return (Instance) ((Instance) m2).getValueOfField("table");
                    } else {
                        return (Instance) ((Instance) m1).getValueOfField("table");
                    }
                } else {
                    return null;
                }
            }
        } else return null;
    }

    public String toString(Object _instance) {
        Instance instance = (Instance) _instance;
        String instanceClassName = instance.getJavaClass().getName();
        if (instanceClassName.equals("java.lang.String")) {
            PrimitiveArrayDump arrayDump = (PrimitiveArrayDump) (instance.getValueOfField("value"));
            if (arrayDump == null)
                return "";
            if (arrayDump.getJavaClass().getName().equals("byte[]")) {
                List<String> byteStr = arrayDump.getValues();
                byte[] target = new byte[byteStr.size()];
                for (int i = 0; i < byteStr.size(); i++) {
                    target[i] = (byte) Integer.parseInt(byteStr.get(i));
                }
                return new String(target);
            }
            return join("", arrayDump.getValues());
        } else if (instanceClassName.equals("char[]")) {
            return join("", ((PrimitiveArrayDump) instance).getValues());
        } else {
            Object val = instance.getValueOfField("value");
            if (val instanceof Instance) {
                return toString(val);
            }
        }
        return null;
    }

    public byte[] toByteArray(Object _instance) {
        if (_instance instanceof PrimitiveArrayDump) {
            PrimitiveArrayDump arrayDump = (PrimitiveArrayDump) _instance;
            if (arrayDump.getJavaClass().getName().equals("byte[]")) {
                List<String> byteStr = arrayDump.getValues();
                byte[] target = new byte[byteStr.size()];
                for (int i = 0; i < byteStr.size(); i++) {
                    target[i] = (byte) Integer.parseInt(byteStr.get(i));
                }
                return target;
            }
        }
        return null;
    }

    public String join(CharSequence delimiter,
                       Iterable<? extends CharSequence> elements) {
        _StringJoiner.requireNonNull(delimiter);
        _StringJoiner.requireNonNull(elements);
        _StringJoiner joiner = new _StringJoiner(delimiter);
        for (CharSequence cs : elements) {
            joiner.add(cs);
        }
        return joiner.toString();
    }

    // Enhanced methods for v2.0

    private static final int MIN_ARRAY_LENGTH = 10;
    private static final int MAX_ARRAY_LENGTH = 10000;

    @Override
    public List<String> searchStrings(Pattern pattern) {
        return searchAllTexts(pattern);
    }

    @Override
    public List<String> searchAllTexts(Pattern pattern) {
        List<String> results = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        // Phase 1: scan java.lang.String instances
        JavaClass stringClass = findClass("java.lang.String");
        if (stringClass != null) {
            for (Object inst : getInstances(stringClass)) {
                if (cancelled.get()) return results;
                String str = toString(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        // Phase 2: scan byte[] instances
        JavaClass byteArrayClass = findClass("byte[]");
        if (byteArrayClass != null) {
            for (Object inst : getInstances(byteArrayClass)) {
                if (cancelled.get()) return results;
                if (!(inst instanceof PrimitiveArrayInstance)) continue;
                int length = ((PrimitiveArrayInstance) inst).getLength();
                if (length < MIN_ARRAY_LENGTH || length > MAX_ARRAY_LENGTH) continue;
                String str = toString(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        // Phase 3: scan char[] instances
        JavaClass charArrayClass = findClass("char[]");
        if (charArrayClass != null) {
            for (Object inst : getInstances(charArrayClass)) {
                if (cancelled.get()) return results;
                if (!(inst instanceof PrimitiveArrayInstance)) continue;
                int length = ((PrimitiveArrayInstance) inst).getLength();
                if (length < MIN_ARRAY_LENGTH || length > MAX_ARRAY_LENGTH) continue;
                String str = toString(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        // Phase 4: scan StringBuilder instances
        JavaClass sbClass = findClass("java.lang.StringBuilder");
        if (sbClass != null) {
            for (Object inst : getInstances(sbClass)) {
                if (cancelled.get()) return results;
                String str = extractStringBuilder(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        // Phase 5: scan StringBuffer instances
        JavaClass sbufClass = findClass("java.lang.StringBuffer");
        if (sbufClass != null) {
            for (Object inst : getInstances(sbufClass)) {
                if (cancelled.get()) return results;
                String str = extractStringBuilder(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        // Phase 6: scan CharBuffer instances
        JavaClass charBufClass = findClass("java.nio.CharBuffer");
        if (charBufClass != null) {
            for (Object inst : getInstances(charBufClass)) {
                if (cancelled.get()) return results;
                String str = extractCharBuffer(inst);
                if (str != null && pattern.matcher(str).find() && seen.add(str)) {
                    results.add(str);
                }
            }
        }

        return results;
    }

    private String extractStringBuilder(Object instance) {
        if (!(instance instanceof Instance)) return null;
        Instance inst = (Instance) instance;
        Object valueField = inst.getValueOfField("value");
        if (valueField instanceof Instance) {
            return toString(valueField);
        }
        return null;
    }

    private String extractCharBuffer(Object instance) {
        if (!(instance instanceof Instance)) return null;
        Instance inst = (Instance) instance;
        // CharBuffer usually wraps a char[] or has a hb (heap buffer) field
        Object hb = inst.getValueOfField("hb");
        if (hb instanceof Instance) {
            return toString(hb);
        }
        return null;
    }

    @Override
    public List<String> searchRawMemory(Pattern pattern) {
        List<String> results = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        
        if (heapFile == null || !heapFile.exists()) {
            return results;
        }

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(heapFile, "r")) {
            long fileSize = raf.length();
            long chunkSize = 1024 * 1024; // 1MB chunks
            byte[] buffer = new byte[(int) chunkSize];
            
            for (long offset = 0; offset < fileSize; offset += chunkSize) {
                if (cancelled.get()) return results;
                
                int readLen = (int) Math.min(chunkSize, fileSize - offset);
                raf.seek(offset);
                int bytesRead = raf.read(buffer, 0, readLen);
                if (bytesRead <= 0) continue;
                
                // Scan for printable strings in this chunk
                scanChunkForPattern(buffer, bytesRead, pattern, results, seen);
            }
        } catch (Exception e) {
            // Ignore errors in raw scanning
        }
        
        return results;
    }

    private void scanChunkForPattern(byte[] buffer, int length, Pattern pattern, 
                                     List<String> results, HashSet<String> seen) {
        int minPrintable = 4;  // Minimum printable sequence length
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            byte b = buffer[i];
            // Check if byte is printable ASCII (similar to strings command)
            if (b >= 32 && b < 127) {
                current.append((char) b);
            } else {
                // End of printable sequence
                if (current.length() >= minPrintable) {
                    String str = current.toString();
                    // Try to match pattern against this string
                    java.util.regex.Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        String match = matcher.group();
                        if (seen.add(match)) {
                            results.add(match);
                        }
                    }
                }
                current.setLength(0);
            }
        }
        
        // Handle end of buffer
        if (current.length() >= minPrintable) {
            String str = current.toString();
            java.util.regex.Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                String match = matcher.group();
                if (seen.add(match)) {
                    results.add(match);
                }
            }
        }
    }

    @Override
    public List<String> searchAll(Pattern pattern) {
        List<String> results = new ArrayList<>(searchAllTexts(pattern));
        HashSet<String> seen = new HashSet<>(results);
        for (String raw : searchRawMemory(pattern)) {
            if (seen.add(raw)) {
                results.add(raw);
            }
        }
        return results;
    }

    @Override
    public List<Object> findClassesByPattern(String pattern) {
        List<Object> results = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        Iterator it = getClasses();
        while (it.hasNext()) {
            Object cls = it.next();
            String name = getClassName(cls);
            if (name != null && p.matcher(name).find()) {
                results.add(cls);
            }
        }
        return results;
    }

    @Override
    public HashMap<String, String> getAllFieldValues(Object instance) {
        HashMap<String, String> result = new HashMap<>();
        if (!(instance instanceof Instance)) return result;
        Instance inst = (Instance) instance;
        JavaClass cls = inst.getJavaClass();
        while (cls != null) {
            for (Field field : (List<Field>) cls.getFields()) {
                String name = field.getName();
                String value = getFieldStringValue(inst, name);
                if (value != null) {
                    result.put(name, value);
                }
            }
            cls = cls.getSuperClass();
        }
        return result;
    }

    @Override
    public List<Object> traverseLinkedList(Object instance) {
        List<Object> results = new ArrayList<>();
        if (!(instance instanceof Instance)) return results;
        Set<Long> visited = new HashSet<>();
        Object current = instance;
        while (current instanceof Instance && !visited.contains(((Instance) current).getInstanceId())) {
            visited.add(((Instance) current).getInstanceId());
            results.add(current);
            Object next = getValueOfField(current, "next");
            if (next == null || next == current) break;
            current = next;
        }
        return results;
    }
}
