package cn.wanghw.spider;

import java.util.HashMap;

public class Redis02 extends AbstractClassFieldSpider {

    public String getName() {
        return "JedisClient";
    }

    @Override
    protected boolean isSingleLine() {
        return true;
    }

    @Override
    protected String getTargetClassName() {
        return "redis.clients.jedis.Client";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("hostname", "hostname");
            put("port", "port");
            put("password", "password");
            put("database", "db");
        }};
    }
}
