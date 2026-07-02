package cn.wanghw.spider;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

public class CookieThief implements ISpider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieThief.class);
    private static final Pattern COOKIE_PATTERN = Pattern.compile("Cookie:");

    public String getName() {
        return "CookieThief";
    }

    public String sniff(IHeapHolder heapHolder)  {
        final StringBuilder result = new StringBuilder();
        try {
            List<String> matches = heapHolder.searchAllTexts(COOKIE_PATTERN);
            for (String text : matches) {
                result.append(text).append("\r\n");
            }
        } catch (Exception ex) {
            LOGGER.warn("Spider {} failed: {}", getName(), ex.getMessage());
        }
        return result.toString();
    }
}
