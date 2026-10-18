-- 创建数据库
CREATE DATABASE IF NOT EXISTS employee_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE employee_db;

-- 管理员表
CREATE TABLE admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 部门表
CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    description TEXT COMMENT '部门描述',
    manager VARCHAR(100) COMMENT '部门主管',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 员工表
CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    employee_number VARCHAR(50) NOT NULL COMMENT '工号',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    password VARCHAR(100) COMMENT '登录密码（BCrypt），初始密码为手机号',
    department VARCHAR(100) NOT NULL COMMENT '部门（展示用快照，写入时按名称同步）',
    department_id BIGINT COMMENT '所属部门ID（外键 departments.id）',
    position VARCHAR(100) NOT NULL COMMENT '职位',
    wage DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '月薪',
    entry_date DATETIME NOT NULL COMMENT '入职日期',
    status BOOLEAN NOT NULL DEFAULT TRUE COMMENT '在职状态',
    is_del TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    photo_path VARCHAR(255) COMMENT '照片存储路径',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_number (employee_number),
    KEY idx_employees_department (department_id),
    KEY idx_employees_is_del (is_del),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 考勤表
CREATE TABLE attendances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    date DATE NOT NULL COMMENT '考勤日期',
    clock_in TIME COMMENT '上班时间',
    clock_out TIME COMMENT '下班时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-迟到，2-早退，3-旷工',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    UNIQUE KEY uk_employee_date (employee_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- 请假表
CREATE TABLE leaves (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '请假ID',
    employee_id BIGINT NOT NULL COMMENT '申请人ID',
    type TINYINT NOT NULL COMMENT '请假类型：1-事假，2-病假，3-年假，4-调休',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reason TEXT NOT NULL COMMENT '请假原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
    approver_id BIGINT COMMENT '审批人ID',
    comment TEXT COMMENT '审批意见',
    approve_time DATETIME COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (approver_id) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假表';

-- 公告表
CREATE TABLE notices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-已发布，2-已撤回',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (publisher_id) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 绩效表
CREATE TABLE performances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '绩效ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    year INT NOT NULL COMMENT '年份',
    quarter TINYINT NOT NULL COMMENT '季度：1-4',
    month INT NULL COMMENT '历史字段：月度业绩，可为空',
    work_score INT NULL COMMENT '历史字段：工作评分，可为空',
    score DECIMAL(3,1) NULL COMMENT '业绩分（权重60%）',
    ability_score DECIMAL(3,1) NULL COMMENT '能力分（权重20%）',
    attitude_score DECIMAL(3,1) NULL COMMENT '态度分（权重20%）',
    final_score DECIMAL(4,1) NULL COMMENT '综合得分（后端计算）',
    grade VARCHAR(2) NULL COMMENT '等级：S/A/B/C/D',
    evaluation TEXT NOT NULL COMMENT '评价',
    evaluator_id BIGINT NOT NULL COMMENT '评价人ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待确认，1-已确认',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (evaluator_id) REFERENCES admins(id),
    UNIQUE KEY uk_employee_year_quarter (employee_id, year, quarter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效表';

-- 薪酬规则表（全局单行配置，id 固定为 1）
CREATE TABLE salary_policy (
    id BIGINT PRIMARY KEY COMMENT '固定为 1',
    leave_deduct_mode TINYINT NOT NULL DEFAULT 0 COMMENT '请假扣款方式：0-按日工资，1-固定金额',
    leave_deduct_amount DECIMAL(10,2) NOT NULL DEFAULT 200.00 COMMENT '固定扣款金额（每天）',
    sick_leave_rate DECIMAL(4,2) NOT NULL DEFAULT 0.50 COMMENT '病假扣款系数（相对事假）',
    absent_deduct_days DECIMAL(4,2) NOT NULL DEFAULT 3.00 COMMENT '旷工一天折算扣几天工资',
    late_deduct_amount DECIMAL(10,2) NOT NULL DEFAULT 50.00 COMMENT '迟到/早退每次扣款',
    overtime_weekday_rate DECIMAL(4,2) NOT NULL DEFAULT 1.50 COMMENT '工作日加班费倍率',
    overtime_weekend_rate DECIMAL(4,2) NOT NULL DEFAULT 2.00 COMMENT '周末加班费倍率',
    overtime_holiday_rate DECIMAL(4,2) NOT NULL DEFAULT 3.00 COMMENT '节假日加班费倍率',
    social_insurance_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1050 COMMENT '个人社保比例',
    housing_fund_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0700 COMMENT '个人公积金比例',
    tax_threshold DECIMAL(10,2) NOT NULL DEFAULT 5000.00 COMMENT '个税起征点（月）',
    bonus_rate_s DECIMAL(4,2) NOT NULL DEFAULT 0.25 COMMENT 'S 级季度奖金系数',
    bonus_rate_a DECIMAL(4,2) NOT NULL DEFAULT 0.18 COMMENT 'A 级季度奖金系数',
    bonus_rate_b DECIMAL(4,2) NOT NULL DEFAULT 0.12 COMMENT 'B 级季度奖金系数',
    bonus_rate_c DECIMAL(4,2) NOT NULL DEFAULT 0.06 COMMENT 'C 级季度奖金系数',
    bonus_rate_d DECIMAL(4,2) NOT NULL DEFAULT 0.00 COMMENT 'D 级季度奖金系数',
    work_start VARCHAR(5) NOT NULL DEFAULT '09:00' COMMENT '上班时间（判迟到）',
    work_end VARCHAR(5) NOT NULL DEFAULT '18:00' COMMENT '下班时间（判早退）',
    late_to_absent_minutes INT NOT NULL DEFAULT 120 COMMENT '迟到超过多少分钟按旷工半天/一天',
    update_by VARCHAR(50) NULL COMMENT '最后修改人',
    update_time DATETIME NULL COMMENT '最后修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪酬规则（全局单行）';

-- 审计日志表
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id BIGINT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
    operator_type VARCHAR(20) NOT NULL COMMENT '操作人类型：ADMIN/EMPLOYEE/SYSTEM/ANONYMOUS',
    action VARCHAR(40) NOT NULL COMMENT '动作编码',
    action_name VARCHAR(60) NOT NULL COMMENT '动作名称',
    target_type VARCHAR(30) NULL COMMENT '对象类型',
    target_id BIGINT NULL COMMENT '对象ID',
    target_name VARCHAR(100) NULL COMMENT '对象名称',
    detail TEXT NULL COMMENT '明细（含变更前后值）',
    ip VARCHAR(64) NULL COMMENT '来源 IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    KEY idx_audit_time (create_time),
    KEY idx_audit_operator (operator_name),
    KEY idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

-- 补发 / 扣款表
CREATE TABLE salary_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(50) NULL COMMENT '员工姓名（冗余，便于展示）',
    year INT NOT NULL COMMENT '年份',
    month INT NOT NULL COMMENT '月份',
    type TINYINT NOT NULL COMMENT '类型：1-补发，2-扣款',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额（正数）',
    reason VARCHAR(200) NOT NULL COMMENT '原因',
    operator_id BIGINT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_adj_month (year, month),
    KEY idx_adj_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补发扣款';

-- 加班记录表
CREATE TABLE overtime_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(50) NULL COMMENT '员工姓名（冗余）',
    work_date DATE NOT NULL COMMENT '加班日期',
    hours DECIMAL(5,1) NOT NULL COMMENT '加班小时数',
    ot_type TINYINT NOT NULL COMMENT '类型：1-工作日，2-周末，3-节假日',
    reason VARCHAR(200) NULL COMMENT '加班事由',
    operator_id BIGINT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ot_date (work_date),
    KEY idx_ot_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录';

-- 薪酬规则默认值
INSERT INTO salary_policy (id) VALUES (1);

-- 插入初始数据
-- 密码为 MD5 加密后的值：admin123 -> 0192023a7bbd73250516f069df18b500
-- （AuthService 用 DigestUtils.md5DigestAsHex 逐位比对，这里必须存密文，不能存明文）
INSERT INTO admins (username, password, name, phone, email, status) VALUES 
('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', '13800000000', 'admin@employee.local', 1);

INSERT INTO departments (name, description, manager) VALUES 
('技术部', '负责技术研发', '张三'),
('人力资源部', '负责人事管理', '李四'),
('市场部', '负责市场营销', '王五'),
('财务部', '负责财务管理', '赵六');

INSERT INTO employees (employee_number, name, department, position, phone, email, entry_date) VALUES 
('EMP2400001', '张三', '技术部', '技术总监', '13900000001', 'zhangsan@123.com', '2024/12/1'),
('EMP2400002', '李四', '人力资源部', 'HR总监', '13900000002', 'lisi@123.com', '2024/12/1'),
('EMP2400003', '王五', '市场部', '市场总监', '13900000003', 'wangwu@123.com', '2024/12/1'),
('EMP2400004', '赵六', '财务部', '财务总监', '13900000004', 'zhaoliu@123.com', '2024/12/1'),
('EMP2400005', '孙七', '技术部', '高级工程师', '13900000005', 'sunqi@123.com', '2024/12/1');

-- photo_path 已在上面的 employees 建表语句中声明，无需再 ALTER
-- 若后续要存 Base64（适合小图片），再单独执行：
-- ALTER TABLE employees ADD COLUMN photo_base64 TEXT;

-- 回填员工的 department_id（按部门名关联；新增员工时由 EmployeeService 自动写入）
UPDATE employees e
    JOIN departments d ON e.department = d.name
SET e.department_id = d.id
WHERE e.department_id IS NULL;
