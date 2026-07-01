<div align="center">

<img src="docs/assets/wordmark.svg" alt="HeapDump Analyzer" width="640"/>

# Dig secrets out of JVM memory.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-1.0.0-89b4fa.svg)](#)
[![Spiders](https://img.shields.io/badge/Spiders-94-a6e3a1.svg)](#spiders)
[![Rules](https://img.shields.io/badge/YAML%20Rules-65-fab387.svg)](#rule-engine)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-f38ba8.svg)](.github/CONTRIBUTING.md)

**HeapDump Analyzer** is a security-focused Java heap dump analysis tool that extracts
credentials, tokens, configuration and other sensitive data **directly from JVM memory** —
the runtime plaintext that never touches disk.

It combines **heap dump parsing**, **94 Spider plugins**, an **extensible YAML rule engine**,
**live credential validation**, a **browser Web UI**, a **JavaFX desktop GUI**, and **HTML report
export** into a single open-source tool — purpose-built for red teamers, penetration testers and
SREs who need to know exactly what secrets a running JVM is holding.

</div>

---

## Why?

When a Java application is hit by an OOM, a `jmap` dump, or a JMX/JFR trigger, a `.hprof`
heap dump lands on disk. Conventional tools (Eclipse MAT, JIFA) help you *diagnose memory
leaks*. **HeapDump Analyzer** answers a different question:

> *What secrets is this JVM holding in memory right now?*

Database passwords, Redis auth tokens, AWS keys, JWT signing keys, Shiro keys, Nacos
credentials, OAuth client secrets, cloud service tokens — all of these live as plaintext
fields inside heap objects. HeapDump Analyzer enumerates them with 94 purpose-built Spiders
and a 65-rule YAML engine, then optionally verifies whether the credentials are still
**LIVE** against the cloud provider's API (à la TruffleHog).

## Comparison

| Feature | **HeapDump Analyzer** | JDumpSpider | heapdump_tool | Eclipse MAT |
|---|:---:|:---:|:---:|:---:|
| JDK compatibility | 8 / 11 / 17 / 21 + GraalVM | 1.8 only | any | any |
| Spider plugins | **94** (10 categories) | ~20 | keyword scan | — |
| Extensible rule engine | ✅ YAML + Java | ❌ hardcoded | ❌ | N/A |
| Live credential validation | ✅ LIVE / EXPIRED / UNKNOWN | ❌ | ❌ | N/A |
| HTML report export | ✅ self-contained | ❌ | ❌ | ❌ |
| Web UI (browser dashboard) | ✅ | ❌ | ❌ | ❌ |
| Desktop GUI | ✅ JavaFX | ❌ | ❌ | ✅ RCP |
| REPL (OQL exploration) | ✅ | ❌ | ✅ | ❌ |
| Parallel scanning | ✅ `--parallel --threads N` | ❌ | ❌ | ❌ |
| Batch scanning | ✅ `--batch <dir>` | ❌ | ❌ | ❌ |
| Maintenance status | **Active** | stalled | active | active |
| License | Apache-2.0 | Apache-2.0 | — | EPL |

## Quickstart (3 steps)

```bash
# 1. Build the fat jar (JDK 17+)
./start.sh build

# 2. Scan a heap dump — text to stdout
./start.sh cli /path/to/heapdump.hprof

# 3. Or get a shareable HTML report
java -jar target/heapdump-analyzer.jar /path/to/heapdump.hprof --format html -o report.html
```

Open the Web UI instead and click through the dashboard:

```bash
./start.sh web            # http://localhost:9090
```

## Modes

| Mode | Command | Use case |
|---|---|---|
| **Web UI** | `./start.sh web [port]` | Browser dashboard with severity cards, charts, filters & export |
| **Desktop GUI** | `./start.sh desktop` | JavaFX app for local analysis |
| **CLI** | `./start.sh cli <file>` | Headless / scripted scans |
| **Batch** | `./start.sh batch <dir>` | Scan a folder of heap dumps in one go |
| **REPL** | `./start.sh repl <file>` | Interactive OQL / heap exploration |

## CLI reference

```text
heapdump-analyzer <heapfile> [options]

Output:
  -f, --format <text|json|csv|html>   Output format (default: text)
  -o, --output <file>                 Output file (default: stdout)

Scanning:
  -s, --spider <name,name|all>        Run specific spiders
      --severity <CRITICAL|HIGH|MEDIUM|LOW|INFO>   Min severity (default: INFO)
      --parallel                      Enable parallel scanning
      --threads <N>                   Thread count (default: CPU cores)
      --batch <dir>                   Scan every heap dump in a directory

Rule engine:
      --rules <dir>                   Load extra YAML rules from a directory
      --rules-only                    Run only the rule engine, skip Spiders
      --list-rules                    List all rules and exit
      --validate                      Offline format validation of candidates
      --validate-live                 ONLINE validation (calls cloud APIs!)
                                      WARNING: may trigger cloud alerts. Off by default.

Discovery:
  -l, --list                          List all spiders and exit
      --extract <regex>               Dump all strings matching a regex

Other:
      --web                           Launch the Web UI server
      --port <N>                      Web UI port (default: 9090)
      --desktop / --gui               Launch the JavaFX desktop GUI
      --repl                          Launch the interactive REPL
  -h, --help                          Show help
```

## Spiders

94 Spiders across 10 categories extract structured data from named JVM classes:

| Category | Examples |
|---|---|
| Database | HikariCP, Druid, MyBatis, ClickHouse, HBase, Neo4j, InfluxDB |
| Cache | Redis (Lettuce/Jedis), Memcached |
| Auth | Shiro, Spring Security, SA-Token, CAS, PAC4J, JWT keys |
| Cloud | AWS, GCP, Azure, Aliyun, Huawei, Tencent, K8s SA, Docker registry |
| Config | Nacos, Apollo, Spring Cloud, Dubbo, Seata, ZooKeeper |
| Message Queue | Kafka, RocketMQ, RabbitMQ, ActiveMQ, Pulsar |
| Registry | Eureka, Nacos, Consul, ZooKeeper |
| Framework | RuoYi, JeecgBoot, Eladmin, Pig, SpringBlade, HuTool |
| HTTP | OkHttp interceptors, RestTemplate, Apache HttpClient, Feign |
| Credentials | TokenSearch, SessionSearch, AuthTokenSearch, CookieThief |

List them all with:

```bash
java -jar target/heapdump-analyzer.jar --list
```

## Rule engine

A YAML rule engine lets you add new secret patterns **without recompiling**. Rules are loaded
from the bundled `resources/rules/` and, automatically, from `~/.heapdump-analyzer/rules/`.

```yaml
# ~/.heapdump-analyzer/rules/my-team-token.yml
kind: RegexRule
metadata:
  id: my-team-token
  name: Internal Team Token
  category: auth
  severity: HIGH
  description: Detects internal "team-xxxx" bearer tokens
spec:
  pattern: 'team-[A-Za-z0-9]{40}'
  validator: GitHubTokenValidator   # optional
```

Supported rule kinds: `RegexRule` (scan all strings) and `ClassRule` (extract fields from a
named class). See [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md) for the
**5-minute "add a Spider / rule" tutorial**.

## Credential validation

Two tiers, off by default to avoid noise:

| Flag | What it does | Network? |
|---|---|:---:|
| `--validate` | Offline format & heuristic check (e.g. AKIA prefix, key checksum) | ❌ |
| `--validate-live` | Calls the provider API and tags each credential `LIVE` / `EXPIRED` / `UNKNOWN` | ✅ |

Supported live validators: **AWS**, **GitHub**, **Stripe**, **Slack**, **Telegram**,
plus offline format validators for Aliyun / GCP / Twilio / SendGrid / Firebase / JWK.

> ⚠️ **`--validate-live` makes real outbound API calls.** AWS GuardDuty and similar
> cloud threat-detection systems may flag these calls. Only enable it on assets you own
> and are authorized to test.

## HTML report

```bash
java -jar target/heapdump-analyzer.jar heap.hprof --format html -o report.html
```

Produces a **single, self-contained** HTML file — no external CDN, no missing images —
with severity statistics, category charts and a filterable detail table. Drop it into an
email, Slack, or a ticket and anyone can read it offline.

## Configuration

| Location | Purpose |
|---|---|
| `~/.heapdump-analyzer/rules/` | Drop custom YAML rules here; auto-loaded |
| `--rules <dir>` | Point at an arbitrary rules directory instead |

## Screenshots

### Web UI — dark dashboard

The default landing view: severity cards up top, category & top-spider charts, and a
filterable spider results table. Dark theme is the default.

<img src="docs/assets/screenshots/web-dashboard-dark.png" alt="Web UI dark dashboard" width="880"/>

### Web UI — light dashboard

Same dashboard with the light theme toggled from the top-right ☀️ button — for daytime
readability and screen-sharing on bright projectors.

<img src="docs/assets/screenshots/web-dashboard-light.png" alt="Web UI light dashboard" width="880"/>

### Web UI — spider results

Per-finding detail: each row shows spider name, category, severity badge and the raw
plaintext data extracted from the heap (collapsible `<pre>` with horizontal severity
filter and free-text search).

<img src="docs/assets/screenshots/web-spider-results.png" alt="Web UI spider results table" width="880"/>

### Web UI — rule engine

The **Rule engine** tab lists every YAML rule match — rule name, category, severity, and
each individual match string (with offline-validated and live-validation results inline).

<img src="docs/assets/screenshots/web-rules-dark.png" alt="Web UI rule engine tab" width="880"/>

> Screenshots were captured on a sample heap dump with display-layer redaction applied
> for documentation only. Real scans show the plaintext secrets — that's the point.

## Contributing

Contributions are welcome — and deliberately easy. The fastest way to help is to **add a
Spider or a YAML rule** for a framework/credential you've encountered. See
[`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md) for the 5-minute tutorial, the local
build (`./start.sh build`) and the PR checklist.

## Acknowledgements

HeapDump Analyzer stands on the shoulders of:

- [JDumpSpider](https://github.com/whwlsfb/JDumpSpider) — original Spider concept & heap parsing
- [Eclipse MAT](https://github.com/eclipse-mat/mat) & [JIFA](https://github.com/eclipse/jifa) — heap dump reference
- [TruffleHog](https://github.com/trufflesecurity/trufflehog) & [Gitleaks](https://github.com/gitleaks/gitleaks) — credential validation inspiration
- GraalVM VisualVM & NetBeans profiler libraries — HPROF parsing

## License

[Apache License 2.0](LICENSE) © wanghw and contributors.

## Legal / responsible use

This tool reads plaintext secrets from heap dumps of JVMs you operate or are explicitly
authorized to assess. It performs **no exploitation**. Credential live-validation only
identifies whether a key is active — it does not exfiltrate data. Use it for defensive
auditing, incident response and authorized security testing only.

---

<div align="center">

**[中文文档](README_zh.md)** · [Contributing](.github/CONTRIBUTING.md) · [Cases](docs/cases/) · [Upgrade Plan](docs/UPGRADE_PLAN_v4.0.md)

</div>
