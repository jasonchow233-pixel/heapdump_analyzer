package cn.wanghw.spider;

import java.util.HashMap;

public class DataSource02 extends AbstractClassFieldSpider {

    public String getName() {
        return "WeblogicDataSourceConnectionPoolConfig";
    }

    @Override
    protected String getTargetClassName() {
        return "weblogic.jdbc.common.internal.DataSourceConnectionPoolConfig";
    }

    @Override
    protected HashMap<String, String> getFieldList() {
        return new HashMap<String, String>() {{
            put("url", "url");
            put("driver", "driver");
            put("name", "name");
            put("username", "defaultConnectionInfo.username");
            put("password", "defaultConnectionInfo.p");
        }};
    }
}
