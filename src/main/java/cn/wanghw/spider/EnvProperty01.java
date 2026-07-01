package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvProperty01 implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvProperty01.class);


    public String getName() {
        return "ProcessEnvironment";
    }


    public String sniff(IHeapHolder heapHolder) {

        final StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.lang.ProcessEnvironment");
            if (clazz == null)
                return null;
            HashMap<String, String> values = new HashMap<String, String>();
            for (Object instance : heapHolder.getInstances(clazz)) {
                values.putAll(heapHolder.arrayDump(heapHolder.getMap(instance)));
            }
            result.append(HashMapUtils.dumpString(values, false));
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
