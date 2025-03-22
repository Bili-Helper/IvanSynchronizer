## Rocket-MQ
```bash
docker run -d \
--privileged=true \
--name rmqnamesrv \
-p 9876:9876 \
-v /volume2/zero/Config/rocketmq/nameserver/logs:/home/rocketmq/logs \
-v /volume2/zero/Config/rocketmq/nameserver/store:/root/store \
-e "MAX_HEAP_SIZE=256M" \
-e "HEAP_NEWSIZE=128M" \
-e "MAX_POSSIBLE_HEAP=100000000" \
apache/rocketmq sh mqnamesrv
```

## Broker Config
```
vi /volume2/zero/Config/rocketmq/broker/conf/broker.conf

# 集群名称
brokerClusterName=DefaultCluster
# 节点名称
brokerName=broker-a
# broker id节点ID， 0 表示 master, 其他的正整数表示 slave，不能小于0
brokerId=0
# Broker服务地址    String    内部使用填内网ip，如果是需要给外部使用填公网ip
brokerIP1=192.168.2.115
# Broker角色
brokerRole=ASYNC_MASTER
# 刷盘方式
flushDiskType=ASYNC_FLUSH
# 在每天的什么时间删除已经超过文件保留时间的 commit log，默认值04
deleteWhen=04
# 以小时计算的文件保留时间 默认值72小时
fileReservedTime=72
# 是否允许Broker 自动创建Topic，建议线下开启，线上关闭
autoCreateTopicEnable=true
# 是否允许Broker自动创建订阅组，建议线下开启，线上关闭
autoCreateSubscriptionGroup=true
# 磁盘使用达到95%之后,生产者再写入消息会报错 CODE: 14 DESC: service not available now, maybe disk full
diskMaxUsedSpaceRatio=95
```

### MQ-Broker
```
docker run -d \
--name rmqbroker \
--link rmqnamesrv:namesrv \
-p 10911:10911 -p 10909:10909 \
--privileged=true \
-v /volume2/zero/Config//rocketmq/broker/logs:/root/logs \
-v /volume2/zero/Config//rocketmq/broker/store:/root/store \
-v /volume2/zero/Config//rocketmq/broker/conf/broker.conf:/home/rocketmq/broker.conf \
-e "MAX_POSSIBLE_HEAP=200000000" \
-e "MAX_HEAP_SIZE=512M" \
-e "HEAP_NEWSIZE=256M" \
-e "NAMESRV_ADDR=namesrv:9876" \
apache/rocketmq \
sh mqbroker -c /home/rocketmq/broker.conf
```

## Dashboard
```
docker run -p 8087:8080 --name rocketmq-console -d \
-e "JAVA_OPTS=-Drocketmq.namesrv.addr=192.168.2.115:9876" \
-t apacherocketmq/rocketmq-dashboard
```
