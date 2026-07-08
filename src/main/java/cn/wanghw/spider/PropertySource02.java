package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertySource02 implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertySource02.class);



    public String getName() {
        return "MutablePropertySources";
    }


    public String sniff(IHeapHolder heapHolder) {

        final StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("org.springframework.core.env.MutablePropertySources");
            if (clazz == null)
                return null;
            HashMap<String, String> values = new HashMap<String, String>();
            for (Object instance : heapHolder.getInstances(clazz)) {
                Object[] array = heapHolder.getArrayItems(heapHolder.getFieldValue(instance, "propertySourceList.array"));
                for (Object source : array) {
                    values.putAll(heapHolder.arrayDump(heapHolder.getMap(source)));
                }
            }
            // 使用对齐格式输出（排序 + 自动对齐）
            result.append(HashMapUtils.dumpStringAligned(values, false, false, false, true));
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
