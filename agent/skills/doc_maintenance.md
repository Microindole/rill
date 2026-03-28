# Skill: 文档维护

## 适用场景

任何代码修改完成后都应触发本 skill。

## 最低动作

- 更新 `agent/STATUS.md`
- 按影响范围更新架构文档或模块文档
- 当用户要求扫描 `agent/` 全部文档时，主文档、模块文档、foundation 文档和 skills 文档都要检查

## 判断规则

- 影响整体结构：更新 `ARCHITECTURE.md`
- 影响某个模块：更新对应 `modules/*.md`
- 形成新套路：更新 `skills/*.md`
