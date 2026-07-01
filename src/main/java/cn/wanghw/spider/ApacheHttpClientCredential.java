package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHttpClientCredential implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheHttpClientCredential.class);


    public String getName() { return "ApacheHttpClientCredential"; }
    public String getCategory() { return "http"; }
    public String getDescription() { return "Extract Apache HttpClient credentials provider and proxy configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // InternalHttpClient
            Object clazz1 = heapHolder.findClass("org.apache.http.impl.client.InternalHttpClient");
            if (clazz1 == null)
                clazz1 = heapHolder.findClass("org.apache.http.impl.client.CloseableHttpClient");
            if (clazz1 != null) {
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[ApacheHttpClient] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // BasicCredentialsProvider
            Object clazz2 = heapHolder.findClass("org.apache.http.impl.client.BasicCredentialsProvider");
            if (clazz2 != null) {
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    HashMap<String, String> vals = heapHolder.getAllFieldValues(instance);
                    result.append("[HttpCredentialsProvider] ").append(HashMapUtils.dumpString(vals, false));
                }
            }
            // UsernamePasswordCredentials
            Object clazz3 = heapHolder.findClass("org.apache.http.auth.UsernamePasswordCredentials");
            if (clazz3 != null) {
                for (Object instance : heapHolder.getInstances(clazz3)) {
                    String userName = heapHolder.getFieldStringValue(instance, "userName");
                    String password = heapHolder.getFieldStringValue(instance, "password");
                    result.append("[HttpCredentials] username=").append(userName)
                            .append(", password=").append(password).append("\n");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
