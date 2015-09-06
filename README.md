简单易用的服务框架
===

##功能

1. 支持HTTP、WebSocket和EventBus三种RPC通道及其透明调用
1. 集群特性，支持多实例自动负载均衡
1. 支持JDBC和Cache（由Redis支撑）的CURD脚手架
1. 支持RBAC认证功能
1. 支持定时任务
1. 支持多种常用的分布式服务
      1. 分布式CountDownLatch服务
      1. 分布式计数器服务
      1. 分布式锁服务
      1. 分布式队列服务
      1. 分布式阻塞队列服务
      1. 分布式调度服务
      1. 分布式消息订阅服务
1. Restful设计，所有业务都抽象成对资源的操作      
1. 类库形式，侵入性低，集成友好
1. 支持链式编程风格，使用方便
1. 支持基于注解的服务注册
1. 支持文件上传
1. client支持异步与同步模式
1. 支持Json（推荐Json格式）与Xml

##Maven

    <dependency>
        <groupId>com.ecfront</groupId>
        <artifactId>ez-framework</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

##示例

   https://github.com/gudaoxuri/ez-framework/wiki

### Check out sources
`git clone https://github.com/gudaoxuri/ez-framework.git`

### License

Under version 2.0 of the [Apache License][].

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
