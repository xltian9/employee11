# 员工管理系统（employee11）

一套面向中小企业的员工与薪酬管理系统。后端 Spring Boot + Spring Data JPA + MySQL，前端原生 HTML/CSS/JavaScript（无框架、无 CDN 依赖），包含管理端与员工自助端两套界面。

除了常规的部门、员工、公告、请假管理，还实现了**考勤统计 → 请假审批 → 绩效评分 → 工资计算 → Excel 导出**的完整链路，以及覆盖敏感操作的审计日志。

---

## 功能特性

### 管理端（左侧边栏 10 个模块）

| 模块 | 说明 |
| --- | --- |
| 概览 | 在职人数、本月应发/实发、待审批请假，最新公告与最近操作 |
| 部门管理 | 部门增删改查，员工通过 `department_id` 外键关联部门 |
| 员工管理 | 分页列表、关键词/部门/状态筛选、照片上传、月薪与入职日期维护 |
| 考勤管理 | 全员月度汇总（应出勤/实际出勤/迟到/早退/旷工/各类请假/加班）+ 逐日明细 |
| 请假管理 | 审批通过/拒绝、撤销、按月统计 |
| 公告管理 | 草稿 → 发布 → 撤回，员工端只看到已发布 |
| 业绩工资 | 月度工资明细 24 列、绩效录入、一键导出 Excel |
| 薪酬调整 | 补发/扣款登记、加班登记（工作日/周末/节假日） |
| 薪酬规则 | 19 项参数在线配置：请假扣款方式、病假系数、旷工折算、迟到罚款、加班费率、社保公积金比例、个税起征点、绩效奖金系数、上下班时间 |
| 审计日志 | 谁改了谁的工资、谁批了谁的假，含前后值明细，可按操作人/动作/对象/时间检索 |

### 员工自助端（6 个板块）

我的信息 · 我的考勤（签到签退 + 月度统计 + 逐日明细） · 我的请假（提交/撤销） · 我的绩效（历史季度评分与等级） · 我的工资（当月明细 + 导出） · 公司公告

### 关键设计

- **工资计算**：应发 = 月薪 − 请假扣款 − 旷工扣款 − 迟到扣款 + 加班费 + 绩效奖金 + 补发 − 扣款；再扣社保、公积金、个税，得到实发。
- **绩效模型**：综合分 = 业绩 60% + 能力 20% + 态度 20% − 出勤扣分（旷工 2 分/天、迟到 0.5 分/次，上限 20），等级 S≥90 / A≥80 / B≥70 / C≥60 / D，等级决定季度奖金系数。
- **考勤与请假联动**：不写定时任务，改为按月**实时逐工作日比对**——有打卡判迟到早退，有已批请假算请假，都没有算旷工。审批或撤销立刻反映到工资，不会出现"同步任务失败导致数据错一天"。
- **审计**：在业务层显式记录（拿到"月薪 30000.00 → 35000.00"这类前后值），而不是 AOP 切面。
- **统一异常**：`BusinessException` + `GlobalExceptionHandler`，**HTTP 状态码 = 业务 code**（200/400/401/403/404/409/500），前端 `api.js` 只需一套拦截逻辑。

---

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 运行时 | JDK 8（Temurin 1.8.0_504） |
| 框架 | Spring Boot 2.7.0（Web / Data JPA / Validation） |
| 数据库 | MySQL 8.0.28（utf8mb4） |
| 密码 | BCrypt（`spring-security-crypto`，未引入 Security 自动配置） |
| Excel | Apache POI 5.2.5 + commons-io 2.16.1 |
| 接口文档 | springfox 3.0.0 → `/swagger-ui/index.html` |
| 构建 | Maven 3.9.16 |
| 前端 | 原生 ES5 + 自绘 CSS（`static/css/app.css`），无 Bootstrap / jQuery |

---

## 快速开始

### 1. 环境要求

- JDK 8（`JAVA_HOME` 指向它）
- MySQL 8.x
- Maven 3.6+

### 2. 建库建表

```bash
mysql -uroot -p -e "CREATE DATABASE employee_db DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p --default-character-set=utf8mb4 employee_db -e "source src/main/resources/db/init.sql"
```

`init.sql` 会创建 11 张表，并插入 1 个管理员、4 个部门、5 名演示员工和一行默认薪酬规则。
若从旧库升级，`ddl-auto: update` 也会在启动时自动补建新增的表与字段。

### 3. 改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456       # 改成你的密码
file:
  upload:
    dir: D:/employee11-img/   # 员工照片存放目录，需事先创建
