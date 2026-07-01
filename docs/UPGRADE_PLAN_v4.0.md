# HeapDump Analyzer v4.0 升级方案 —— GitHub 热度提升专项

> 目标：3 个月内对标并超越 JDumpSpider（1.1k⭐），冲刺 heap dump × 敏感信息蓝海赛道头部位置。
>
> 制定日期：2026-06-30  当前版本：v3.2.0（96 Spider / 42 YAML 规则 / 10+ 验证器）

---

## 一、现状诊断

### 1.1 已具备的技术领先优势

| 维度 | 现状 | 对比 JDumpSpider |
|---|---|---|
| Spider 数量 | 96 个，覆盖 10 大类 | ~20 个，远超 |
| JDK 兼容性 | JDK 17 + GraalVM + NetBeans 双解析器 | 仅 Oracle JDK 1.8 |
| 规则引擎 | YAML 规则 + 类规则 + 正则规则，可扩展 | 硬编码，不可扩展 |
| 凭证验证 | 10+ 云厂商验证器 | 无 |
| 运行模式 | CLI / JavaFX GUI / Web UI / REPL 四模式 | 仅 CLI |
| 并行扫描 | `--parallel --threads N` | 无 |

### 1.2 决定 GitHub 热度的致命短板

| 缺失项 | 影响 | 修复成本 |
|---|---|---|
| ❌ README.md | 落地页空白，星标转化率接近 0 | 低 |
| ❌ LICENSE | 企业不敢用、法律基础缺失 | 极低 |
| ❌ Logo / Slogan | 无品牌识别度，无法传播 | 中 |
| ❌ .github/（CONTRIBUTING、Issue/PR 模板） | 无贡献入口 | 低 |
| ❌ 截图 / GIF / Demo 站点 | "可视化传播"因子缺失（星标差 3.5 倍） | 中 |
| ❌ 中英双语文档 | 国内涨星慢 40%，海外无法触达 | 中 |
| ❌ HTML 报告导出 | 最易传播的格式缺失 | 中 |
| ❌ 凭证存活标注 | 无法对标 TruffleHog 杀手锏 | 中 |

---

## 二、竞品格局与定位

### 2.1 赛道地图

```
                Heap Dump 分析
                       │
        ┌──────────────┼──────────────┐
   内存泄漏诊断      敏感信息提取      脱敏清理
   (MAT/JIFA)     (本项目/JDumpSpider)  (hprof-redact)
                       │
                       ▼
              与 Secret Scanning 交叉
              (TruffleHog/Gitleaks)
              形成"运行时内存明文"蓝海
```

### 2.2 直接竞品对比

| 特性 | heapdump-analyzer | JDumpSpider | heapdump_tool | Eclipse MAT |
|---|---|---|---|---|
| JDK 兼容 | 8/11/17/21 + GraalVM | 仅 1.8 | 任意 | 任意 |
| Spider 数 | 96 | ~20 | 关键词扫描 | 无 |
| 规则可扩展 | ✅ YAML + Java | ❌ | ❌ | N/A |
| 凭证验证 | ✅ 10+ | ❌ | ❌ | N/A |
| Web UI | ✅ | ❌ | ❌ | ❌ |
| 桌面 GUI | ✅ JavaFX | ❌ | ❌ | ✅ RCP |
| REPL | ✅ | ❌ | ✅ 交互式 | ❌ |
| 并行扫描 | ✅ | ❌ | ❌ | ❌ |
| 维护状态 | 活跃 | 停滞 | 活跃 | 活跃 |

### 2.3 定位

- **Slogan**：`Dig secrets out of JVM memory.`
- **双受众**：红队/渗透测试人员 + 运维/SRE 自查
- **差异化**：唯一结合「heap dump 解析 + 100+ Spider + 规则引擎 + 凭证验证 + Web UI」的开源工具

---

## 三、硬约束（继承自项目记忆）

以下功能**不在本次升级范围内**，已与项目负责人确认：

- ❌ CI/CD 集成（SARIF 输出、GitHub Action、Docker 化）
- ❌ 脱敏功能（脱敏引擎、规则定义、CLI 集成）

