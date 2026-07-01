package cn.wanghw.spider;

import java.util.HashMap;

public class DataSource04 extends AbstractClassFieldSpider {

    public String getName() {
        return "AliDruidDataSourceWrapper";
    }

    @Override
    protected String getTargetClassName() {
        return "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceWrapper";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("username", "username");
            put("password", "password");
            put("jdbcUrl", "jdbcUrl");
        }};
    }
}
