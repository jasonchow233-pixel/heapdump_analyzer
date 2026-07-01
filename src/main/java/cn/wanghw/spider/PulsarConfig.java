package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulsarConfig implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarConfig.class);


    public String getName() { return "PulsarConfig"; }
    public String getCategory() { return "mq"; }
    public String getDescription() { return "Extract Apache Pulsar client configuration and authentication"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // PulsarClientImpl
            Object clazz1 = heapHolder.findClass("org.apache.pulsar.client.impl.PulsarClientImpl");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[PulsarClient] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // ClientConfigurationData
            Object clazz2 = heapHolder.findClass("org.apache.pulsar.client.impl.conf.ClientConfigurationData");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("serviceUrl", "serviceUrl");
                    put("authPluginClassName", "authPluginClassName");
                    put("authParams", "authParams");
                    put("tlsTrustCertsFilePath", "tlsTrustCertsFilePath");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[PulsarClientConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
