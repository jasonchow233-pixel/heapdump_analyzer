package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Base class for Spiders that extract a fixed set of named fields from all
 * instances of a single target class. Eliminates the findClass / null-check /
 * iterate / dumpString boilerplate repeated across DataSource01-04, Redis01-02
 * and similar plugins.
 *
 * <p>Subclasses provide:</p>
 * <ul>
 *   <li>{@link #getTargetClassName()} - the fully-qualified class to locate</li>
 *   <li>{@link #getFieldList()} - display-name to field-path mapping</li>
 *   <li>{@link #isSingleLine()} (optional) - whether HashMapUtils dumps one line</li>
 * </ul>
 */
public abstract class AbstractClassFieldSpider implements ISpider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClassFieldSpider.class);

    protected abstract String getTargetClassName();

    protected abstract HashMap<String, String> getFieldList();

    protected boolean isSingleLine() {
        return false;
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass(getTargetClassName());
            if (clazz == null) return null;
            HashMap<String, String> fieldList = getFieldList();
            for (Object instance : heapHolder.getInstances(clazz)) {
                result.append(HashMapUtils.dumpString(
                        heapHolder.getFieldsByNameList(instance, fieldList),
                        isSingleLine()));
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
