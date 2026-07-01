## Summary

<!-- Briefly describe what this PR changes and why. -->

## Type of change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New Spider plugin
- [ ] New YAML rule
- [ ] New credential validator
- [ ] New feature (UI / report / CLI flag)
- [ ] Documentation
- [ ] Refactor / chore

## Checklist

- [ ] `./start.sh build` succeeds locally
- [ ] `java -jar target/heapdump-analyzer.jar --list` works
- [ ] `java -jar target/heapdump-analyzer.jar --list-rules` works
- [ ] New Spider is registered in `META-INF/services/cn.wanghw.ISpider` (if applicable)
- [ ] New rule is added to `resources/rules/` and `rule-index.txt` (if applicable)
- [ ] No outbound network calls unless gated behind `--validate-live`
- [ ] README / docs updated if a new capability was added
- [ ] Commit messages follow `feat:` / `fix:` / `docs:` convention

## Test plan

<!-- How did you verify this? Include the command and a redacted snippet of output. -->

```bash
# Reproduction command
java -jar target/heapdump-analyzer.jar ...
```

<!-- Redacted output snippet: -->
