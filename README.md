<div align="center">

<img src="docs/assets/logo.svg" alt="HeapDump Analyzer" width="120"/>
<h1 align="center">HeapDump Analyzer</h1>

<p align="center">
  <strong>Dig secrets out of JVM memory — extract credentials, tokens, and sensitive data directly from Java heap dumps</strong>
</p>

<p align="center">
  <a href="https://github.com/wanghw/heapdump-analyzer/stargazers">
    <img src="https://img.shields.io/github/stars/wanghw/heapdump-analyzer?style=for-the-badge&logo=github&logoColor=white&color=yellow" alt="Stars"/>
  </a>
  <a href="https://github.com/wanghw/heapdump-analyzer/network/members">
    <img src="https://img.shields.io/github/forks/wanghw/heapdump-analyzer?style=for-the-badge&logo=github&logoColor=white&color=blue" alt="Forks"/>
  </a>
  <a href="https://github.com/wanghw/heapdump-analyzer/issues">
    <img src="https://img.shields.io/github/issues/wanghw/heapdump-analyzer?style=for-the-badge&logo=github&logoColor=white&color=red" alt="Issues"/>
  </a>
  <a href="https://github.com/wanghw/heapdump-analyzer/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/wanghw/heapdump-analyzer?style=for-the-badge&logo=apache&logoColor=white&color=green" alt="License"/>
  </a>
</p>

<p align="center">
  <a href="https://openjdk.org/">
    <img src="https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java"/>
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Spiders-94-brightgreen?style=flat-square" alt="Spiders"/>
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/YAML%20Rules-65-yellow?style=flat-square" alt="Rules"/>
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Web%20UI-%E2%9C%94-blue?style=flat-square" alt="Web UI"/>
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/GUI-%E2%9C%94-purple?style=flat-square" alt="GUI"/>
  </a>
  <a href="https://github.com/wanghw/heapdump-analyzer/pulls">
    <img src="https://img.shields.io/badge/PRs-welcome-pink?style=flat-square" alt="PRs Welcome"/>
  </a>
</p>

<p align="center">
  <a href="#quickstart">Quickstart</a> •
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#comparison">Comparison</a> •
  <a href="README_zh.md">中文文档</a> •
  <a href="docs/cases/">Cases</a>
</p>

</div>

---

## Why HeapDump Analyzer?

When a Java application hits an OOM, a `jmap` dump, or a JMX/JFR trigger, a `.hprof` heap dump lands on disk. Conventional tools (Eclipse MAT, JIFA) help you **diagnose memory leaks**. HeapDump Analyzer answers a different question:

> **What secrets is this JVM holding in memory right now?**

Database passwords, Redis auth tokens, AWS keys, JWT signing keys, Shiro keys, Nacos credentials, OAuth client secrets, cloud service tokens — all of these live as **plaintext fields** inside heap objects. HeapDump Analyzer enumerates them with 94 purpose-built Spiders and a 65-rule YAML engine, then optionally verifies whether the credentials are still **LIVE** against the cloud provider's API (à la TruffleHog).

---

## Features

### 🔥 Breakthrough Credential Extraction

- **94 Spider plugins** — Far beyond comparable tools, covering 10 categories:
  - 🔑 **Cloud**: AWS, GCP, Azure, Aliyun, Tencent, Huawei, K8s, Docker Registry
  - 💾 **Database**: HikariCP, Druid, MyBatis, ClickHouse, HBase, Neo4j, InfluxDB
  - 🚀 **Cache**: Redis (Lettuce/Jedis), Memcached
  - 🛡️ **Auth**: Shiro, Spring Security, SA-Token, JWT, OAuth2
  - ⚙️ **Config Center**: Nacos, Apollo, Spring Cloud Config, Dubbo, ZooKeeper
  - 📨 **Message Queue**: Kafka, RocketMQ, RabbitMQ, ActiveMQ, Pulsar
  - 🌐 **HTTP Client**: OkHttp interceptors, RestTemplate, Apache HttpClient, Feign
  - 🏗️ **Microservice Frameworks**: RuoYi, JeecgBoot, Eladmin, Pig, SpringBlade
  - 🔐 **Credential Search**: TokenSearch, SessionSearch, CookieThief
  - 📊 **Dependency Version**: Maven/Gradle dependency scanning

