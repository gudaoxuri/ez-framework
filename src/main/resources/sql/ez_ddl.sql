CREATE TABLE IF NOT EXISTS ez_scheduler
(
    id INT NOT NULL AUTO_INCREMENT COMMENT '记录主键' ,
    name varchar(200) NOT NULL COMMENT '调度名称' ,
    cron varchar(20) NOT NULL COMMENT '调度周期' ,
    clazz varchar(200) NOT NULL COMMENT '回调执行的类' ,
    parameterstr TEXT NOT NULL COMMENT '任务参数' ,
    module varchar(200) NOT NULL COMMENT '使用的模块' ,
    enable BOOLEAN NOT NULL COMMENT '是否启用' ,
    create_user varchar(100) NOT NULL COMMENT '创建用户' ,
    create_org varchar(100) NOT NULL COMMENT '创建组织' ,
    create_time BIGINT NOT NULL COMMENT '创建时间' ,
    update_user varchar(100) NOT NULL COMMENT '更新用户' ,
    update_org varchar(100) NOT NULL COMMENT '更新组织' ,
    update_time BIGINT NOT NULL COMMENT '更新时间' ,
    PRIMARY KEY(id) ,
    INDEX idx_enable(enable) ,
    INDEX idx_module(module) ,
    INDEX idx_update_user(update_user) ,
    INDEX idx_update_org(update_org) ,
    INDEX idx_update_time(update_time)
)ENGINE=innodb DEFAULT CHARSET=utf8;