```

### 4. 启动

```bash
mvn -DskipTests package
java -jar target/employee-1.0-SNAPSHOT.jar
```

Windows 下可直接用 `restart.ps1`（停旧进程 → clean package → 后台启动 → 轮询日志等待启动完成）。

首次启动日志里会出现：

```
PasswordMigrationRunner : 已为 5 名员工生成初始密码（初始密码 = 手机号，BCrypt 存储）
```

### 5. 访问

- 系统入口：<http://localhost:8080>
- 接口文档：<http://localhost:8080/swagger-ui/index.html>

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 员工 | 姓名，如 `张三` | 初始密码 = 本人手机号（如 `13900000001`），登录后可在员工端修改 |

演示员工：张三 13900000001、李四 13900000002、王五 13900000003、赵六 13900000004、孙七 13900000005。

> 打包前必须先停掉正在运行的 java 进程，否则 Windows 上会因 jar 被占用而报 `Unable to rename ... .jar.original`。

---

## 接口一览

所有接口统一返回 `{"code":200,"msg":"操作成功","data":...}`；未登录返回 401，越权返回 403，参数错误 400，重复操作 409。

| 分组 | 主要接口 |
| --- | --- |
| 认证 | `POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`POST /api/auth/change-password` |
| 员工 | `GET/POST /api/employees`、`GET /api/employees/page`、`GET /api/employees/search`、`GET/PUT/DELETE /api/employees/{id}`、`GET /api/employees/{id}/photo`、`POST /api/employees/{id}/upload-photo` |
| 部门 | `GET/POST /api/departments`、`GET/PUT/DELETE /api/departments/{id}` |
| 公告 | `GET/POST /api/notices`、`GET /api/notices/published`、`POST /api/notices/{id}/publish`、`POST /api/notices/{id}/withdraw` |
| 请假 | `GET/POST /api/leaves`、`GET /api/leaves/my`、`GET /api/leaves/pending`、`POST /api/leaves/{id}/approve`、`POST /api/leaves/{id}/cancel`、`GET /api/leaves/meta` |
| 考勤 | `POST /api/attendance/check-in/{employeeId}`、`POST /api/attendance/check-out/{attendanceId}`、`GET /api/attendance/summary`、`GET /api/attendance/summary/all`、`GET /api/attendance/daily` |
| 绩效 | `GET/POST /api/performances`、`PUT /api/performances/{id}`、`GET /api/performances/my` |
| 工资报告 | `GET /api/report/list`（全员）、`GET /api/report/my`（本人）、`GET /api/report/export`、`GET /api/report/my/export` |
| 薪酬规则 | `GET/PUT /api/salary-policy` |
| 补发扣款 | `GET/POST /api/salary-adjustments`、`DELETE /api/salary-adjustments/{id}`、`GET /api/salary-adjustments/types` |
| 加班 | `GET/POST /api/overtimes`、`DELETE /api/overtimes/{id}`、`GET /api/overtimes/types` |
| 审计 | `GET /api/audit-logs`、`GET /api/audit-logs/actions` |
| 管理员 | `GET /api/admin/info`、`POST /api/admin/add`、`PUT /api/admin/update` |

导出的 Excel 含「工资报告」（33 列 + SUM 合计行）、「业绩明细」，当月有补发/扣款时追加「补发扣款明细」sheet。

---

## 项目结构

```
com.limiti.employee
├── common        Result / ApiCode 统一响应与错误码
├── config        WebMvcConfig（静态资源 + 拦截器注册）、Swagger 配置
├── controller    15 个 REST 控制器（含页面跳转的 IndexController）
├── dto           请求/响应 DTO（工资报告、考勤汇总、绩效、分页等）
├── entity        11 个 JPA 实体
├── exception     BusinessException、GlobalExceptionHandler
├── model         请求体模型
├── repository    Spring Data JPA 仓储
├── security      AuthInterceptor、SessionUser、PasswordService、密码迁移
├── service       业务层（含 impl 子包）
└── utils         WorkdayUtils 工作日计算等

src/main/resources
├── application.yml
├── db/init.sql
└── static        前端 29 个文件：login.html、index.html（管理端）、pages/（片段）、js/、css/app.css
```

---

## 安全说明

- 登录密码使用 **BCrypt** 存储；历史无盐 MD5 数据在用户首次登录时自动升级为 BCrypt。
- 员工登录不再"手机号即密码"：手机号只作为初始密码，可在员工端修改。
- `AuthInterceptor` 统一拦截 `/api/**`（登录、登出除外），未登录 401；各接口再按角色校验，员工只能访问本人数据（用服务端 session 中的 id，不信任前端传参）。
- 员工手机号即初始密码，属于弱口令策略，**上线前建议强制首次登录修改密码**（当前为提示文案，未强制）。
- 照片上传限制：后缀白名单（jpg/jpeg/png/gif/bmp）+ 5 MB + `ImageIO` 内容校验 + UUID 重命名；读取时限制在上传目录内，防路径穿越。

---

## 已知问题

1. 没有单元测试/集成测试（`src/test` 仅空壳）。
2. 员工工号取号（`findMax + 1`）与重复审批未加锁，高并发下可能撞号。
3. `WorkdayUtils` 的节假日表硬编码，只覆盖到 2023 年，之后需维护；不支持调休（周末上班）。
4. 员工列表照片逐行请求，未做缩略图。
5. springfox 3.0.0 已停止维护，升级 Spring Boot 3 需换 springdoc。

---

## 开发计划

- [x] 用户认证与会话鉴权
- [x] 文件（照片）上传
- [x] 数据导出（Excel）
- [x] 系统操作日志（审计）
- [x] 考勤 / 请假 / 工资联动
- [x] 绩效多维评分与等级
- [x] 前端改版（顶部导航 → 侧边栏）
- [ ] 数据导入（Excel 批量导入员工）
- [ ] 邮件/站内通知
- [ ] 单元测试与 CI

---

部署细节、数据库迁移 SQL、逐项验证记录与备份方式见 **[运行说明.md](运行说明.md)**。
