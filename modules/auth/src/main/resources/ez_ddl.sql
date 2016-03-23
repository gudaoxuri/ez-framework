CREATE TABLE IF NOT EXISTS ez_organization
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT 'Code' ,
    name varchar(200) NOT NULL COMMENT 'Name' ,
    image varchar(200) COMMENT 'Image' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_code(code) ,
    INDEX idx_name(name) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ez_account
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    login_id varchar(200) NOT NULL COMMENT 'Login Id' ,
    name varchar(200) NOT NULL COMMENT 'Name' ,
    image varchar(200) COMMENT 'Image' ,
    password varchar(200) NOT NULL COMMENT 'Password' ,
    email varchar(200) NOT NULL COMMENT 'Email' ,
    ext_id varchar(200) COMMENT 'Ext Id' ,
    ext_info JSON COMMENT 'Ext Info' ,
    oauth JSON COMMENT 'OAuth Info' ,
    organization_code varchar(200) COMMENT '' ,
    role_codes JSON COMMENT '' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_login_id(login_id) ,
    INDEX idx_name(name) ,
    INDEX idx_email(email) ,
    INDEX idx_ext_id(ext_id) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ez_role
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT 'Code' ,
    name varchar(200) NOT NULL COMMENT 'Name' ,
    flag varchar(200) NOT NULL COMMENT 'Flag' ,
    resource_codes JSON COMMENT '' ,
    organization_code varchar(200) COMMENT '' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_code(code) ,
    INDEX idx_flag(flag) ,
    INDEX idx_name(name) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ez_resource
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(1000) NOT NULL COMMENT 'Code（Method+URI）' ,
    method varchar(200) NOT NULL COMMENT 'Method' ,
    uri varchar(1000) NOT NULL COMMENT 'URI' ,
    name varchar(200) NOT NULL COMMENT 'Name' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_method(method) ,
    INDEX idx_name(name) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ez_menu
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    uri varchar(1000) NOT NULL COMMENT 'URI' ,
    name varchar(200) NOT NULL COMMENT 'Name' ,
    icon varchar(200) COMMENT '' ,
    translate varchar(200) COMMENT '' ,
    role_codes JSON COMMENT '' ,
    parent_uri varchar(1000) COMMENT '' ,
    sort INT COMMENT '' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_name(name) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ez_token_info
(
    id varchar(200)  NOT NULL COMMENT '记录主键' ,
    login_id varchar(200) COMMENT '' ,
    login_name varchar(200) COMMENT '' ,
    image varchar(200) COMMENT '' ,
    organization JSON COMMENT '' ,
    roles JSON COMMENT '' ,
    ext_id varchar(200) COMMENT '' ,
    ext_info JSON COMMENT '' ,
    last_login_time BIGINT COMMENT '' ,
    PRIMARY KEY(id) ,
    INDEX idx_login_id(login_id)
)ENGINE=innodb DEFAULT CHARSET=utf8;