**替代方案**：
- 无 Docker → 提供 `jpackage` 跨平台原生包 + `start.sh` 一键启动
- 无 CI → 手动 tag + GitHub Release + release 脚本
- 无脱敏 → 在文档中明确"提取用途"，引导用户合规使用

---

## 四、四维度升级方案

### 4.1 UI 设计

| 任务 | 描述 | 优先级 |
|---|---|---|
| Web UI Severity 卡片 | CRITICAL 红 / HIGH 橙 / MEDIUM 黄 / LOW 蓝 计数卡，参考 TruffleHog/GitGuardian | P0 |
| Web UI 暗色模式 | 默认暗色，可切换亮色 | P0 |
| Web UI 仪表盘 | 敏感信息分类饼图、Spider 命中柱状图、引用链拓扑图 | P1 |
| JavaFX 同步升级 | 同步 Severity 卡片 + 暗色模式 | P1 |
| HTML 报告导出 | 一键导出独立 HTML 文件（含图表，可邮件/Slack 分享） | P0 |

### 4.2 功能设计

| 任务 | 描述 | 优先级 |
|---|---|---|
| 凭证存活验证 | AWS/GitHub/Stripe/Slack 等 key 调用 API 标 `LIVE/EXPIRED/UNKNOWN`，参考 TruffleHog | P0 |
| 规则外置化 | YAML 规则支持 `~/.heapdump-analyzer/rules/` 用户目录加载 | P1 |
| OQL/SQL 查询面板 | Web UI 内嵌 OQL 编辑器，参考 JIFA Calcite SQL | P2 |
| 批量扫描 | `--batch <dir>` 一次扫描多个 heap dump | P2 |
| 增量规则贡献流程 | 文档化"5 分钟加一个 Spider / 规则"教程 | P0 |

### 4.3 产品设计

| 任务 | 描述 | 优先级 |
|---|---|---|
| Logo 设计 | 醒目 Logo（参考 TruffleHog 猪鼻吉祥物的辨识度） | P0 |
| Slogan 定稿 | "Dig secrets out of JVM memory" | P0 |
| README 对比表 | 首屏放 vs JDumpSpider/heapdump_tool/MAT 功能对比 | P0 |
| GIF 演示 | CLI + Web UI + 桌面 GUI 三模式各一段 GIF | P0 |
| 在线 Demo 站点 | 部署只读 Demo（参考 jifa.dragonwell-jdk.io） | P1 |
| 案例集 | docs/cases/ 收录"从 heapdump 提取 50 种云凭证"等技术案例 | P1 |

### 4.4 开源运营设计

| 任务 | 描述 | 优先级 |
|---|---|---|
| 双语 README | 英文主 + 中文副，3 步 quickstart | P0 |
| LICENSE | Apache 2.0 | P0 |
| CONTRIBUTING.md | 贡献流程 + 5 分钟 Spider/规则教程 | P0 |
| Issue/PR 模板 | .github/ISSUE_TEMPLATE/、PULL_REQUEST_TEMPLATE.md | P0 |
| CODE_OF_CONDUCT | Contributor Covenant | P1 |
| 首发渠道 | 掘金/知乎/FreeBuf/看雪 技术文章 | P0 |
| 投递 awesome 列表 | awesome-java-security / awesome-forensics | P1 |
| 月度发版节奏 | 紧跟新 CVE / 新框架（Spring Boot 3.x、Shiro 新版） | 持续 |

---

## 五、分阶段执行计划

### 阶段一：门面工程（1 周，W1-W2）

**目标**：让 GitHub 落地页从 0 到 1，具备星标转化能力。

- [ ] 编写双语 README.md（含对比表、quickstart、截图位、GIF 位）
- [ ] 添加 LICENSE（Apache 2.0）
- [ ] 设计 Logo + Slogan
- [ ] .github/CONTRIBUTING.md
- [ ] .github/ISSUE_TEMPLATE/（bug_report.md、feature_request.md）
- [ ] .github/PULL_REQUEST_TEMPLATE.md
- [ ] .github/CODE_OF_CONDUCT.md
- [ ] 录制 GIF 演示（CLI / Web UI / 桌面 GUI 三模式）

**完成标志**：GitHub 落地页首屏完整、专业、有辨识度。