### 🎨 Multiple Interfaces

- **🌐 Web UI** — Browser dashboard with dark/light themes, real-time charts, filters
- **🖥️ Swing GUI** — Modern desktop interface (FlatLaf theme), drag-and-drop, live preview
- **⚡ CLI** — Command-line mode for automation and CI/CD integration
- **🔍 REPL** — Interactive OQL exploration (jshell-like heap browsing)
- **📦 Batch Scan** — Scan all heap dumps in a directory in one go

### 🔐 Live Credential Validation (TruffleHog-style)

| Mode | Function | Network? |
|---|---|:---:|
| `--validate` | Offline format check (e.g., AKIA prefix, key checksum) | ❌ |
| `--validate-live` | Calls provider API, tags each credential **LIVE/EXPIRED/UNKNOWN** | ✅ |

Supported live validators: **AWS**, **GitHub**, **Stripe**, **Slack**, **Telegram**  
Offline format validators: Aliyun, GCP, Twilio, SendGrid, Firebase, JWK

> ⚠️ **Warning**: `--validate-live` makes real outbound API calls. AWS GuardDuty and similar cloud threat-detection systems may flag these calls. Only enable on assets you own and are authorized to test.

### 📊 YAML Rule Engine — Extend Without Recompiling

```yaml
# ~/.heapdump-analyzer/rules/my-token.yml
kind: RegexRule
metadata:
  id: my-team-token
  name: Internal Team Token
  category: auth
  severity: CRITICAL
  description: Detect internal "team-xxxx" bearer tokens
spec:
  pattern: 'team-[A-Za-z0-9]{40}'
  validator: GitHubTokenValidator  # optional
```

**Supported rule kinds**:
- `RegexRule` — Scan all string pools
- `ClassRule` — Extract fields from named classes

**65 built-in rules** covering:
- ☁️ Cloud keys (AWS, Azure, GCP, Aliyun, Tencent, etc.)
- 🔑 Auth tokens (GitHub, GitLab, Slack, Stripe, etc.)
- 💾 Database connections (MySQL, PostgreSQL, MongoDB, etc.)
- 📱 PII (email, phone, ID card, etc.)
- 🔒 Password/crypto fields (BCrypt, plaintext passwords, etc.)

### 🚀 Performance Optimizations

- **Parallel scan** — `--parallel --threads N` (default: CPU cores)
- **Batch scan** — `--batch <dir>` scan entire directory
- **Raw memory scan** — `--extract-raw` (like `strings` command)
- **Category filter** — `--category <category>` scan specific types only

### 📄 Multiple Output Formats

- **HTML report** — Single self-contained file with severity stats, category charts, filterable table
- **JSON** — Structured data for API integration
- **CSV** — Table format for data analysis
- **Text** — Traditional CLI output

---

## Screenshots

### Web UI — Dark Dashboard

<img src="docs/assets/screenshots/web-dashboard-dark.png" alt="Web UI dark dashboard" width="880"/>

Default landing view: severity cards at top, category & spider charts, filterable results table.

### Web UI — Light Dashboard

<img src="docs/assets/screenshots/web-dashboard-light.png" alt="Web UI light dashboard" width="880"/>

Same dashboard with light theme toggled from top-right ☀️ button — for daytime readability and screen-sharing.

### Swing GUI — Modern Desktop Interface

<img src="docs/assets/screenshots/ScreenShot_2026-07-07_194206_723.png" alt="Swing GUI" width="880"/>

FlatLaf theme, left-right split layout, live preview of details.

---

## Quickstart (3 steps)

```bash
# 1. Build
./start.sh build

# 2. Scan a heap dump — text output to stdout
./start.sh cli /path/to/heapdump.hprof

# 3. Or generate a shareable HTML report
java -jar target/heapdump-analyzer.jar heapdump.hprof --format html -o report.html
```

**Launch the Web UI browser dashboard**:

```bash
./start.sh web  # http://localhost:9090
```

**Launch the desktop GUI**:

```bash
./start.sh desktop
```

---

## Comparison

