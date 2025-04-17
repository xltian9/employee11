# 员工管理系统

## 项目介绍
基于Spring Boot的员工管理系统，提供部门管理、员工管理、公告管理和请假管理等功能。

## 技术栈
- 后端：Spring Boot, Spring MVC, Spring Data JPA
- 前端：HTML5, CSS3, JavaScript, Bootstrap
- 数据库：MySQL
- 工具：Maven, Git

## 功能模块
- 部门管理：部门的增删改查
- 员工管理：员工信息的维护
- 公告管理：公告的发布、撤回
- 请假管理：请假申请和审批

## 快速开始
1. 创建数据库  source src/main/resources/db/init.sql
2. 修改application-dev.properties中的数据库连接信息
3. 启动项目  mvn spring-boot:run

4、访问系统
http://localhost:8080
默认管理员账号：admin
默认密码：admin123

## 项目结构
com.limiti.employee
├── common // 公共组件
├── config // 配置类
├── controller // 控制器
├── entity // 实体类
├── repository // 数据访问层
├── service // 业务逻辑层
└── util // 工具类

## 开发计划
- [ ] 添加用户认证和权限管理
- [ ] 添加文件上传功能
- [ ] 添加数据导入导出
- [ ] 添加邮件通知功能
- [ ] 添加系统日志功能

