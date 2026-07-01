package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeserializeChain implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeserializeChain.class);


    public String getName() {
        return "DeserializeChain";
    }

    public String getCategory() {
        return "vulnerability";
    }

    public String getDescription() {
        return "Detect dangerous deserialization gadgets in classpath (CommonsCollections, etc.)";
    }

    public cn.wanghw.Severity getSeverity() {
        return cn.wanghw.Severity.CRITICAL;
    }

    static final String[] DANGEROUS_CLASSES = {
            "org.apache.commons.collections.functors.InvokerTransformer",
            "org.apache.commons.collections.functors.InstantiateTransformer",
            "org.apache.commons.collections.functors.ConstantTransformer",
            "org.apache.commons.collections.functors.ChainedTransformer",
            "org.apache.commons.collections4.functors.InvokerTransformer",
            "org.apache.commons.collections4.functors.InstantiateTransformer",
            "org.apache.xalan.xsltc.trax.TemplatesImpl",
            "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl",
            "org.springframework.beans.factory.ObjectFactory",
    };

    static final String[][] GADGET_CHAINS = {
            {"org.apache.commons.collections.Transformer", "CommonsCollections1 (Transformer)"},
            {"org.apache.commons.collections4.Transformer", "CommonsCollections1 (Transformer4)"},
            {"org.apache.commons.collections.map.LazyMap", "CommonsCollections6/7 (LazyMap)"},
            {"org.apache.commons.collections4.map.LazyMap", "CommonsCollections6/7 (LazyMap4)"},
    };

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // Check for dangerous classes loaded in the JVM
            for (String className : DANGEROUS_CLASSES) {
                Object clazz = heapHolder.findClass(className);
                if (clazz != null) {
                    List instances = heapHolder.getInstances(clazz);
                    result.append("[DANGEROUS] ").append(className)
                            .append(" (instances: ").append(instances.size()).append(")\n");
                }
            }
            // Check for gadget chains
            for (String[] chain : GADGET_CHAINS) {
                Object clazz = heapHolder.findClass(chain[0]);
                if (clazz != null) {
                    result.append("[CHAIN] ").append(chain[1]).append(" detected!\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
