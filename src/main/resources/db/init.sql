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
    score DECIMAL(3,1) NOT NULL COMMENT '评分',
    evaluation TEXT NOT NULL COMMENT '评价',
    evaluator_id BIGINT NOT NULL COMMENT '评价人ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待确认，1-已确认',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (evaluator_id) REFERENCES admins(id),
    UNIQUE KEY uk_employee_year_quarter (employee_id, year, quarter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效表';

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
