package cn.wanghw.spider;

import java.util.HashMap;

public class Redis01 extends AbstractClassFieldSpider {

    public String getName() {
        return "RedisStandaloneConfiguration";
    }

    @Override
    protected boolean isSingleLine() {
        return true;
    }

    @Override
    protected String getTargetClassName() {
        return "org.springframework.data.redis.connection.RedisStandaloneConfiguration";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("hostName", "hostName");
            put("port", "port");
            put("password", "password.thePassword");
            put("database", "database");
        }};
    }
}
