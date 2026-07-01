package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerRegistrySearch implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerRegistrySearch.class);


    public String getName() { return "DockerRegistrySearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract Docker registry credentials from docker-java client configuration"; }
    public cn.wanghw.Severity getSeverity() { return cn.wanghw.Severity.HIGH; }

    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        try {
            // DefaultDockerClientConfig
            Object clazz1 = heapHolder.findClass("com.github.dockerjava.core.DefaultDockerClientConfig");
            if (clazz1 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("dockerHost", "dockerHost");
                    put("registryUrl", "registryUrl");
                    put("username", "username");
                    put("password", "password");
                    put("email", "email");
                }};
                for (Object instance : heapHolder.getInstances(clazz1)) {
                    result.append("[DockerConfig] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
            // AuthConfig
            Object clazz2 = heapHolder.findClass("com.github.dockerjava.api.model.AuthConfig");
            if (clazz2 != null) {
                HashMap<String, String> fieldList = new HashMap<String, String>() {{
                    put("registryAddress", "registryAddress");
                    put("username", "username");
                    put("password", "password");
                    put("auth", "auth");
                }};
                for (Object instance : heapHolder.getInstances(clazz2)) {
                    result.append("[DockerAuth] ").append(
                            HashMapUtils.dumpString(heapHolder.getFieldsByNameList(instance, fieldList), false));
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
