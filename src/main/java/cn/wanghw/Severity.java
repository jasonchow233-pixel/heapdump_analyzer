package cn.wanghw;

public enum Severity {
    CRITICAL("Critical", "#f38ba8"),
    HIGH("High", "#fab387"),
    MEDIUM("Medium", "#f9e2af"),
    LOW("Low", "#a6e3a1"),
    INFO("Info", "#89dceb");

    private final String label;
    private final String color;

    Severity(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