| Feature | **HeapDump Analyzer** | JDumpSpider | heapdump_tool | Eclipse MAT |
|---|:---:|:---:|:---:|:---:|
| JDK compatibility | 8/11/17/21 + GraalVM | 1.8 only | any | any |
| Spider plugins | **94** (10 categories) | ~20 | keyword scan | — |
| Extensible rule engine | ✅ YAML + Java | ❌ hardcoded | ❌ | N/A |
| Live credential validation | ✅ LIVE/EXPIRED/UNKNOWN | ❌ | ❌ | N/A |
| HTML report export | ✅ self-contained | ❌ | ❌ | ❌ |
| Web UI (browser dashboard) | ✅ | ❌ | ❌ | ❌ |
| Desktop GUI | ✅ Swing (FlatLaf) | ❌ | ❌ | ✅ RCP |
| REPL (OQL exploration) | ✅ | ❌ | ✅ | ❌ |
| Parallel scanning | ✅ `--parallel --threads N` | ❌ | ❌ | ❌ |
| Batch scanning | ✅ `--batch <dir>` | ❌ | ❌ | ❌ |
| Maintenance status | **Active** | stalled | active | active |
| License | Apache-2.0 | Apache-2.0 | — | EPL |

---

## CLI Reference

```text
heapdump-analyzer <heapfile> [options]

Output:
  -f, --format <text|json|csv|html>   Output format (default: text)
  -o, --output <file>                 Output file (default: stdout)

Scanning:
  -s, --spider <name,name|all>        Run specific spiders
      --severity <CRITICAL|HIGH|MEDIUM|LOW|INFO>  Min severity (default: INFO)
      --category <category>           Category filter: credential, pii, session, etc.
      --parallel                      Enable parallel scanning
      --threads <N>                   Thread count (default: CPU cores)
      --batch <dir>                   Scan every heap dump in a directory

Rule engine:
      --rules <dir>                   Load extra YAML rules from a directory
      --rules-only                    Run only the rule engine, skip Spiders
      --list-rules                    List all rules and exit
      --validate                      Offline format validation (no network calls)
      --validate-live                 ONLINE validation (calls cloud APIs!)
                                      WARNING: may trigger cloud alerts. Off by default.

Discovery:
  -l, --list                          List all spiders and exit
      --extract <regex>               Dump all strings matching a regex
      --extract-raw <regex>           Raw memory scan (like `strings` command)

Other:
      --swing                         Launch Swing GUI (default)
      --web                           Launch the Web UI server
      --port <N>                      Web UI port (default: 9090)
      --repl                          Launch interactive REPL
  -h, --help                          Show help
```

---

## Use Cases

### 1. Security Audit & Penetration Testing

```bash
# Scan production heap dump, discover exposed credentials
java -jar heapdump-analyzer.jar prod.hprof --severity CRITICAL --format html -o audit-report.html
```

### 2. Incident Response & Credential Rotation

```bash
# Validate whether leaked credentials are still active
java -jar heapdump-analyzer.jar leaked.hprof --validate-live --severity HIGH -o live-credentials.txt
```

### 3. CI/CD Integration

```bash
# Automated scan as security gate
./start.sh cli artifact.hprof --format json --severity HIGH --rules-only
```

### 4. Batch Historical Analysis

```bash
# Scan all heap dumps from the past week
./start.sh batch /var/log/heapdumps/ --format html
```

---

## Real-World Case Study

See the full case study: [Extracting 50+ Cloud Credentials from a Single Heap Dump](docs/cases/extract-50-cloud-credentials.md)

**Highlights**:
- Extracted **52 distinct credential surfaces** from a single 1.4 GB Spring Boot gateway heap dump
- 38 CRITICAL-level credentials, 24 HIGH-level
- Found 8 **LIVE credentials** through real-time validation, immediately rotated
- Covered: Cloud IAM (12), Auth (11), Databases (9), Message Queues (5), Config Centers (7)

---

## Architecture

### Spider Plugin System

Java SPI (ServiceLoader) based pluggable architecture:

```java
public interface ISpider {
    String getName();
    String getCategory();
    String getDescription();
    Severity getSeverity();
    String sniff(IHeapHolder heapHolder);
}
```

