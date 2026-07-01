package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;

/**
 * Extract Shopify credentials from shopify-sdk-java
 * com.shopify.ShopifyClient, com.shopify.model.Shop
 */
public class ShopifyConfigSearch implements ISpider {
    @Override
    public String getName() {
        return "ShopifyConfig";
    }

    @Override
    public String getCategory() {
        return "cloud";
    }

    @Override
    public String getDescription() {
        return "Extract Shopify store credentials (shop domain, access token) from heap";
    }

    @Override
    public String sniff(IHeapHolder heapHolder) {
        StringBuilder result = new StringBuilder();
        String[] classNames = {
                "com.shopify.ShopifyClient",
                "com.shopify.model.ShopifyShop"
        };
        for (String className : classNames) {
            try {
                Object clazz = heapHolder.findClass(className);
                if (clazz == null) continue;
                for (Object instance : heapHolder.getInstances(clazz)) {
                    HashMap<String, String> fields = new HashMap<>();
                    String[] fieldNames = {"shopDomain", "accessToken", "apiKey",
                            "password", "sharedSecret", "shopName"};
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