### 阶段二：差异化功能并行（2-3 周，W3-W5）

**目标**：补齐对标 TruffleHog/HeapHero 的杀手锏功能。

#### Track A：HTML 报告导出
- [ ] 设计 HTML 报告模板（含 Severity 统计、分类图表、详情列表）
- [ ] CLI 新增 `--format html -o report.html`
- [ ] Web UI 新增"导出 HTML"按钮
- [ ] 内嵌 ECharts/Chart.js 渲染图表

#### Track B：凭证存活验证
- [ ] 扩展 `CredentialValidator` 接口，新增 `validateLive()` 方法
- [ ] 实现 AWS / GitHub / Stripe / Slack / Telegram 5 个 LIVE 验证器
- [ ] CLI/GUI/Web UI 三端展示 `LIVE/EXPIRED/UNKNOWN` 状态
- [ ] 默认关闭，需 `--validate-live` 显式开启（避免误触发云告警）

#### Track C：Web UI 视觉升级
- [ ] Severity 色彩卡片（CRITICAL/HIGH/MEDIUM/LOW/INFO 计数）
- [ ] 暗色模式默认 + 亮色切换
- [ ] 仪表盘页（饼图 + 柱状图 + 拓扑图）
- [ ] 结果列表支持 Severity 过滤、关键词搜索、分页

**完成标志**：HTML 报告可分享、凭证可验证、Web UI 截图能"自传播"。

### 阶段三：生态扩展（1 月，W6-W9）

- [ ] 规则外置化（`~/.heapdump-analyzer/rules/` 加载）
- [ ] "5 分钟加 Spider / 规则"贡献教程
- [ ] 在线 Demo 站点部署（只读、预置样例 heapdump）
- [ ] OQL/SQL 查询面板
- [ ] 批量扫描 `--batch`
- [ ] 案例集 docs/cases/（首篇："从 heapdump 提取 50 种云凭证"）

### 阶段四：社区运营（持续）

- [ ] 首发技术文章（掘金/FreeBuf/看雪）
- [ ] 投递 HelloGitHub / GitHubDaily
- [ ] 提交 awesome-java-security / awesome-forensics
- [ ] 月度发版 + Release Notes
- [ ] Issue 24h 内首响应（每次回复带来 15% 月增长）

---

## 六、热度指标与里程碑

| 时间点 | 目标 stars | 关键里程碑 |
|---|---|---|
| W2（门面完成） | 50+ | README/Logo 上线，开始内部传播 |
| W5（功能完成） | 200+ | HTML 报告 + 凭证验证 + Web UI 升级 |
| W9（生态完成） | 500+ | Demo 站点 + 规则外置 + 案例文章 |
| M3（运营 1 月） | 1k+ | 对标 JDumpSpider，进入赛道头部 |
| M6 | 2k+ | 赛道第一，被 awesome 列表收录 |

---

## 七、风险与对策

| 风险 | 对策 |
|---|---|
| 凭证验证误触发云告警（AWS GuardDuty） | 默认关闭，需显式 `--validate-live`，文档强警告 |
| 在线 Demo 被滥用上传大文件 | 仅预置样例 heapdump，禁止用户上传 |
| 规则市场碎片化 | 官方维护核心规则库，社区规则独立仓库 |
| 无 CI 导致贡献门槛高 | 提供 `verify.sh` 本地校验脚本 + 详细 CONTRIBUTING |
| 无 Docker 导致试用门槛高 | `jpackage` 跨平台原生包 + `start.sh` 一键启动 |

---

## 八、参考项目

- Eclipse MAT: https://github.com/eclipse-mat/mat
- Eclipse JIFA: https://github.com/eclipse/jifa
- JDumpSpider: https://github.com/whwlsfb/JDumpSpider
- heapdump_tool: https://github.com/wyzxxz/heapdump_tool
- hprof-redact: https://github.com/parttimenerd/hprof-redact
- TruffleHog: https://github.com/trufflesecurity/trufflehog
- Gitleaks: https://github.com/gitleaks/gitleaks

---

**版本**：v4.0 草案
**状态**：待批准
**下一步**：等待项目负责人确认后，进入阶段一执行
