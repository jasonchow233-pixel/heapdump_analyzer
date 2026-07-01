# Case Study: Extracting 50+ Cloud Credentials from a Single Heap Dump

> A walkthrough showing how HeapDump Analyzer v4.0 recovers a broad inventory of cloud
> credentials from one production JVM heap dump — and how the live validator triages which
> keys are still active.

## Scenario

An incident-response team captures a heap dump from a compromised Spring Boot 3 gateway
service that federated authentication to ~15 downstream microservices and brokered calls to
a dozen cloud SaaS providers. The dump (`gateway.hprof`, ~1.4 GB) is handed to the
analysis team with one question:

> *What cloud credentials did this JVM have in memory, and which are still valid?*

## Step 1 — Inventory

```bash
java -jar target/heapdump-analyzer.jar gateway.hprof --format html -o gateway-report.html
```

The HTML report's severity cards immediately surface the picture:

| Severity | Count |
|---|---|
| Critical | 38 |
| High | 24 |
| Medium | 11 |
| Low | 6 |

The **Live credential validation** section is empty here — that comes next, because
`--validate-live` is off by default.

## Step 2 — Triage with live validation

```bash
java -jar target/heapdump-analyzer.jar gateway.hprof \
      --rules-only --validate-live --severity HIGH \
      -o gateway-live.txt
```

```
[!] --validate-live enabled: real outbound API calls will be made.
    This may trigger cloud threat-detection alerts (e.g. AWS GuardDuty).
```

The rule engine walks every credential-shaped string found by the regex rules and calls the
matching provider API. Output:

```
[GitHub Personal Access Token] Detect GitHub personal access tokens
  ghp_9aZ...redacted... [LIVE | login=ci-deploy-bot]
  ghp_x7Q...redacted... [EXPIRED | HTTP 401]
[Stripe API Key] Detect Stripe live/test API keys
  sk_live_51H...redacted... [LIVE | LIVE secret works]
[Telegram Bot Token] Detect Telegram Bot API tokens
  5182394111:AAH...redacted... [LIVE | bot=@gw_alerts_bot]
[AWS Access Key ID] Detect AWS Access Key IDs (AKIA prefix) in heap string pool
  AKIAIOSFODNN7EXAMPLE [UNKNOWN | STS GetCallerIdentity requires Access Key + Secret Access Key (SigV4)]
```

Status legend:
- **LIVE** — the provider accepted the credential; it is still active and must be revoked.
- **EXPIRED** — the provider rejected it (401/403); already rotated or revoked.
- **UNKNOWN** — could not be determined online (e.g. AWS, where only the access key id is
  in the string pool; the secret lives in a separate field).

## Step 3 — Recover the full inventory

Across the 94 Spiders + 42 rules, the single dump yielded credentials / config touching
**50+ cloud/SaaS surfaces**, grouped here by category:

**Cloud IAM (12)**
AWS access keys, AWS secret access keys (via `AWSCredentialSearch` Spider), GCP service
account JSON, Azure connection strings, Aliyun AK/SK, Huawei AK/SK, Tencent AK/SK,
K8s service-account tokens, Docker registry tokens, Firebase service accounts, GCP OAuth
client secrets, Cloudflare-style API keys.

**Auth & identity (11)**
JWT signing keys (HS256/RS256), OAuth2 client secrets, Shiro AES keys, Spring Security
password encoders, SA-Token tokens, CAS service tickets, PAC4J profiles, Auth0 client
secrets, Okta API tokens, GitHub/Slack/Telegram tokens, GitLab PATs.

**Datastores (9)**
HikariCP/Druid JDBC URLs (MySQL/PostgreSQL/Oracle), MongoDB connection strings, Redis
passwords (Lettuce + Jedis), ClickHouse, HBase, Neo4j, InfluxDB, Couchbase, Elasticsearch
credentials.

**Messaging (5)**
Kafka SASL/SCRAM, RocketMQ access keys, RabbitMQ AMQP URLs, ActiveMQ, Pulsar tokens.

**Config & registry (7)**
Nacos username/password, Apollo config tokens, Spring Cloud Config, Dubbo registries,
Seata, ZooKeeper auth, Spring Cloud Gateway route secrets.

**HTTP interceptors (3)**
OkHttp interceptor-injected `Authorization` headers, RestTemplate `Bearer` headers,
Apache HttpClient credential caches, Feign client secrets.

**Observability/SaaS (5)**
Datadog API keys, New Relic keys, Splunk HEC tokens, SendGrid API keys, Twilio
auth tokens.

> Totals: ~52 distinct credential surfaces from one 1.4 GB dump, scan time ~45 s with
> `--parallel --threads 8`.

## Step 4 — Act

The `LIVE`-tagged credentials were revoked first (highest priority). The `UNKNOWN` AWS
keys were paired with the secrets recovered by `AWSCredentialSearch` and re-checked
manually via `aws sts get-caller-identity`. The `EXPIRED` entries were recorded for
forensic completeness but deprioritized.

## Why heap dumps are a credential goldmine

Java holds secrets in memory far longer than developers expect:

1. **Connection pools** (HikariCP, Druid) keep `password` fields alive for the pool's
   lifetime — often the entire JVM uptime.
2. **HTTP clients** (OkHttp, Apache HC, RestTemplate) cache `Authorization` headers in
   request objects that the GC may not collect for many cycles.
3. **Config sources** (Nacos, Apollo, Spring Cloud Config) hold the resolved plaintext in
   `PropertySource` maps indefinitely.
4. **Auth frameworks** (Shiro, Spring Security, JWT) retain signing keys and session
   secrets in static fields or long-lived beans.
5. **String interning** means a secret loaded once may be referenced from the shared
   string pool, surviving even after its owning object is collected.

HeapDump Analyzer turns this runtime reality into a structured, exportable, actionable
report — without running a single line of the application's code.

## Reproduce

```bash
# 1. Full scan → HTML report
java -jar target/heapdump-analyzer.jar your-dump.hprof --format html -o report.html

# 2. Live triage of high/critical credentials (authorize first!)
java -jar target/heapdump-analyzer.jar your-dump.hprof \
      --rules-only --validate-live --severity HIGH -o live.txt

# 3. Or do it in the browser
./start.sh web
```

---

*Part of the [HeapDump Analyzer case collection](../). See the [main README](../../README.md)
for the full feature list and [CONTRIBUTING](../../.github/CONTRIBUTING.md) to add your own
case.*
