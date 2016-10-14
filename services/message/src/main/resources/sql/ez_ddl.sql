CREATE TABLE IF NOT EXISTS ez_message
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    to_account varchar(100) NOT NULL COMMENT '个人消息' ,
    to_role varchar(100) NOT NULL COMMENT '角色消息' ,
    category varchar(100) NOT NULL COMMENT '类型' ,
    level varchar(100) NOT NULL COMMENT '级别' ,
    template_code varchar(100) NOT NULL COMMENT '模板' ,
    content varchar(500) NOT NULL COMMENT '内容' ,
    title varchar(150) NOT NULL COMMENT '标题' ,
    start_time BIGINT NOT NULL COMMENT '开始时间,yyyyMMddHHmmss' ,
    end_time BIGINT NOT NULL COMMENT '结束时间,yyyyMMddHHmmss' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_to_account(to_account) ,
    INDEX idx_to_role(to_role),
    INDEX idx_category(category)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '消息表';

CREATE TABLE IF NOT EXISTS ez_message_log
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    message_id varchar(200) NOT NULL COMMENT '消息Id' ,
    read_account_code varchar(200) NOT NULL COMMENT '阅读账号' ,
    read_time BIGINT NOT NULL COMMENT '阅读时间,yyyyMMddHHmmss' ,
    PRIMARY KEY(id) ,
    INDEX idx_message_id(message_id),
    INDEX idx_read_acc_code(read_account_code)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '消息读取日志表';