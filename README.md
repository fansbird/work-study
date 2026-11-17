# 勤工助学管理系统

> Work-study management system — Spring Boot 3 + LangChain4j + Vue 3

面向高校勤工助学业务的全流程管理系统：岗位发布 → 学生报名 → 工时审核 → 薪酬结算 → 统计报表，并集成基于 Function Calling 的自然语言 AI 助手。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.3.5、Java 17、Maven 多模块 |
| 持久层 | MyBatis、Druid、PageHelper |
| 缓存 | Redis（登录态、AI 会话记忆） |
| 实时通信 | WebSocket |
| 定时任务 | Spring Task（@Scheduled） |
| 接口文档 | Knife4j（OpenAPI 3） |
| 鉴权 | JWT（jjwt） |
| 文件 | 阿里云 OSS、Apache POI |
| AI | LangChain4j 1.0.0-beta5（OpenAI 兼容接口） |
| 前端 | Vue 3、Vite 3、Element Plus、ECharts、Axios |

## 功能模块

1. **岗位管理**：岗位发布、审批、上下架，实时计算剩余名额
2. **报名管理**：学生报名 → 部门审核（录用 / 驳回）→ 撤销；已录用后放弃自动释放名额
3. **工时管理**：学生填报 → 部门审核 → 中心复核 → 驳回重提，含日 / 周 / 月工时上限校验
4. **薪酬结算**：按规则（时薪 × 系数、月封顶、保底）生成结算单 → 确认 → 发放 → 作废
5. **统计报表**：概览、工时趋势、薪酬趋势、岗位统计、类别分布、部门汇总
6. **AI 助手**：用自然语言查询岗位、名额、工时、薪酬等业务数据
7. **消息通知**：WebSocket 实时推送 + 定时待办提醒

## 技术要点

### 1. 名额并发控制

录用时执行条件更新，把并发控制交给数据库：

```sql
UPDATE position SET hired = hired + 1 WHERE id = #{id} AND hired < quota
```

影响行数为 0 表示 `hired` 已达 `quota`，名额已满，抛出业务异常回滚事务；放弃录用时反向执行 `hired = hired - 1 WHERE hired > 0`。依靠数据库行级锁保证并发下不超招，未引入分布式锁。

### 2. AI 助手的工具调用

- 以 `@Tool` 注解封装 4 个业务查询，由模型按用户意图自行选择调用哪个、传什么参数；
- `ChatMemory` 持久化到 Redis，保留最近 20 条消息，TTL 为 1 天，支持多轮上下文；
- 通过 `Flux` 流式返回，前端逐字渲染；
- 未引入 RAG，回答严格基于工具返回的真实业务数据，不做自由发挥。

### 3. AOP 横切关注点

- `AutoFillAspect`：统一填充创建 / 更新时间与操作人；
- `OperLogAspect`：记录操作日志。

### 4. 登录态上下文

ThreadLocal 保存当前用户 id 与角色，由 JWT 拦截器写入；请求结束时在 `afterCompletion` 中强制清理，避免 Tomcat 线程池复用导致的数据串号。

### 5. 定时任务

`@Scheduled` 实现每日 9:00 待办统计与每月 1 日 09:05 结算提醒，统计结果经 WebSocket 群发。

## 目录结构

```
Work-Study Program/
├── work-study/                # 后端 Maven 多模块
│   ├── work-common/           # 公共模块：上下文、异常、常量
│   ├── work-pojo/             # 实体、DTO、VO
│   └── work-server/           # 业务模块：controller / service / mapper / aspect / task / websocket
└── work-study-vue/            # 前端（Vue 3 + Vite）
```

## 本地运行

1. 创建 MySQL 数据库 `work_study`，并建好业务表；
2. 配置环境变量（仓库不含真实凭据）：

```text
MYSQL_PASSWORD
Access_Key_Id        # 阿里云 OSS（文件上传功能需要）
Access_Key_Secret
ALIYUN_API_KEY       # AI 助手需要
```

3. 启动后端：在 `work-study` 目录下 `mvn clean package` 后运行 `work-server` 模块，或用 IDE 直接启动主类；
4. 启动前端：在 `work-study-vue` 目录下 `npm install && npm run dev`。

## 说明

- 配置中的数据库密码、OSS 密钥、AI API Key 均通过环境变量注入，仓库内不含真实凭据；
- 数据库表结构脚本尚未纳入仓库，可按 `work-pojo` 中的实体类与 `mapper` 中 SQL 自行建表。
