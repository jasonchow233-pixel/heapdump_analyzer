# 增强 searchStrings 支持 byte[]/char[] 数组扫描

## Context

当前 `searchStrings(Pattern)` 只扫描 `java.lang.String` 实例，导致存储在独立 `byte[]`/`char[]` 数组中的凭据（JWT token、密钥、密码等）被遗漏。用户反馈项目 JWT 分析输出比 `strings heapdump | grep` 少很多，根本原因在此。

## 受影响范围

- **9 个 Spider** 直接调用 `searchStrings()`：TokenSearch, AuthTokenSearch, AWSCredentialSearch, AliyunCredentialSearch, GCPCredentialSearch, AzureCredentialSearch, HuaweiCloudCredentialSearch, TencentCloudCredentialSearch, K8sServiceAccountSearch
- **42 条 YAML RegexRule** 通过 `RegexRule.execute()` 间接调用 `searchStrings()`
- **CookieThief** 手动遍历 String 实例（未使用 `searchStrings()`）
- **Main / ReplRunner** 也调用 `searchStrings()`

## 方案：修改 searchStrings 增加数组扫描 + 新增 searchAllTexts

核心思路：在 `IHeapHolder` 新增 `searchAllTexts(Pattern)` 方法，扫描 String + byte[] + char[]，然后让 `searchStrings()` 委托给它。所有现有调用者自动受益。

## 实施步骤

### 步骤 1：IHeapHolder 接口新增方法

文件：`src/main/java/cn/wanghw/IHeapHolder.java`

- 新增 `List<String> searchAllTexts(Pattern pattern)` 方法签名

### 步骤 2：两个 HeapHolder 实现 searchAllTexts

文件：
- `src/main/java/org/netbeans/lib/profiler/heap/NetbeansHeapHolder.java`
- `src/main/java/org/graalvm/visualvm/lib/jfluid/heap/GraalvmHeapHolder.java`

核心逻辑：
```
1. 扫描 java.lang.String 实例（原有逻辑，HashSet 去重）
2. 扫描 byte[] 实例（长度 10~10000 过滤，toString 转换，去重）
3. 扫描 char[] 实例（长度 10~10000 过滤，toString 转换，去重）
```

关键优化：
- 用 `PrimitiveArrayInstance.getLength()` 前置过滤，避免对小数组/巨型数组调用 `getValues()`
- HashSet 基于文本内容去重（String 和其底层 byte[] 内容相同时只返回一次）
- 每个循环检查 `cancelled.get()` 支持中断

### 步骤 3：searchStrings 委托给 searchAllTexts

在两个 HeapHolder 中将 `searchStrings()` 改为 `return searchAllTexts(pattern)`。

### 步骤 4：修改 CookieThief

文件：`src/main/java/cn/wanghw/spider/CookieThief.java`

将手动遍历 String 实例改为调用 `searchAllTexts(Pattern.compile(".*Cookie:.*"))`。

### 步骤 5：RegexRule 确认（无需修改）

`RegexRule.execute()` 已调用 `searchStrings()`，委托后自动受益。

## 验证方式

1. 编译项目确认无报错
2. 使用真实 heapdump 测试 JWT 搜索结果是否增加
3. 对比 `strings heapdump | grep JWT` 的输出验证覆盖率提升
