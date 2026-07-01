package cn.wanghw;

public interface ISpider {
    String getName();

    String sniff(IHeapHolder heapHolder);

    default String getCategory() {
        return "default";
    }

    default String getDescription() {
        return "";
    }

    default Severity getSeverity() {
        return Severity.MEDIUM;
    }
}
