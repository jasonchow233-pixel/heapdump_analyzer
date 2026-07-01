package cn.wanghw.spider;

import java.util.HashMap;

public class DataSource03 extends AbstractClassFieldSpider {

    public String getName() {
        return "MongoClient";
    }

    @Override
    protected String getTargetClassName() {
        return "com.mongodb.MongoClient";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("host", "cluster.settings.hosts.list.elementData.host");
            put("port", "cluster.settings.hosts.list.elementData.port");
            put("username", "credentialsList.list.elementData.userName");
            put("password", "credentialsList.list.elementData.password");
            put("database", "credentialsList.list.elementData.source");
        }};
    }
}
