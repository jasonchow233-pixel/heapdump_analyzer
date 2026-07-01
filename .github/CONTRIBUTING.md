# Contributing to HeapDump Analyzer

Thanks for your interest in making HeapDump Analyzer better! The fastest, highest-impact way
to contribute is to **add a Spider or a YAML rule** for a framework or credential type you've
encountered in the wild. This guide gets you shipping in ~5 minutes.

## Prerequisites

- JDK 17 or later
- Maven 3.8+
- A heap dump to test against (any `.hprof`; a small one is fine)

## Build & verify

```bash
git clone https://github.com/wanghw/heapdump-analyzer.git
cd heapdump-analyzer
./start.sh build                       # mvn clean package -q -DskipTests
java -jar target/heapdump-analyzer.jar --list          # should print 94 spiders
java -jar target/heapdump-analyzer.jar --list-rules    # should print 42 rules
```

## 5-minute tutorial: add a YAML rule (no recompile)

Rules are the lowest-friction contribution. Drop a YAML file into the rules directory and
you're done.

1. Create `~/.heapdump-analyzer/rules/my-rule.yml`:

   ```yaml
   kind: RegexRule
   metadata:
     id: my-company-token
     name: Acme Internal Token
     category: auth
     severity: HIGH
     description: Detects Acme "acme_" prefixed bearer tokens
   spec:
     pattern: 'acme_[A-Za-z0-9]{32,}'
   ```

2. Run:

   ```bash
   java -jar target/heapdump-analyzer.jar heap.hprof --rules ~/.heapdump-analyzer/rules --rules-only
   ```

3. To ship it in the project, copy the file to
   `src/main/resources/rules/<category>/` and add its relative path to
   `src/main/resources/rules/rule-index.txt`.

Supported `kind` values:

- `RegexRule` — scans every string in the heap against `spec.pattern` (Java regex).
- `ClassRule` — locates a class by `spec.className` (with optional `alternateClassNames`)
  and extracts `spec.fields` (display-name → field-path map).

Optional `spec.validator` references a `cn.wanghw.rule.validator.<Name>` class for format
checking (offline) or live validation (online, see below).

## 10-minute tutorial: add a Spider (Java)

Spiders extract structured data from named JVM classes. The `AbstractClassFieldSpider`
base class removes most boilerplate.

1. Create `src/main/java/cn/wanghw/spider/AcmeConfig.java`:

   ```java
   package cn.wanghw.spider;

   import cn.wanghw.Severity;
   import java.util.HashMap;

   public class AcmeConfig extends AbstractClassFieldSpider {
       @Override public String getName()        { return "AcmeConfig"; }
       @Override public String getCategory()    { return "框架"; }
       @Override public Severity getSeverity()  { return Severity.HIGH; }
       @Override public String getDescription() { return "Acme platform connection config"; }

       @Override protected String getTargetClassName() { return "com.acme.platform.ConnectionConfig"; }

       @Override protected HashMap<String, String> getFieldList() {
           HashMap<String, String> m = new HashMap<>();
           m.put("Endpoint", "endpoint");
           m.put("Token",    "apiToken");
           m.put("Region",   "region");
           return m;
       }
   }
   ```

2. Register it by adding one line to
   `src/main/resources/META-INF/services/cn.wanghw.ISpider`:

   ```
   cn.wanghw.spider.AcmeConfig
   ```

3. Rebuild and test:

   ```bash
   ./start.sh build
   java -jar target/heapdump-analyzer.jar heap.hprof -s AcmeConfig
   ```

That's it. Severity levels map to colors in the Web UI and HTML report:

| Severity | Color   | Use for |
|---|---|---|
| `CRITICAL` | red    | cloud keys, DB passwords |
| `HIGH`     | orange | auth tokens, signing keys |
| `MEDIUM`   | yellow | internal config |
| `LOW`      | green  | version info, hints |
| `INFO`     | blue   | metadata |

## Adding a credential validator

1. Implement `cn.wanghw.rule.Validator` (offline format check) and optionally
   `cn.wanghw.rule.LiveValidator` (online API call returning
   `CredentialStatus.LIVE / EXPIRED / UNKNOWN`).
2. Place it in `cn.wanghw.rule.validator`.
3. Reference it from a rule via `spec.validator: <ClassName>`.

> **Live validators must be off by default.** They are only invoked when the user passes
> `--validate-live`. Never call external APIs from a plain `validate()`.

## Screenshot / GIF checklist

When adding UI features, capture and drop into `docs/assets/`:

- `screenshot-web.png` — Web UI dashboard
- `screenshot-desktop.png` — JavaFX desktop
- `demo-cli.gif` — CLI scan of a sample heap
- `demo-web.gif` — Web UI load → scan → export

## Pull request checklist

- [ ] `./start.sh build` succeeds
- [ ] `--list` / `--list-rules` still work
- [ ] New Spider appears in the Web UI category list
- [ ] No new outbound network calls unless behind `--validate-live`
- [ ] Docs updated (README comparison table if a new capability was added)
- [ ] Commit message follows `feat: ...` / `fix: ...` / `docs: ...` convention

## Reporting issues

Use the issue templates in `.github/ISSUE_TEMPLATE/`. Include:

- Heap dump origin (JDK version, app framework, how the dump was taken)
- HeapDump Analyzer version (`java -jar ... --version`)
- Reproduction command
- Redacted snippet of the output

## Code of conduct

By participating you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

By contributing, you agree your contributions are licensed under the Apache License 2.0.
