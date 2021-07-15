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