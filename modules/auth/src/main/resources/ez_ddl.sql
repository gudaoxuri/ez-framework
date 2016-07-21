CREATE TABLE IF NOT EXISTS ez_organization
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT '组织编码' ,
    name varchar(200) NOT NULL COMMENT '组织名称' ,
    image varchar(200) NOT NULL COMMENT '组织图标' ,
    category varchar(200) NOT NULL COMMENT '组织类型' ,
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
    INDEX idx_category(category) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '组织表，用于租户管理';

CREATE TABLE IF NOT EXISTS ez_account
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT '用户编码，自动生成' ,
    login_id varchar(200) NOT NULL COMMENT '登录Id' ,
    name varchar(200) NOT NULL COMMENT '用户名' ,
    image varchar(200) NOT NULL COMMENT '头像' ,
    password varchar(200) NOT NULL COMMENT '密码' ,
    email varchar(200) NOT NULL COMMENT 'Email' ,
    ext_id varchar(200) NOT NULL COMMENT '扩展Id，用于关联其它对象以扩展属性，扩展Id多为业务系统用户信息表的主键' ,
    ext_info JSON NOT NULL COMMENT '扩展信息，json格式' ,
    oauth JSON NOT NULL COMMENT 'OAuth认证信息，json格式，key=oauth服务标记，value=openid' ,
    organization_code varchar(200)  NOT NULL COMMENT '所属组织编码' ,
    role_codes JSON NOT NULL COMMENT '所属角色编码列表，json格式' ,
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
    INDEX idx_code(code) ,
    INDEX idx_email(email) ,
    INDEX idx_ext_id(ext_id) ,
    INDEX idx_org_id(organization_code) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '用户表';

CREATE TABLE IF NOT EXISTS ez_role
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT '角色编码，自动生成：所属组织编码@角色标记' ,
    name varchar(200) NOT NULL COMMENT '角色名称' ,
    flag varchar(200) NOT NULL COMMENT '角色标记' ,
    resource_codes JSON NOT NULL COMMENT '可访问资源编码列表，json格式' ,
    organization_code varchar(200) NOT NULL COMMENT '所属组织编码' ,
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
    INDEX idx_org_id(organization_code) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '角色表';

CREATE TABLE IF NOT EXISTS ez_resource
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(1000) NOT NULL COMMENT '资源编码，自动生成：访问方法@资源名称' ,
    method varchar(200) NOT NULL COMMENT '访问方法' ,
    uri varchar(1000) NOT NULL COMMENT '资源URL' ,
    name varchar(200) NOT NULL COMMENT '资源名称' ,
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
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '资源表';

CREATE TABLE IF NOT EXISTS ez_menu
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    code varchar(200) NOT NULL COMMENT '菜单编码，自动生成：所属组织编码@菜单链接' ,
    uri varchar(150) NOT NULL COMMENT '菜单链接' ,
    name varchar(200) NOT NULL COMMENT '菜单名称' ,
    icon varchar(200) NOT NULL COMMENT '菜单图标' ,
    translate varchar(200) NOT NULL COMMENT '菜单名称翻译标识（i18n用）' ,
    role_codes JSON NOT NULL COMMENT '菜单所属角色编码列表，json格式' ,
    parent_code varchar(1000) NOT NULL COMMENT '父菜单编码，用于组装多级菜单' ,
    sort INT NOT NULL COMMENT '显示排序，倒序排列' ,
    organization_code varchar(200) NOT NULL COMMENT '所属组织编码' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
    PRIMARY KEY(id) ,
    INDEX idx_name(name) ,
    INDEX idx_code(code) ,
    INDEX idx_org_id(organization_code) ,
    INDEX idx_enable(enable) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8
COMMENT '菜单表';
