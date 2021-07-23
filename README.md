# 项目简介

1. 轻量级AI容器+JupyterLab私有化快速部署服务，支持自有主机部署与公共服务器部署
2. 基于Vue + sparkJava 构建的前后端分离项目

# 项目在线访问与使用
[DockerHub](https://ai.dvclab.com)

## 功能特性

1. 在线jupyterlab开发
![](https://github.com/DvcLAB/dockerhub/blob/main/images/jupyterlab.png)

2. 支持Docker Compose配置文件下载在私有主机运行，由平台管理，远程访问
![](https://github.com/DvcLAB/dockerhub/blob/main/images/docker-compose.png)

3. 基于Keycloak的统一认证管理(支持微信登录与Github登录)
    - 用户登录
    - 用户访问资源（容器、项目、数据集..）
![](https://github.com/DvcLAB/dockerhub/blob/main/images/login.png)

4. 在线项目管理
![](https://github.com/DvcLAB/dockerhub/blob/main/images/project.png)
   
5. 在线数据集管理
![](https://github.com/DvcLAB/dockerhub/blob/main/images/dataset.png)
   
6. 镜像管理
![](https://github.com/DvcLAB/dockerhub/blob/main/images/image.png)
   
7. 在线容器管理
![](https://github.com/DvcLAB/dockerhub/blob/main/images/container.png)

# 配置文件
## Conf
1. DockerHost.conf
    * 容器所在主机相关配置
2. DockerHubServer.conf
    * 主服务配置
3. ESClient.conf
    * ElasticSearch连接配置
4. HBaseClient.conf
    * HBase连接配置
5. IpDetector.conf
    * IP检测器公网验证地址配置
6. KafkaClient.conf
    * Kafka集群连接配置
7. KeycloakAdapter.conf
    * Keycloak统一认证服务配置
8. MongoDBAdapter.conf
    * MongoDB连接配置
9. PooledDataSource.conf
    * RDBMS数据库连接配置
10. PypiServerUpdater.conf
    * bandersnatch python镜像源服务器连接配置
11. RedissonAdapter.conf
    * Redis集群连接配置
12. Requester.conf
    * nio requester相关参数
13. ResourceInfoFetcher.conf
    * 外部资源采集器相关参数
14. S3Adapter.conf
    * 对象存储服务配置
    
# 测试环境安装
## 系统环境安装
### 安装 Ubuntu server 20.04 系统
1. 官网下载[Ubuntu server 20.04系统镜像](https://releases.ubuntu.com/20.04.2/ubuntu-20.04.2-live-server-amd64.iso)
2. 创建U盘安装盘[Win32DiskImager](https://sourceforge.net/projects/win32diskimager/) 或 [UltraISO](https://www.ultraiso.com/)
3. [安装Ubuntu server 20.04](https://www.jianshu.com/p/da49cd69e8ff)

#### 配置网络

修改配置文件
```dtd
echo 'network:
  ethernets:
    # 配置的网卡的名称
    eth0:
      # 关闭DHCP，如果需要打开DHCP则写true
      dhcp4: false  
      addresses:
        # 配置的静态ip地址和掩码
        - aa.bb.cc.dd/24
      optional: true
      # 网关地址
      gateway4: aa.bb.cc.1  
      nameservers:
        # DNS服务器地址
        addresses:          
          - 8.8.8.8
          - 8.8.4.4 
  version: 2' > /etc/netplan/netplan.yaml && \
sudo netplan apply
```
#### 配置APT更新源
```dtd
echo 'deb http://mirrors.aliyun.com/ubuntu/ focal main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ focal main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ focal-security main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ focal-security main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ focal-updates main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ focal-updates main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ focal-proposed main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ focal-proposed main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ focal-backports main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ focal-backports main restricted universe multiverse
' > /etc/apt/sources.list && \
sudo apt update && \
sudo apt upgrade
```
### 安装Docker
```dtd
sudo apt-get purge docker-ce docker-ce-cli containerd.io && \
sudo rm -rf /var/lib/docker && \
sudo rm -rf /var/lib/containerd && \
sudo apt-get update && \
```
## 软件环境安装

### Bind9
#### 安装
```dtd
docker pull sameersbn/bind:9.16.1-20200524 && \
sudo service systemd-resolved stop && \
sudo systemctl disable systemd-resolved && \
docker run --name bind -d --restart=always \
  --publish 53:53/tcp --publish 53:53/udp --publish 10000:10000/tcp \
  --volume /srv/docker/bind:/data \
  sameersbn/bind:9.16.1-20200524
```
1. 修改/etc/resolv.conf，设置nameserver 127.0.0.1
2. Ubuntu20.04更改/etc/netplan/00-installer-config.yaml文件中的nameservers为容器所在宿主机的IP，然后保存，执行netplan apply即可更新DNS配置

#### 配置
假设服务器IP地址为10.0.0.41，本地根域名为rr。访问Webmin管理界面，地址为：https://10.0.0.41:10000/，默认用户名：root，密码：password，相关设置如下：
1. Servers → BIND DNS Server → Global Server Options → Access Control Lists，添加： 
   1. allow-query any
2. Servers → BIND DNS Server → Global Server Options → Forwarding and Transfers → Global forwarding and zone transfer options，添加转发dns服务器IP地址： 
   1. 8.8.8.8
   2. 8.8.4.4
   3. 暂时只添加了Google的DNS。添加其他的一些国内的DNS（如AliDNS），反而会有问题（ntp 服务器访问失败等等）
3. Servers → BIND DNS Server → Existing DNS Zones →  Create Master Zone
   1. Zone type: Forward (Names to Addresses)
   2. Domain name / Network: rr
   3. Master server: 41.rr
   4. Email address: admin@rr
4. Servers → BIND DNS Server → Existing DNS Zones →  Create Master Zone
   1. Zone type: Reverse (Addresses to Names)
   2. Domain name / Network: 10.0.0
   3. Master server: 41.rr
   4. Email address: admin@rr
5. Servers → BIND DNS Server → Existing DNS Zones → rr
   1. Address中添加DNS记录
        * Name: 41，Address: 10.0.0.41，点击Create，会自动添加并更新逆向地址记录
        * 按需添加其他DNS记录（可能需要重启容器才会使新添加的DNS记录生效）
   2. Name Server确认存在域名服务器地址
        * Zone Name: rr
        * Name Server: 41.rr.





### MySQL
#### HeidiSQL

### Redis
#### redisclient

### zookeeper

### Kafka
#### Kafka-Manager
1. https://reid.red:233/w/Hadoop/HBase/Kafka_%E6%90%AD%E5%BB%BA#Kafka_Manager

### KeyCloak
1. realm / client / role / user 设置
2. 是否需要添加 WX / Github Identity Provider （需要单独申请，与生产环境不同）
3. 测试：引入KeyCloakAdapter(common项目中)，本项目补充测试用例

### Pypi Server
1. 测试：命令行

### Bandersnatch Server
1. 测试

### Ceph

#### Ceph Dashboard

### Prometheus

### OpenResty
1. 配置服务转发
2. 配置动态容器转发

## VPN配置
