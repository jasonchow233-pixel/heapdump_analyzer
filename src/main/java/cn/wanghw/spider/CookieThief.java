package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieThief implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieThief.class);

    public String getName() {
        return "CookieThief";
    }

    public String sniff(IHeapHolder heapHolder)  {
        final StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("java.lang.String");
            if (clazz == null)
                return null;
            for (Object instance : heapHolder.getInstances(clazz)) {
                String text = heapHolder.toString(instance);
                if (text.contains("Cookie:")) {
                    result.append(heapHolder.toString(instance)).append("\r\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
