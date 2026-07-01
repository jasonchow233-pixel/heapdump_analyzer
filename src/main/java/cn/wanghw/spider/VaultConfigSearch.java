package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract HashiCorp Vault credentials from vault-java-driver / spring-cloud-vault
 * io.github.jopenlibs.vault.Vault, org.springframework.cloud.vault.client.VaultProperties
 */
public class VaultConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "VaultConfig";
    }

    @Override
    public String getCategory() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Extract HashiCorp Vault configuration (token, address, role ID, secret ID) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "org.springframework.cloud.vault.client.VaultProperties",
                "io.github.jopenlibs.vault.Vault"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"host", "port", "scheme", "uri", "token",
                            "namespace", "roleId", "secretId", "path"};
                    for (String fn : fieldNames) {
                        String val = heapHolder.getFieldStringValue(instance, fn);
                        if (val != null && !val.isEmpty()) {
                            fields.put(fn, val);
                        }
                    }
                    if (!fields.isEmpty()) {
                        result.append(HashMapUtils.dumpString(fields, false)).append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }
        return result.toString();
    }
}
