package com.heapdump.analyzer.ui.swing;

import cn.wanghw.IHeapHolder;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomSearchDialog extends JDialog {

    private final IHeapHolder heapHolder;
    private JTextField searchField;
    private JCheckBox regexCheckbox;
    private JCheckBox caseSensitiveCheckbox;
    private JSpinner contextSizeSpinner;
    private JButton searchButton;
    private JButton exportButton;
    private JButton closeButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<SearchResult> searchResults = new ArrayList<>();

    public CustomSearchDialog(Window parent, IHeapHolder heapHolder) {
        super(parent, "Custom String Search", ModalityType.MODELESS);
        this.heapHolder = heapHolder;
        initComponents();
        layoutComponents();
        registerKeyBindings();

        setSize(900, 600);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(700, 400));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void initComponents() {
        searchField = new JTextField(30);
        searchField.putClientProperty("JTextField.placeholderText", "Enter search string...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(ThemeConfig.FONT_BODY);

        regexCheckbox = new JCheckBox("Regex");
        regexCheckbox.setFont(ThemeConfig.FONT_CAPTION);
        regexCheckbox.setToolTipText("Treat search string as regular expression");

        caseSensitiveCheckbox = new JCheckBox("Case Sensitive");
        caseSensitiveCheckbox.setFont(ThemeConfig.FONT_CAPTION);
        caseSensitiveCheckbox.setToolTipText("Enable case-sensitive matching");

        contextSizeSpinner = new JSpinner(new SpinnerNumberModel(5000, 0, 10000, 100));
        contextSizeSpinner.setFont(ThemeConfig.FONT_CAPTION);
        contextSizeSpinner.setToolTipText("Number of characters to show before and after match");

        searchButton = new JButton("Search", new FlatSVGIcon("icons/search.svg", 14, 14));
        searchButton.putClientProperty("JButton.buttonType", "roundRect");
        searchButton.setFont(ThemeConfig.FONT_CAPTION);
        searchButton.addActionListener(e -> performSearch());

        exportButton = new JButton("Export", new FlatSVGIcon("icons/export.svg", 14, 14));
        exportButton.putClientProperty("JButton.buttonType", "roundRect");
        exportButton.setFont(ThemeConfig.FONT_CAPTION);
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportResults());

        closeButton = new JButton("Close");
        closeButton.setFont(ThemeConfig.FONT_CAPTION);
        closeButton.addActionListener(e -> dispose());

        tableModel = new DefaultTableModel(
            new Object[]{"Match", "Context"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setFont(ThemeConfig.FONT_BODY);
        resultsTable.setRowHeight(28);
        resultsTable.getTableHeader().setFont(ThemeConfig.FONT_CAPTION.deriveFont(Font.BOLD));
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showContextInDetailPanel(e);
            }
        });

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(ThemeConfig.FONT_CAPTION);
        statusLabel.setForeground(ThemeConfig.getMutedTextColor());

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 16));
    }

    private void layoutComponents() {
        setLayout(new MigLayout("insets 12, fill, wrap 1", "[grow, fill]", "[][][grow, fill][]"));

        // Search panel
        JPanel searchPanel = new JPanel(new MigLayout("insets 0, fillx", "[][grow, fill][][][][]", "[]"));
        searchPanel.setBackground(ThemeConfig.getCardBackground());
        searchPanel.setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        searchPanel.add(new JLabel("Search:"), "gapright 8");
        searchPanel.add(searchField, "growx");
        searchPanel.add(regexCheckbox, "gapleft 12");
        searchPanel.add(caseSensitiveCheckbox, "gapleft 8");
        searchPanel.add(new JLabel("Context:"), "gapleft 12, gapright 4");
        searchPanel.add(contextSizeSpinner, "w 60!");
        searchPanel.add(searchButton, "gapleft 12");

        add(searchPanel, "growx");

        // Quick search buttons panel
        JPanel quickSearchPanel = new JPanel(new MigLayout("insets 4 0, fillx", "push[][][][][]push", "[]"));
        quickSearchPanel.setBackground(ThemeConfig.getCardBackground());
        quickSearchPanel.setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        JLabel quickSearchLabel = new JLabel("Quick Search:");
        quickSearchLabel.setFont(ThemeConfig.FONT_CAPTION.deriveFont(Font.BOLD));
        quickSearchPanel.add(quickSearchLabel, "gapright 8");

        // HTTP Request button
        JButton httpRequestButton = new JButton("HTTP Request", new FlatSVGIcon("icons/network.svg", 12, 12));
        httpRequestButton.putClientProperty("JButton.buttonType", "roundRect");
        httpRequestButton.setFont(ThemeConfig.FONT_CAPTION);
        httpRequestButton.setToolTipText("Search for HTTP request/response data");
        httpRequestButton.addActionListener(e -> searchHttpRequests());
        quickSearchPanel.add(httpRequestButton, "gapright 8");

        // JSON Data button
        JButton jsonDataButton = new JButton("JSON Data", new FlatSVGIcon("icons/code.svg", 12, 12));
        jsonDataButton.putClientProperty("JButton.buttonType", "roundRect");
        jsonDataButton.setFont(ThemeConfig.FONT_CAPTION);
        jsonDataButton.setToolTipText("Search for JSON format data");
        jsonDataButton.addActionListener(e -> searchJsonData());
        quickSearchPanel.add(jsonDataButton, "gapright 8");

        // URL Pattern button
        JButton urlButton = new JButton("URL Pattern", new FlatSVGIcon("icons/link.svg", 12, 12));
        urlButton.putClientProperty("JButton.buttonType", "roundRect");
        urlButton.setFont(ThemeConfig.FONT_CAPTION);
        urlButton.setToolTipText("Search for URL/URI patterns");
        urlButton.addActionListener(e -> searchUrlPatterns());
        quickSearchPanel.add(urlButton, "gapright 8");

        // API Endpoint button
        JButton apiButton = new JButton("API Endpoints", new FlatSVGIcon("icons/api.svg", 12, 12));
        apiButton.putClientProperty("JButton.buttonType", "roundRect");
        apiButton.setFont(ThemeConfig.FONT_CAPTION);
        apiButton.setToolTipText("Search for API endpoints");
        apiButton.addActionListener(e -> searchApiEndpoints());
        quickSearchPanel.add(apiButton, "gapright 8");

        add(quickSearchPanel, "growx");

        // Results table
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(null);
        add(scrollPane, "growx, growy");

        // Button panel
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, fillx", "push[][][]", "[]"));
        buttonPanel.setBackground(ThemeConfig.getCardBackground());
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, ThemeConfig.getBorderColor()));

        buttonPanel.add(statusLabel, "pushx");
        buttonPanel.add(progressBar, "gapright 8");
        buttonPanel.add(exportButton, "gapright 8");
        buttonPanel.add(closeButton);

        add(buttonPanel, "growx");
    }

    private void registerKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                performSearch();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actionMap.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a search string",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (heapHolder == null) {
            JOptionPane.showMessageDialog(this,
                "No heap dump loaded",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        exportButton.setEnabled(false);
        statusLabel.setText("Searching...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        executor.submit(() -> {
            try {
                List<SearchResult> results = executeSearch(searchText);
                SwingUtilities.invokeLater(() -> {
                    displayResults(results);
                    searchButton.setEnabled(true);
                    progressBar.setVisible(false);
                    exportButton.setEnabled(!results.isEmpty());
                    statusLabel.setText(String.format("Found %d matches", results.size()));
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    searchButton.setEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        });
    }

    private List<SearchResult> executeSearch(String searchText) {
        List<SearchResult> results = new ArrayList<>();
        int contextSize = (Integer) contextSizeSpinner.getValue();
        boolean useRegex = regexCheckbox.isSelected();
        boolean caseSensitive = caseSensitiveCheckbox.isSelected();

        Pattern pattern;
        if (useRegex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(searchText, flags);
        } else {
            String escaped = Pattern.quote(searchText);
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(escaped, flags);
        }

        // 搜索 String 实例并获取对象信息和引用路径
        try {
            Object stringClass = heapHolder.findClass("java.lang.String");
            if (stringClass != null) {
                java.util.List instances = heapHolder.getInstances(stringClass);
                java.util.Set<String> seen = new java.util.HashSet<>();

                for (Object instance : instances) {
                    if (Thread.currentThread().isInterrupted()) break;

                    try {
                        String strValue = heapHolder.toString(instance);
                        if (strValue == null || !pattern.matcher(strValue).find()) continue;

                        // 避免重复结果
                        if (seen.contains(strValue)) continue;
                        seen.add(strValue);

                        // 提取匹配和上下文
                        Matcher matcher = pattern.matcher(strValue);
                        while (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();
                            String matchedText = matcher.group();

                            // 计算上下文边界
                            int contextStart = Math.max(0, start - contextSize);
                            int contextEnd = Math.min(strValue.length(), end + contextSize);
                            String context = strValue.substring(contextStart, contextEnd);

                            String before = strValue.substring(contextStart, start);
                            String after = strValue.substring(end, contextEnd);

                            // 获取对象信息
                            String className = "java.lang.String";
                            Long objectId = null;
                            String referencePath = null;
                            String referrerInfo = null;

                            // 尝试获取对象ID和引用信息
                            try {
                                if (instance instanceof org.graalvm.visualvm.lib.jfluid.heap.Instance) {
                                    org.graalvm.visualvm.lib.jfluid.heap.Instance heapInstance =
                                        (org.graalvm.visualvm.lib.jfluid.heap.Instance) instance;
                                    objectId = heapInstance.getInstanceId();

                                    // 获取引用者信息
                                    referrerInfo = getReferrerInfo(heapInstance);
                                }
                            } catch (Exception e) {
                                // 忽略获取对象信息的错误
                            }

                            SearchResult result = new SearchResult(
                                matchedText, before, after, context, strValue,
                                instance, className, objectId, referencePath, referrerInfo
                            );
                            results.add(result);
                        }
                    } catch (Exception e) {
                        // 忽略单个实例的错误
                    }
                }
            }
        } catch (Exception e) {
            // 如果实例搜索失败，回退到字符串搜索
            List<String> matches = heapHolder.searchAll(pattern);
            for (String match : matches) {
                Matcher matcher = pattern.matcher(match);
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    String matchedText = matcher.group();

                    int contextStart = Math.max(0, start - contextSize);
                    int contextEnd = Math.min(match.length(), end + contextSize);
                    String context = match.substring(contextStart, contextEnd);

                    String before = match.substring(contextStart, start);
                    String after = match.substring(end, contextEnd);

                    SearchResult result = new SearchResult(matchedText, before, after, context, match);
                    results.add(result);
                }
            }
        }

        return results;
    }

    /**
     * 获取对象的引用者信息，并自动展开HTTP请求对象的详细信息
     */
    private String getReferrerInfo(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        try {
            StringBuilder info = new StringBuilder();
            java.util.Iterator<?> referrers = instance.getReferences().iterator();
            int count = 0;
            int maxReferrers = 10;  // 最多显示10个引用者

            while (referrers.hasNext() && count < maxReferrers) {
                Object ref = referrers.next();
                if (ref instanceof org.graalvm.visualvm.lib.jfluid.heap.Value) {
                    org.graalvm.visualvm.lib.jfluid.heap.Value value =
                        (org.graalvm.visualvm.lib.jfluid.heap.Value) ref;
                    org.graalvm.visualvm.lib.jfluid.heap.Instance refInstance = value.getDefiningInstance();
                    if (refInstance != null) {
                        if (info.length() > 0) info.append("\n\n");
                        info.append("════════════════════════════════════════\n");
                        info.append("引用对象 #").append(count + 1).append("\n");
                        info.append("════════════════════════════════════════\n");

                        String className = refInstance.getJavaClass().getName();
                        info.append("类名: ").append(className).append("\n");
                        info.append("对象ID: ").append(refInstance.getInstanceId()).append("\n");

                        if (value instanceof org.graalvm.visualvm.lib.jfluid.heap.FieldValue) {
                            String fieldName = ((org.graalvm.visualvm.lib.jfluid.heap.FieldValue) value).getField().getName();
                            info.append("引用字段: ").append(fieldName).append("\n");
                        }

                        // 检查是否是HTTP请求相关对象，如果是则提取详细信息
                        String httpDetails = extractHttpRequestDetails(refInstance, className);
                        if (httpDetails != null) {
                            info.append("\n").append(httpDetails);
                        }

                        count++;
                    }
                }
            }

            if (info.length() > 0) {
                return info.toString();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    /**
     * 提取HTTP请求对象的详细信息
     */
    private String extractHttpRequestDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance, String className) {
        try {
            StringBuilder details = new StringBuilder();

            // Reactor Netty HttpClient
            if (className.contains("reactor.netty.http.client") ||
                className.contains("HttpClientConfig") ||
                className.contains("HttpClientRequest")) {
                details.append(extractReactorNettyDetails(instance));
            }
            // OkHttp Request
            else if (className.contains("okhttp3.Request") ||
                     className.contains("okhttp3.internal")) {
                details.append(extractOkHttpDetails(instance));
            }
            // Apache HttpClient
            else if (className.contains("org.apache.http.client.methods") ||
                     className.contains("HttpRequestBase")) {
                details.append(extractApacheHttpClientDetails(instance));
            }
            // Spring Request
            else if (className.contains("org.springframework.http") ||
                     className.contains("ClientRequest")) {
                details.append(extractSpringRequestDetails(instance));
            }
            // Tomcat/Catalina Request
            else if (className.contains("org.apache.catalina") ||
                     className.contains("org.apache.coyote.Request")) {
                details.append(extractTomcatRequestDetails(instance));
            }

            return details.length() > 0 ? details.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取Reactor Netty HTTP请求详情
     */
    private String extractReactorNettyDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        StringBuilder details = new StringBuilder();
        try {
            details.append("────────────────────────────────────────\n");
            details.append("HTTP请求详情 (Reactor Netty)\n");
            details.append("────────────────────────────────────────\n");

            // 尝试提取各种字段
            String[] fieldsToExtract = {
                "method", "uri", "uriStr", "path", "query",
                "version", "status", "request", "response"
            };

            for (String fieldName : fieldsToExtract) {
                try {
                    Object fieldValue = heapHolder.getFieldValue(instance, fieldName);
                    if (fieldValue != null) {
                        String valueStr = heapHolder.toString(fieldValue);
                        if (valueStr != null && !valueStr.isEmpty()) {
                            details.append(fieldName).append(": ").append(valueStr).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 尝试提取headers
            try {
                Object headers = instance.getValueOfField("headers");
                if (headers == null) {
                    headers = instance.getValueOfField("requestHeaders");
                }
                if (headers != null) {
                    details.append("\nHeaders:\n");
                    HashMap<String, String> headerMap = heapHolder.arrayDump(heapHolder.getMap(headers));
                    if (headerMap != null) {
                        for (Map.Entry<String, String> e : headerMap.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 尝试提取配置信息
            try {
                Object config = instance.getValueOfField("config");
                if (config != null) {
                    HashMap<String, String> configFields = heapHolder.getAllFieldValues(config);
                    if (configFields != null && !configFields.isEmpty()) {
                        details.append("\n配置信息:\n");
                        for (Map.Entry<String, String> e : configFields.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            details.append("提取失败: ").append(e.getMessage()).append("\n");
        }

        return details.toString();
    }

    /**
     * 提取OkHttp请求详情
     */
    private String extractOkHttpDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        StringBuilder details = new StringBuilder();
        try {
            details.append("────────────────────────────────────────\n");
            details.append("HTTP请求详情 (OkHttp)\n");
            details.append("────────────────────────────────────────\n");

            // Method
            String method = heapHolder.getFieldStringValue(instance, "method");
            if (method != null) {
                details.append("Method: ").append(method).append("\n");
            }

            // URL
            try {
                Object url = heapHolder.getFieldValue(instance, "url");
                if (url != null) {
                    details.append("URL: ").append(heapHolder.toString(url)).append("\n");
                }
            } catch (Exception ignored) {}

            // Headers
            try {
                Object headers = instance.getValueOfField("headers");
                if (headers != null) {
                    details.append("\nHeaders:\n");
                    String headersStr = heapHolder.toString(headers);
                    if (headersStr != null) {
                        details.append(headersStr).append("\n");
                    }
                }
            } catch (Exception ignored) {}

            // Body
            try {
                Object body = heapHolder.getFieldValue(instance, "body");
                if (body != null) {
                    details.append("\nBody: ").append(heapHolder.toString(body)).append("\n");
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            details.append("提取失败: ").append(e.getMessage()).append("\n");
        }

        return details.toString();
    }

    /**
     * 提取Apache HttpClient请求详情
     */
    private String extractApacheHttpClientDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        StringBuilder details = new StringBuilder();
        try {
            details.append("────────────────────────────────────────\n");
            details.append("HTTP请求详情 (Apache HttpClient)\n");
            details.append("────────────────────────────────────────\n");

            // Method
            String method = heapHolder.getFieldStringValue(instance, "method");
            if (method != null) {
                details.append("Method: ").append(method).append("\n");
            }

            // URI
            try {
                Object uri = heapHolder.getFieldValue(instance, "uri");
                if (uri != null) {
                    details.append("URI: ").append(heapHolder.toString(uri)).append("\n");
                }
            } catch (Exception ignored) {}

            // Protocol version
            try {
                Object protocol = instance.getValueOfField("protocol");
                if (protocol != null) {
                    details.append("Protocol: ").append(heapHolder.toString(protocol)).append("\n");
                }
            } catch (Exception ignored) {}

            // Headers
            try {
                Object headerGroup = heapHolder.getFieldValue(instance, "headergroup");
                if (headerGroup != null) {
                    details.append("\nHeaders:\n");
                    details.append(heapHolder.toString(headerGroup)).append("\n");
                }
            } catch (Exception ignored) {}

            // Config
            try {
                Object config = heapHolder.getFieldValue(instance, "config");
                if (config != null) {
                    details.append("\n配置信息:\n");
                    HashMap<String, String> configFields = heapHolder.getAllFieldValues(config);
                    if (configFields != null) {
                        for (Map.Entry<String, String> e : configFields.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            details.append("提取失败: ").append(e.getMessage()).append("\n");
        }

        return details.toString();
    }

    /**
     * 提取Spring请求详情
     */
    private String extractSpringRequestDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        StringBuilder details = new StringBuilder();
        try {
            details.append("────────────────────────────────────────\n");
            details.append("HTTP请求详情 (Spring)\n");
            details.append("────────────────────────────────────────\n");

            // Method
            try {
                Object method = instance.getValueOfField("method");
                if (method != null) {
                    details.append("Method: ").append(method.toString()).append("\n");
                }
            } catch (Exception ignored) {}

            // URL/URI
            try {
                Object url = instance.getValueOfField("url");
                if (url != null) {
                    details.append("URL: ").append(heapHolder.toString(url)).append("\n");
                }
            } catch (Exception ignored) {}

            // Headers
            try {
                Object headers = instance.getValueOfField("headers");
                if (headers != null) {
                    details.append("\nHeaders:\n");
                    HashMap<String, String> headerMap = heapHolder.getAllFieldValues(headers);
                    if (headerMap != null) {
                        for (Map.Entry<String, String> e : headerMap.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Body
            try {
                Object body = instance.getValueOfField("body");
                if (body != null) {
                    details.append("\nBody:\n");
                    details.append(heapHolder.toString(body)).append("\n");
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            details.append("提取失败: ").append(e.getMessage()).append("\n");
        }

        return details.toString();
    }

    /**
     * 提取Tomcat请求详情
     */
    private String extractTomcatRequestDetails(org.graalvm.visualvm.lib.jfluid.heap.Instance instance) {
        StringBuilder details = new StringBuilder();
        try {
            details.append("────────────────────────────────────────\n");
            details.append("HTTP请求详情 (Tomcat)\n");
            details.append("────────────────────────────────────────\n");

            String[] basicFields = {"requestURI", "queryString", "method",
                                  "protocol", "serverName", "remoteAddr",
                                  "scheme", "localAddr", "localPort"};

            for (String fn : basicFields) {
                String val = heapHolder.getFieldStringValue(instance, fn);
                if (val != null && !val.isEmpty()) {
                    details.append(fn).append(": ").append(val).append("\n");
                }
            }

            // Headers
            try {
                Object headers = heapHolder.getFieldValue(instance, "headers");
                if (headers != null) {
                    HashMap<String, String> headerMap = heapHolder.arrayDump(heapHolder.getMap(headers));
                    if (headerMap != null && !headerMap.isEmpty()) {
                        details.append("\nHeaders:\n");
                        for (Map.Entry<String, String> e : headerMap.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Parameters
            try {
                Object parameters = heapHolder.getFieldValue(instance, "parameters");
                if (parameters != null) {
                    HashMap<String, String> paramMap = heapHolder.arrayDump(heapHolder.getMap(parameters));
                    if (paramMap != null && !paramMap.isEmpty()) {
                        details.append("\nParameters:\n");
                        for (Map.Entry<String, String> e : paramMap.entrySet()) {
                            details.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Cookies
            try {
                Object cookies = heapHolder.getFieldValue(instance, "cookies");
                if (cookies != null) {
                    details.append("\nCookies: ").append(heapHolder.toString(cookies)).append("\n");
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            details.append("提取失败: ").append(e.getMessage()).append("\n");
        }

        return details.toString();
    }

    private void displayResults(List<SearchResult> results) {
        searchResults.clear();
        searchResults.addAll(results);
        tableModel.setRowCount(0);

        for (SearchResult result : results) {
            tableModel.addRow(new Object[]{result.matchedText, result.getDisplayContext()});
        }
    }

    private void showContextInDetailPanel(javax.swing.event.ListSelectionEvent e) {
        int row = resultsTable.getSelectedRow();
        if (row >= 0 && row < searchResults.size()) {
            SearchResult result = searchResults.get(row);
            String detail = buildDetailText(result);
            // Find the main GUI and show in detail panel
            Container parent = getParent();
            while (parent != null && !(parent instanceof HeapDumpAnalyzerGUI)) {
                parent = parent.getParent();
            }
            if (parent instanceof HeapDumpAnalyzerGUI) {
                HeapDumpAnalyzerGUI gui = (HeapDumpAnalyzerGUI) parent;
                gui.showSearchResultDetail("Custom Search Result", detail);
            }
        }
    }

    private String buildDetailText(SearchResult result) {
        StringBuilder sb = new StringBuilder();

        // 匹配文本
        sb.append("════════════════════════════════════════\n");
        sb.append("匹配内容 (Matched Text)\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(result.matchedText).append("\n\n");

        // 对象信息
        if (result.className != null || result.objectId != null) {
            sb.append("════════════════════════════════════════\n");
            sb.append("对象信息 (Object Information)\n");
            sb.append("════════════════════════════════════════\n");
            if (result.className != null) {
                sb.append("类名: ").append(result.className).append("\n");
            }
            if (result.objectId != null) {
                sb.append("对象ID: ").append(result.objectId).append("\n");
            }
            sb.append("\n");
        }

        // 完整上下文（5000字符）
        sb.append("════════════════════════════════════════\n");
        sb.append("上下文 (Context - ").append(result.fullString.length()).append(" chars total)\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(result.before).append("【").append(result.matchedText).append("】").append(result.after).append("\n\n");

        // 完整字符串
        sb.append("════════════════════════════════════════\n");
        sb.append("完整字符串 (Full String)\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(result.fullString).append("\n\n");

        // 引用流程和相关节点（已自动展开HTTP请求详情）
        if (result.referrerInfo != null) {
            sb.append(result.referrerInfo).append("\n");
        }

        // 如果没有引用信息，显示提示
        if (result.referrerInfo == null && result.instance == null) {
            sb.append("════════════════════════════════════════\n");
            sb.append("引用流程 (Reference Flow)\n");
            sb.append("════════════════════════════════════════\n");
            sb.append("未找到对象引用信息\n");
            sb.append("(可能来自原始内存扫描，非Java堆对象)\n\n");
        }

        return sb.toString();
    }

    private void exportResults() {
        if (searchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No results to export",
                "Export",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Search Results");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text File", "txt"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("cURL Commands", "sh"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = buildExportContent(file.getName());
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                statusLabel.setText("Exported to: " + file.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Export failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String buildExportContent(String fileName) {
        String ext = getFileExtension(fileName);
        switch (ext.toLowerCase()) {
            case "csv":
                return buildCsvContent();
            case "json":
                return buildJsonContent();
            case "sh":
                return buildCurlContent();
            default:
                return buildTextContent();
        }
    }

    private String buildTextContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("Custom Search Results\n");
        sb.append("Search String: ").append(searchField.getText()).append("\n");
        sb.append("Total Matches: ").append(searchResults.size()).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = searchResults.get(i);
            sb.append("Match #").append(i + 1).append(":\n");
            sb.append("  Matched: ").append(result.matchedText).append("\n");

            // 对象信息
            if (result.className != null || result.objectId != null) {
                sb.append("  Object Info:\n");
                if (result.className != null) {
                    sb.append("    Class: ").append(result.className).append("\n");
                }
                if (result.objectId != null) {
                    sb.append("    Object ID: ").append(result.objectId).append("\n");
                }
            }

            sb.append("  Context: ").append(result.getDisplayContext()).append("\n");
            sb.append("  Full String: ").append(result.fullString).append("\n");

            // 引用信息
            if (result.referrerInfo != null) {
                sb.append("  ").append(result.referrerInfo.replace("\n", "\n  ")).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildCsvContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("Match,Class,ObjectID,Context,Full String,Reference Info\n");

        for (SearchResult result : searchResults) {
            sb.append(escapeCsv(result.matchedText)).append(",");
            sb.append(escapeCsv(result.className != null ? result.className : "")).append(",");
            sb.append(result.objectId != null ? result.objectId.toString() : "").append(",");
            sb.append(escapeCsv(result.getDisplayContext())).append(",");
            sb.append("\"").append(result.fullString.replace("\"", "\"\"")).append("\",");

            // 引用信息
            if (result.referrerInfo != null) {
                sb.append("\"").append(result.referrerInfo.replace("\"", "\"\"")).append("\"");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * 构建JSON格式的导出内容
     */
    private String buildJsonContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"searchQuery\": \"").append(escapeJson(searchField.getText())).append("\",\n");
        sb.append("  \"totalMatches\": ").append(searchResults.size()).append(",\n");
        sb.append("  \"results\": [\n");

        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = searchResults.get(i);
            if (i > 0) sb.append(",\n");

            sb.append("    {\n");
            sb.append("      \"matchedText\": \"").append(escapeJson(result.matchedText)).append("\",\n");

            if (result.className != null) {
                sb.append("      \"className\": \"").append(escapeJson(result.className)).append("\",\n");
            }
            if (result.objectId != null) {
                sb.append("      \"objectId\": ").append(result.objectId).append(",\n");
            }

            sb.append("      \"context\": \"").append(escapeJson(result.getDisplayContext())).append("\",\n");
            sb.append("      \"fullString\": \"").append(escapeJson(result.fullString)).append("\"");

            if (result.referrerInfo != null) {
                sb.append(",\n      \"referenceInfo\": \"").append(escapeJson(result.referrerInfo)).append("\"");
            }

            sb.append("\n    }");
        }

        sb.append("\n  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 构建cURL命令格式的导出内容（针对HTTP请求）
     */
    private String buildCurlContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("# Exported cURL Commands from HeapDump Analyzer\n");
        sb.append("# Total: ").append(searchResults.size()).append(" entries\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = searchResults.get(i);
            sb.append("# Entry ").append(i + 1).append("\n");

            String fullString = result.fullString;

            // 尝试解析为HTTP请求并生成curl命令
            if (isHttpRequestData(fullString)) {
                sb.append(convertToCurl(fullString));
            } else {
                // 如果不是HTTP请求数据，直接输出字符串
                sb.append("# Data: ").append(escapeForShell(fullString.substring(0, Math.min(100, fullString.length()))));
                if (fullString.length() > 100) sb.append("...");
                sb.append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 判断字符串是否包含HTTP请求数据
     */
    private boolean isHttpRequestData(String str) {
        if (str == null) return false;
        String lower = str.toLowerCase();
        return lower.contains("http://") || lower.contains("https://") ||
               lower.contains("get ") || lower.contains("post ") ||
               lower.contains("authorization:") || lower.contains("content-type:");
    }

    /**
     * 将数据转换为cURL命令（简单实现）
     */
    private String convertToCurl(String data) {
        StringBuilder curl = new StringBuilder();

        // 尝试提取URL
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
            "(https?://[^\s\"']+)"
        );
        java.util.regex.Matcher urlMatcher = urlPattern.matcher(data);

        String url = null;
        if (urlMatcher.find()) {
            url = urlMatcher.group(1);
        }

        if (url != null) {
            curl.append("curl -X GET '").append(url).append("'");

            // 尝试提取headers
            java.util.regex.Pattern headerPattern = java.util.regex.Pattern.compile(
                "(\\w+[\\w-]*):\\s*([^\n]+)"
            );
            java.util.regex.Matcher headerMatcher = headerPattern.matcher(data);

            while (headerMatcher.find()) {
                String headerName = headerMatcher.group(1);
                String headerValue = headerMatcher.group(2).trim();

                // 跳过一些不需要的header
                if (headerName.equalsIgnoreCase("host") ||
                    headerName.equalsIgnoreCase("content-length")) {
                    continue;
                }

                curl.append(" \\\n  -H '").append(headerName).append(": ").append(headerValue).append("'");
            }

            curl.append("\n");
        } else {
            // 无法解析为curl命令，直接输出数据
            curl.append("# Unable to convert to curl command\n");
            curl.append("# Data: ").append(escapeForShell(data.substring(0, Math.min(200, data.length()))));
            if (data.length() > 200) curl.append("...");
            curl.append("\n");
        }

        return curl.toString();
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 转义Shell字符串
     */
    private String escapeForShell(String value) {
        if (value == null) return "";
        return value.replace("'", "'\\''");
    }

    /**
     * 快速搜索HTTP请求
     */
    private void searchHttpRequests() {
        // 搜索常见的HTTP请求模式
        String[] patterns = {
            "http://", "https://",  // URL
            "GET ", "POST ", "PUT ", "DELETE ", "PATCH ",  // HTTP方法
            "application/json", "application/xml",  // Content-Type
            "Authorization:", "Cookie:", "X-",  // Headers
            "Bearer ", "Basic ",  // Auth类型
            "\"url\":", "\"uri\":", "\"endpoint\":",  // JSON中的URL字段
            "requestURI", "queryString",  // Java字段名
            "accessToken", "refreshToken", "api_key", "apikey"  // 常见字段名
        };

        StringBuilder regexPattern = new StringBuilder();
        for (int i = 0; i < patterns.length; i++) {
            if (i > 0) regexPattern.append("|");
            regexPattern.append(Pattern.quote(patterns[i]));
        }

        searchField.setText(regexPattern.toString());
        regexCheckbox.setSelected(true);
        caseSensitiveCheckbox.setSelected(false);
        performSearch();
    }

    /**
     * 快速搜索JSON数据
     */
    private void searchJsonData() {
        // 搜索JSON格式特征
        searchField.setText("^\\s*[\\{\\[].*[\\}\\]]\\s*$");
        regexCheckbox.setSelected(true);
        caseSensitiveCheckbox.setSelected(false);
        performSearch();
    }

    /**
     * 快速搜索URL模式
     */
    private void searchUrlPatterns() {
        // 搜索各种URL格式
        searchField.setText("https?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*");
        regexCheckbox.setSelected(true);
        caseSensitiveCheckbox.setSelected(false);
        performSearch();
    }

    /**
     * 快速搜索API端点
     */
    private void searchApiEndpoints() {
        // 搜索常见的API端点路径
        String[] apiPatterns = {
            "/api/", "/v1/", "/v2/", "/v3/",  // API版本
            "/graphql", "/rest/", "/rpc/",  // API类型
            "/login", "/logout", "/auth", "/token",  // 认证端点
            "/users", "/admin", "/config", "/settings",  // 资源端点
            "endpoint", "baseUrl", "apiUrl", "apiEndpoint"  // 配置字段名
        };

        StringBuilder regexPattern = new StringBuilder();
        for (int i = 0; i < apiPatterns.length; i++) {
            if (i > 0) regexPattern.append("|");
            regexPattern.append(Pattern.quote(apiPatterns[i]));
        }

        searchField.setText(regexPattern.toString());
        regexCheckbox.setSelected(true);
        caseSensitiveCheckbox.setSelected(false);
        performSearch();
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "txt";
    }

    private static class SearchResult {
        final String matchedText;
        final String before;
        final String after;
        final String context;
        final String fullString;
        final Object instance;  // 对象实例
        final String className;  // 类名
        final Long objectId;     // 对象ID
        final String referencePath;  // 引用路径
        final String referrerInfo;    // 引用者信息

        SearchResult(String matchedText, String before, String after, String context, String fullString) {
            this(matchedText, before, after, context, fullString, null, null, null, null, null);
        }

        SearchResult(String matchedText, String before, String after, String context, String fullString,
                     Object instance, String className, Long objectId, String referencePath, String referrerInfo) {
            this.matchedText = matchedText;
            this.before = before;
            this.after = after;
            this.context = context;
            this.fullString = fullString;
            this.instance = instance;
            this.className = className;
            this.objectId = objectId;
            this.referencePath = referencePath;
            this.referrerInfo = referrerInfo;
        }

        String getDisplayContext() {
            return before + "【" + matchedText + "】" + after;
        }
    }
}
