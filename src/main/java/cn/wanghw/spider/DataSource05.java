package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.utils.HashMapUtils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataSource05 implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSource05.class);


    public String getName() {
        return "HikariDataSource";
    }


    public String sniff(IHeapHolder heapHolder) {

        final StringBuilder result = new StringBuilder();
        try {
            Object clazz = heapHolder.findClass("com.zaxxer.hikari.util.DriverDataSource");
            if (clazz == null)
                return null;
            HashMap<String, String> fieldList = new HashMap<String, String>() {{
                put("jdbcUrl", "jdbcUrl");
                put("tableId", "driverProperties.table.@ID");
            }};
            for (Object instance : heapHolder.getInstances(clazz)) {
                HashMap<String, String> fieldValue = heapHolder.getFieldsByNameList(instance, fieldList);
                String tableId = fieldValue.get("tableId");
                if (tableId == null) continue;
                Object paramsTable = heapHolder.findThing(Long.parseLong(tableId));
                fieldValue.remove("tableId");
                fieldValue.putAll(heapHolder.arrayDump(paramsTable));
                result.append(HashMapUtils.dumpString(fieldValue, false));
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