**Implementation strategies**:
- Direct field extraction from JVM class instances (e.g., `BasicAWSCredentials`)
- String pool scanning (regex pattern matching)
- Hybrid approach (class fields + string pool)

### YAML Rule Engine

```java
public class RuleEngine {
    private List<Rule> rules;
    private boolean validateEnabled;
    private boolean validateLiveEnabled;
    private boolean parallelEnabled;

    public List<RuleResult> execute(IHeapHolder heapHolder);
    public List<EnhancedResult> executeEnhanced(IHeapHolder heapHolder);
}
```

**Rule types**:
- `RegexRule` — Regex scan of string pools
- `ClassRule` — Direct field extraction from class instances

### Sensitivity Category System

```java
public enum SensitivityCategory {
    CREDENTIAL("🔑", "Credential", "#f38ba8"),    // passwords, keys, tokens
    PII("📱", "Personal Info", "#fab387"),         // phone, email, ID
    SESSION("🍪", "Session Data", "#f9e2af"),     // cookie, session
    NETWORK("🌐", "Network Info", "#89dceb"),     // IP, URL
    DATABASE("💾", "Database", "#cba6f7"),         // connection strings
    CLOUD("☁️", "Cloud Service", "#f5c2e7"),      // AWS/Azure/GCP keys
    CONFIG("⚙️", "Config Info", "#94e2d5");       // config sensitive items
}
```

### Heap Dump Parsers

- **GraalVM VisualVM Heap Library** — JDK 9+ HPROF support
- **NetBeans Profiler Heap Library** — JDK 8 compatibility
- **Auto version detection** — Select parser based on file format & Java class version

---

## Contributing

We **deliberately made contribution easy**. The fastest way to help is to **add a Spider or a YAML rule**:

### Add a New Spider (5 minutes)

```java
// src/main/java/cn/wanghw/spider/MyCloudCredentialSearch.java
public class MyCloudCredentialSearch implements ISpider {
    public String getName() { return "MyCloudCredentialSearch"; }
    public String getCategory() { return "cloud"; }
    public String getDescription() { return "Extract MyCloud credentials"; }
    public Severity getSeverity() { return Severity.CRITICAL; }

    public String sniff(IHeapHolder heapHolder) {
        Object clazz = heapHolder.findClass("com.mycloud.Credentials");
        // ... extraction logic
    }
}
```

Then add it to `src/main/resources/META-INF/services/cn.wanghw.ISpider`.

### Add a New YAML Rule (2 minutes)

```yaml
# src/main/resources/rules/cloud/mycloud-key.yml
kind: RegexRule
metadata:
  id: mycloud-api-key
  name: MyCloud API Key
  category: cloud
  severity: CRITICAL
  description: Detect MyCloud API keys
spec:
  pattern: 'MC-[A-Za-z0-9]{32}'
```

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for the full guide.

---

## Acknowledgements

HeapDump Analyzer stands on the shoulders of:

- [JDumpSpider](https://github.com/whwlsfb/JDumpSpider) — Original Spider concept & heap parsing
- [Eclipse MAT](https://github.com/eclipse-mat/mat) & [JIFA](https://github.com/eclipse/jifa) — Heap dump reference
- [TruffleHog](https://github.com/trufflesecurity/trufflehog) & [Gitleaks](https://github.com/gitleaks/gitleaks) — Credential validation inspiration
- GraalVM VisualVM & NetBeans Profiler Libraries — HPROF parsing

---

## Legal / Responsible Use

This tool reads plaintext secrets from heap dumps of JVMs you operate or are explicitly authorized to assess. It performs **no exploitation**. Credential live-validation only identifies whether a key is active — it does not exfiltrate data. Use it for **defensive auditing, incident response, and authorized security testing** only.

---

## License

Apache License 2.0 © wanghw and contributors.

---

<div align="center">

<p>
  <a href="README_zh.md">中文文档</a> •
  <a href=".github/CONTRIBUTING.md">Contributing</a> •
  <a href="docs/cases/">Cases</a> •
  <a href="https://github.com/wanghw/heapdump-analyzer/issues">Issues</a>
</p>

<p>
  If this project helps your security work, please consider giving it a ⭐️ Star!
</p>

**Made with ❤️ by the security community**

</div>