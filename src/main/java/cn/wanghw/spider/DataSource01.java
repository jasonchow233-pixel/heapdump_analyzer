package cn.wanghw.spider;

import cn.wanghw.Severity;

import java.util.HashMap;

public class DataSource01 extends AbstractClassFieldSpider {

    public String getName() {
        return "SpringDataSourceProperties";
    }

    public String getCategory() { return "database"; }
    public String getDescription() { return "Extract Spring Boot DataSource credentials"; }
    public Severity getSeverity() { return Severity.CRITICAL; }

    @Override
    protected String getTargetClassName() {
        return "org.springframework.boot.autoconfigure.jdbc.DataSourceProperties";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("driverClassName", "driverClassName");
            put("username", "username");
            put("password", "password");
            put("url", "url");
        }};
    }
}
