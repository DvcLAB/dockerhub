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
#### 卸载旧版本
```dtd
sudo apt-get purge docker-ce docker-ce-cli containerd.io && \
sudo rm -rf /var/lib/docker && \
sudo rm -rf /var/lib/containerd
```
#### 安装Docker
1. 添加源/访问证书
```dtd
sudo apt-get update && \
sudo apt-get install \
        apt-transport-https \
        ca-certificates \
        curl \
        software-properties-common
```
2. 使用脚本自动安装
```dtd
curl -fsSL get.docker.com -o get-docker.sh && \
sudo sh get-docker.sh --mirror Aliyun
```
3. 创建Docker用户组
```dtd
sudo groupadd docker && \
sudo usermod -aG docker $USER
```
4. 启动Docker
```dtd
sudo systemctl enable docker && \
sudo systemctl start docker
```
5. 使用国内镜像
修改 /etc/docker/daemon.json
```dtd
{
  "registry-mirrors": ["http://hub-mirror.c.163.com"]
}
```
重启Docker
```dtd
sudo service docker restart
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
假设服务器IP地址为${ip}，本地根域名为rr。访问Webmin管理界面，地址为：https://${ip}:10000/，默认用户名：root，密码：password
1. BIND DNS Server 的 DNS Zones 的配置文件位于： Others ⇒ File Manager ⇒ /var/lib/bind 目录下
2. 快速导入现有DNS Zones配置文件：
   * 将配置文件上传到Others ⇒ File Manager ⇒ /var/lib/bind 目录下
   * 编辑/etc/bind/named.conf.local文件，添加如下部分：
   ```dtd
   zone "dvc" {
	  type master;
	  file "/var/lib/bind/dvc.hosts";
   };
   zone "7.0.10.in-addr.arpa" {
      type master;
      file "/var/lib/bind/10.0.7.rev";
   };
   ```
3. 配置文件说明：
   * Zone类型为Forward的转发区域配置文件：dvc.hosts
   * Zone类型为Reverse的反向区域配置文件：10.0.7.rev







### MySQL
#### 部署
```dtd
mkdir -p /opt/mysql/db /opt/mysql/log /opt/mysql/conf && \
echo '[mysqld]
   character-set-server = utf8mb4
   wait_timeout = 31536000
   max_allowed_packet = 128M
   group_concat_max_len = 1638400
   default-time-zone = '+8:00'
   ' > /opt/mysql/conf/my.cnf && \

echo 'version: "2"
services: 
  mysql:
    restart: always
    image: mysql:5.7
    container_name: mysql_test
    volumes:
      - /opt/mysql/db:/var/lib/mysql
      - /opt/mysql/logs:/var/log/mysql
      - /opt/mysql/conf/:/etc/mysql/conf.d
    ports:
      - 3306:3306
    environment:
      - MYSQL_ROOT_PASSWORD=${password}' > /opt/mysql/mysql.yaml && \
docker-compose -f /opt/mysql/mysql.yaml up -d
```
#### HeidiSQL

### Redis
#### 部署
```dtd
mkdir -p /opt/redis/data && \
touch /opt/redis/redis.conf && \
echo 'bind *
   protected-mode yes
   port 6379
   tcp-backlog 511
   timeout 0
   tcp-keepalive 300
   daemonize no
   supervised no
   pidfile /var/run/redis_6379.pid
   loglevel notice
   logfile ""
   databases 16
   always-show-logo yes
   save 900 1
   save 300 10
   save 60 10000
   stop-writes-on-bgsave-error yes
   rdbchecksum yes
   dbfilename dump.rdb
   # Default cluster config
   slave-serve-stale-data yes
   slave-read-only yes
   repl-diskless-sync no
   repl-diskless-sync-delay 5
   repl-disable-tcp-nodelay no
   slave-priority 100
   # password
   requirepass ${password}
   lazyfree-lazy-eviction no
   lazyfree-lazy-expire no
   lazyfree-lazy-server-del no
   slave-lazy-flush no 
   appendonly no
   appendfilename "appendonly.aof"
   appendfsync everysec
   no-appendfsync-on-rewrite no
   auto-aof-rewrite-percentage 100
   auto-aof-rewrite-min-size 64mb
   aof-load-truncated yes
   aof-use-rdb-preamble no
   lua-time-limit 5000
   slowlog-log-slower-than 10000
   slowlog-max-len 128
   latency-monitor-threshold 0
   notify-keyspace-events ""
   hash-max-ziplist-entries 512
   hash-max-ziplist-value 64
   list-max-ziplist-size -2
   list-compress-depth 0
   set-max-intset-entries 512
   zset-max-ziplist-entries 128
   zset-max-ziplist-value 64
   hll-sparse-max-bytes 3000
   activerehashing yes
   client-output-buffer-limit normal 0 0 0
   client-output-buffer-limit slave 256mb 64mb 60
   client-output-buffer-limit pubsub 32mb 8mb 60
   hz 10
   aof-rewrite-incremental-fsync yes' > /opt/redis/redis.conf && \


echo 'version: "3"
services:
   redis:
     image: redis
     restart: always
     container_name: redis_test
     ports:
       - "6379:6379"
     volumes:
       - /opt/redis/redis.conf:/etc/redis/redis.conf 
       - /opt/redis/data:/data      
     command: redis-server /etc/redis/redis.conf 
     privileged: true' > /opt/redis/redis.yaml && \
docker-compose -f /opt/redis/redis.yaml up -d
```
#### redisclient

### zookeeper/Kafka
#### 安装启动
```dtd
mkdir -p /opt/kafka/data /opt/zookeeper/data && \
echo 'version: "3.7"
services:
  zookeeper:
    image: wurstmeister/zookeeper
    volumes:
       - /opt/zookeeper/data:/data
    container_name: zookeeper
    mem_limit: 1024M
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - 2181:2181
    restart: always
  kafka_node1:
    image: wurstmeister/kafka
    container_name: kafka_node1
    mem_limit: 1024M
    depends_on:
      - zookeeper
    ports:
      - 61205:9092
    volumes:
      - /opt/kafka/data:/kafka
    environment:
      KAFKA_CREATE_TOPICS: "test"
      KAFKA_BROKER_NO: 0
      KAFKA_LISTENERS: PLAINTEXT://kafka_node1:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://10.107.1.168:61205
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_HEAP_OPTS: "-Xmx512M -Xms16M"
    restart: always' > /opt/kafka/kafka.yml && \
docker-compose -f /opt/kafka/kafka.yml up -d
```
#### Kafka-Manager安装启动
```dtd
apt install unzip -y && cd /opt && wget http://reid.red:233/f/kafka-manager-2.0.0.2.zip && \
unzip kafka-manager-2.0.0.2.zip && \
sed -i "s/kafka-manager-zookeeper:2181/rr51:2181/" /opt/kafka-manager-2.0.0.2/conf/application.conf && \
sed -i "1i\http.port=9001" /opt/kafka-manager-2.0.0.2/conf/application.conf && \
cd /opt/kafka-manager-2.0.0.2/ && nohup bin/kafka-manager &
```
参考https://reid.red:233/w/Hadoop/HBase/Kafka_%E6%90%AD%E5%BB%BA#Kafka_Manager

### KeyCloak

#### 部署
1. 准备相应文件
   * /opt/keycloak/realm-export.json
   * /opt/keycloak/keycloak-services-social-weixin-0.0.19.jar
   * /opt/keycloak/realm-identity-provider-weixin.html
   * /opt/keycloak/realm-identity-provider-weixin-ext.html
2. 部署启动
```dtd
mkdir -p /opt/keycloak/mysql/data && \
docker network create keycloak-network && \


echo ‘version: "3"
services:
    keycloakmysql:
        image: mysql:8.0.21
        container_name: keycloakmysql
        hostname: keycloakmysql
        volumes:
            - /opt/keycloak/mysql/data:/var/lib/mysql
        privileged: true
        environment:
            - MYSQL_DATABASE=keycloak
            - MYSQL_USER=keycloak
            - MYSQL_PASSWORD=password
            - MYSQL_ROOT_PASSWORD=root_password
            
    keycloak:
        image: jboss/keycloak:12.0.4
        container_name: keycloak
        hostname: keycloak
        restart: always
        links: 
            - keycloakmysql
        ports:
            - 8080:8080
        volumes:
            - /opt/keycloak/realm-export.json:/tmp/realm-export.json
            - /opt/keycloak/keycloak-services-social-weixin-0.0.19.jar:/opt/jboss/keycloak/providers/keycloak-services-social-weixin-0.0.19.jar
            - /opt/keycloak/realm-identity-provider-weixin.html:/opt/jboss/keycloak/themes/base/admin/resources/partials/realm-identity-provider-weixin.html
            - /opt/keycloak/realm-identity-provider-weixin-ext.html:/opt/jboss/keycloak/themes/base/admin/resources/partials/realm-identity-provider-weixin-ext.html
        environment:
            - KEYCLOAK_USER=admin
            - KEYCLOAK_PASSWORD=${password}
            - DB_VENDOR=mysql
            - DB_ADDR=keycloakmysql
            - DB_PORT=3306
            - DB_DATABASE=keycloak
            - DB_USER=keycloak
            - DB_PASSWORD=password
            - PROXY_ADDRESS_FORWARDING=true
            - KEYCLOAK_IMPORT="/tmp/realm-export.json -Dkeycloak.profile.feature.upload_scripts=enabled"
        command:
            [   
                 "-b 0.0.0.0",
                 "-Dkeycloak.profile.feature.upload_scripts=enabled",
                 "-Dkeycloak.import=/tmp/realm-export.json",
                 "-Dkeycloak.profile.feature.docker=enabled",
                 "-Dkeycloak.profile.feature.token_exchange=enabled",
                 "-Dkeycloak.profile.feature.admin_fine_grained_authz=enabled"
            ]
        depends_on:
            - keycloakmysql
networks:
    default:
        external:
            name: keycloak-network’ > /opt/keycloak/docker-compose-keycloak.yml && \

docker-compose -f /opt/keycloak/docker-compose-keycloak.yml up -d keycloakmysql && \
docker-compose -f /opt/keycloak/docker-compose-keycloak.yml up -d keycloak
```

### Docker Registry
#### 部署
1. 准备文件
```dtd
mkdir -p /opt/docker_registry/config /opt/docker_registry/data /opt/docker_registry/ssl && \
touch /opt/docker_registry/config/config.yml
```
   * 将registry.33.dvc域名所对应的ssl证书文件（.pem和.key）添加至路径/opt/docker_registry/ssl下
   * 登录KeyCloak >> DvcLAB(realm) >> Clients >> docker-registry >>Installation >> Docker Compose Yaml >>Download，将下载文件中的localhost_trust_chain.pem文件添加至路径/opt/docker_registry/ssl下

参考：
   * [Configure a registry](https://docs.docker.com/registry/configuration/#token)
   * [Docker Authentication with Keycloak](https://developers.redhat.com/blog/2017/10/31/docker-authentication-keycloak#)

2. 配置文件
```dtd
echo 'version: 0.1
log:
  fields:
    service: registry
storage:
  delete:
    enabled: true
  cache:
    blobdescriptor: inmemory
  filesystem:
    rootdirectory: /var/lib/registry
auth:
  token:
    realm: https://auth.33.dvc/auth/realms/DvcLAB/protocol/docker-v2/auth
    service: docker-registry
    issuer: https://auth.33.dvc/auth/realms/DvcLAB
    rootcertbundle: /root/localhost_trust_chain.pem
http:
  addr: :5000
  tls:
    certificate: /root/cert.pem
    key: /root/cert.key
  headers:
    X-Content-Type-Options: [nosniff]
  health:
    storagedriver:
      enabled: true
      interval: 10s
threshold: 3'   > /opt/docker_registry/config/config.yml
```

3. 启动容器
```dtd
echo  ‘version: '3.7'
services:
  docker_registry:
    image: registry:2.3
    container_name: registry
    ports:
      - 61642:5000
    volumes:
      - /opt/docker_registry/config:/etc/docker/registry
      - /opt/docker_registry/data:/var/lib/registry
      - /opt/docker_registry/ssl/localhost_trust_chain.pem:/root/localhost_trust_chain.pem:ro
      - /opt/docker_registry/ssl/registry.33.dvc.pem:/root/cert.pem:ro
      - /opt/docker_registry/ssl/registry.33.dvc.key:/root/cert.key:ro
    restart: always’ > /opt/docker_registry/registry.yaml && \
docker-compose -f /opt/docker_registry/registry.yaml up -d
```

4. 对于自签发ssl证书需要通过配置解决 x509: certificate signed by unknown authority
   * 首先生成的自签发证书必须具有参数subjectAltName
   * 将CA根证书（.crt或.pem文件）复制并重命名为ca.crt到 /etc/docker/certs.d/registry.33.dvc:443/ca.crt（无需重启Docker）
   * 若上述依然没解决问题，执行以下步骤:将CA根证书（.crt文件，.pem文件不识别转为.crt文件）添加到/usr/local/share/ca-certificates/路径下
   
   执行一下命令：
   ```dtd
    update-ca-certificates
    service docker restart
   ```
   
   参考：
      * [Logging into your docker registry fails with x509 certificate signed by unknown authority error](https://www.ibm.com/docs/en/cloud-paks/cp-management/2.2.x?topic=tcnm-logging-into-your-docker-registry-fails-x509-certificate-signed-by-unknown-authority-error)
      * [Use self-signed certificates](https://docs.docker.com/registry/insecure/)


### Pypi Server
#### 部署
```dtd
docker pull pypiserver/pypiserver && \
apt-get install -y apache2-utils && \
sudo pip3 install passlib && \
mkdir -p /opt/pypiserver/auth /opt/pypiserver/packages && \
chmod -R 666 /opt/pypiserver/packages
```

会 prompt 密码输入，重复两遍一样的
```dtd
cd /opt/pypiserver/auth && htpasswd -sc .htaccess ${username}
```

docker容器部署
```dtd
docker run -d -p ${port}:8080 --restart=always --name=pypiserver \
  -v /opt/pypiserver/packages/:/data/packages \
  -v /opt/pypiserver/auth:/data/auth/ \
  pypiserver/pypiserver -P /data/auth/.htaccess -a update /data/packages
```

### Bandersnatch Server

### Ceph
1. 移除需要加入集群硬盘的GPT分区信息
   参考：[How to remove GPT from HDD?](https://askubuntu.com/questions/211477/how-to-remove-gpt-from-hdd)
2. 部署
   
   2.1. 配置文件
   ```dtd
   cat <<EOF > /opt/ceph.conf
   [global]
   osd crush chooseleaf type = 0
   mon_max_pg_per_osd = 1000
   
   [client.radosgw.gateway]
   rgw sts key = abcdefghijklmnop
   rgw s3 auth use sts = true
   rgw enable usage log = true
   
   rgw usage log tick interval = 1
   rgw usage log flush threshold = 1
   rgw usage max shards = 32
   
   rgw usage max user shards = 1
   
   [client.rgw.psiai.xian]
   rgw sts key = abcdefghijklmnop
   rgw s3 auth use sts = true
   rgw sts token introspection_url = ${keycloak_introspection}
   rgw enable usage log = true
   rgw enable ops log = true
   rgw usage log tick interval = 1
   rgw usage log flush threshold = 1
   
   [client.rgw]
   rgw enable usage log = true
   rgw enable ops log = true
   rgw usage log tick interval = 1
   rgw usage log flush threshold = 1
   EOF
   ```
   注：
      * osd crush chooseleaf type = 0 只在单机环境下设置，生产环境中应设置为1，即在不同host间备份数据。
      * mon_max_pg_per_osd = 1000 ,如果不设置，部署 Rados 网关后使用 s3cmd上传数据时，上传失败并显示“ S3 错误：416 (InvalidRange) ”。
      * ${keycloak_introspection}的格式为https://${keycloak_url}/auth/realms/${realm}/protocol/openid-connect/token/introspect 
   
   2.2. 下载执行安装脚本
   ```dtd
   curl --silent --remote-name --location https://github.com/ceph/ceph/raw/octopus/src/cephadm/cephadm && \
   chmod +x cephadm && \
   mkdir -p /etc/ceph && \
   ./cephadm bootstrap -c /opt/ceph.conf --mon-ip ${ip}
   ```
   安装成功后输出：
   ```dtd
   INFO:cephadm:Ceph Dashboard is now available at:
   
                URL: https://${host}:8443/
               User: admin
           Password: qsuwhhv1ey
   
   INFO:cephadm:You can access the Ceph CLI with:
   
           sudo ./cephadm shell --fsid 20363b38-ee0f-11eb-9695-8d43afb88ac7 -c /etc/ceph/ceph.conf -k /etc/ceph/ceph.client.admin.keyring
   
   INFO:cephadm:Bootstrap complete.
   ```
      * https://${ip}:8443/是Web控制台，第一次访问强制重设密码；
   
   2.3. 添加环境路径
   ```dtd
   # 使用 exit 命令退出ceph CLI后执行，执行完后重新进入ceph CLI
   ./cephadm install
   ```
   2.4. 增加节点
   ```dtd
   ssh-copy-id -f -i /etc/ceph/ceph.pub root@${new_ip}
   ceph orch host add ${new_ip}
   ceph orch host ls
   ```
   2.5. 添加osd
   ```dtd
   ceph orch device ls # 初始列表中是可用的设备，并不代表已添加
   ceph orch daemon add osd ${host}:${path} // rr60:/dev/sdb
   ```
   2.6. 部署RGE
   ```dtd
   radosgw-admin realm create --rgw-realm=${realm} --default
   radosgw-admin zonegroup create --rgw-zonegroup=default --master --default
   radosgw-admin zone create --rgw-zonegroup=default --rgw-zone=${zone} --master --default
   ceph orch apply rgw ${realm} ${zone}
   #创建系统用户
   radosgw-admin user create --uid=rr --display-name="rr" --system
   radosgw-admin caps add --uid=rr --caps="roles=*"
   #查看用户信息
   radosgw-admin user info --uid=rr
   ```
   2.7. 记录${access_key}和${secret_key}，并更新Web控制台配置
   ```dtd
   mkdir -p /etc/ceph && \
   echo '${access_key}' > /etc/ceph/access_key && \
   echo '${secret_key}' > /etc/ceph/secret_key && \
   ceph dashboard set-rgw-api-access-key -i /etc/ceph/access_key && \
   ceph dashboard set-rgw-api-secret-key -i /etc/ceph/secret_key && \
   ceph dashboard set-rgw-api-ssl-verify False && \
   radosgw-admin period update --rgw-realm=${realm} --commit
   ```
   RGW默认S3服务端口为：80
   ![](https://github.com/DvcLAB/dockerhub/blob/main/images/s3.png)
   
   
#### Ceph Dashboard

### Prometheus

### OpenResty
1. 配置OpenResty的Docker容器并启动
   ```dtd
   echo 'version: "3"
      services:
      redis:
      image: redis
      restart: always
      volumes:
      - /opt/redis/redis.conf:/etc/redis/redis.conf
      command: redis-server /etc/redis/redis.conf
      ports:
      # redis对内暴露6379端口
      - "61379:6379"
      container_name: openresty-redis
      openresty:
      image: openresty/openresty
      restart: always
      depends_on:
      - redis
      container_name: openresty
      volumes:
      - /opt/openresty/ssl/:/etc/nginx/ssl/
      - /opt/openresty/conf.d/:/etc/nginx/conf.d/
      - /opt/openresty/lua/:/usr/local/openresty/nginx/lua/
      - /opt/www/dvclab:/etc/nginx/dist
      ports:
      - "443:443"
      - "80:80"
   ' > /opt/openresty/openresty.yaml && \
   docker-compose -f /opt/openresty/openresty.yaml up -d
   ```
2. 自签发域名证书
   * 安装OpenSSL
   ```dtd
   apt-get install openssl
   ```
   
   * 生成CA根证书
   ```dtd
   cd /opt/CA
   # 生成CA私钥
   openssl genrsa -des3 -out rootCA.key 4096
   #执行命令后，需要输入一个密码作为私钥的密码，后续通过该私钥来生成或者签发证书都要用到这个私钥。

   # 生成私钥后，执行以下命令生成CA证书
   openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 3560 -out rootCA.crt
   # 执行后会进入交互式界面，需要输入CN相关信息。等待根证书生成后，一个CA也就创建完成了，此时要做的是生成用户证书，并使用CA证书对它签名，然后就可以用了。
   ```
   
   * 生成用户证书
   ```dtd
   # 1. 生成用户证书的私钥
   openssl genrsa -out 33.dvc.key 2048

   # 2. 生成csr，csr是一个证书签名的请求，后面生成证书需要使用这个csr文件
   openssl req -new -key 33.dvc.key -out 33.dvc.csr
   # 这一步也需要输入地区、CN等相关的信息(A challenge password []:dvclab)

   # 3. 创建ext.ini文件，内容如下（设置subjectAltName参数）：
   echo '
      basicConstraints = CA:FALSE
      keyUsage = nonRepudiation, digitalSignature, keyEncipherment
      subjectAltName = @alt_names

      [alt_names]
      DNS.1 = *.33.dvc
      DNS.2 = 33.dvc
   '  > /opt/CA/ssl/ext.ini

   # 4. 使用CA证书签发用户证书
   openssl x509 -req -in 33.dvc.csr -CA /opt/CA/rootCA.crt -CAkey /opt/CA/rootCA.key -CAcreateserial -extfile ext.ini -out 33.dvc.crt -days 3650

   ```
3. 配置服务转发
   以KeyCloak为例
   ```dtd
   echo ' server {
       listen          443 ssl;
       server_name     auth.33.dvc;
   
       #ssl证书文件位置(常见证书文件格式为：crt/pem)
       ssl_certificate      /etc/nginx/ssl/auth-cert.pem;
   
       #ssl证书key位置
       ssl_certificate_key  /etc/nginx/ssl/auth-cert.key;
       ssl_session_timeout  10m;
       ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
       ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
       ssl_prefer_server_ciphers  on;
   
       location / {
           # 是后端知晓自己的服务地址，便于进行页面跳转，如果不设置会跳转www.dvclab.com:61174
           proxy_set_header  Host  $host;
           proxy_set_header  X-Forwarded-Proto $scheme;
           proxy_set_header  X-Forwarded-For $host;
           proxy_set_header  Upgrade $http_upgrade;
           proxy_set_header  Connection upgrade;
           proxy_set_header  X-Real-IP $remote_addr;
   
           #配置跨域访问
           add_header Access-Control-Allow-Headers Access-Control-Allow-Origin,Content-Type,Authorization;
           proxy_hide_header access-control-allow-origin;
           add_header Access-Control-Allow-Origin $http_origin;
           add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
           add_header Access-Control-Expose-Headers Access-Control-Allow-Origin,Access-Control-Allow-Credentials;
           # change frp config for security
           proxy_pass    http://10.0.7.33:8080;
       }
   }' > /opt/openresty/conf.d/keycloak.conf
   ```
4. 配置动态容器转发
   
   4.1. lua脚本
   定义拆分字符串方法，例如：
   
      e.g. /a/b/c 
   
      table[1] a
   
      table[2] b
   
      table[3] c

   ```dtd
   echo '
      function split(str, pat)
      local t = {}
      local fpat = "(.-)" .. pat
      local last_end = 1
      local s, e, cap = str:find(fpat, 1)
      while s do
      if s ~= 1 or cap ~= "" then
      table.insert(t, cap)
      end
      last_end = e + 1
      s, e, cap = str:find(fpat, last_end)
      end
      if last_end <= #str then
      cap = str:sub(last_end)
      table.insert(t, cap)
      end
      return t
      end
      
      function split_path(str)
      return split(str, “[\\/]+”)
      end
   ' > /opt/openresty/lua/split.lua
   ```
   定义Redis查询逻辑
   ```dtd
   echo '
      -- redis结果解析,导入redis.parser脚本
      local parser = require "redis.parser"
      
      -- ngx.var.uri只包含路径参数，不包含主机与端口
      ngx.log(ngx.EMERG, "ngx.var.uri---->", ngx.var.uri)
      local parameters = split_path(ngx.var.uri)
      
      -- 访问的是根路径
      if(#parameters == 0) then
      ngx.exit(ngx.HTTP_FORBIDDEN)
      end
      
      
      -- user_id = parameters[2]
      -- container_id = parameters[4]
      container_id = parameters[2]
      
      -- ngx.log(ngx.EMERG, "user_id--->", user_id)
      ngx.log(ngx.EMERG, "container_id--->", container_id)
      
      -- 查询参数
      key = "DynamicRoute"
      -- id = user_id .. "_" .. container_id
      -- id = "\"" .. user_id .. "_" .. container_id .. "\""
      id = "\"" .. container_id .. "\""
      ngx.log(ngx.EMERG, "id--->", id)
      
      -- 向redis查询
      res = ngx.location.capture(
      "/redis", { args = { key = key, id = id } }
      )
      
      -- 查询失败
      if res.status ~= 200 then
      ngx.log(ngx.ERR, "redis server returned bad status: ",
      res.status)
      ngx.exit(res.status)
      end
      
      -- 结果为空
      if not res.body then
      ngx.log(ngx.ERR, "redis returned empty body")
      ngx.exit(500)
      end
      
      -- raw tcp response from redsi server
      -- 共2条返回所以应该使用parse_replies(res.body, 2)
      -- OK
      -- 172.17.144.4:8080
      ngx.log(ngx.EMERG, "raw response ----->", res.body)
      
      local results = parser.parse_replies(res.body, 2)
      for i, result in ipairs(results) do
      if i == 2 then
      server = result[1]
      typ = result[2]
      end
      end
      
      
      -- 检查结果类型
      if typ ~= parser.BULK_REPLY or not server then
      ngx.exit(500)
      end
      
      
      -- 返回value为空
      if server == "" then
      server = "default.com"
      end
      
      -- \"172.17.144.4:432\"
      
      local index = string.find(server, "\"", 2)
      
      result = string.sub(server, 2 ,index - 1)
      
      ngx.var.target = result
      
      ngx.log(ngx.EMERG, "key--->", key)
      ngx.log(ngx.EMERG, "id--->", id)
      ngx.log(ngx.EMERG, "service--->", result)
   ' > /opt/openresty/lua/query_redis.lua
   ```
   
   4.2. nginx配置文件
   ```dtd
   echo '# 启用主进程后，在每次Nginx工作进程启动时运行指定的Lua代码
   client_max_body_size 0;
   init_worker_by_lua_file /usr/local/openresty/nginx/lua/split.lua;
   
   server {
   listen       443;
   server_name  j.33.dvc;
   
       location = /redis {
           internal;
           redis2_query auth ${redis_password};
           set_unescape_uri $id $arg_id;
           set_unescape_uri $key $arg_key;
           redis2_query hget $key $id;
           redis2_pass 10.107.1.10:61379;
       }
       # 一般跳转
       location /index/ {
          proxy_read_timeout 86400s;
          proxy_send_timeout 86400s;
          proxy_set_header  Host  $host;
          proxy_set_header  X-Forwarded-For $host;
          proxy_set_header  Upgrade $http_upgrade;
          proxy_set_header  Connection 'Upgrade';
          proxy_set_header  X-Forwarded-Proto $scheme;
          proxy_set_header  X-Real-IP $remote_addr;
          proxy_http_version 1.1;
          proxy_pass http://10.107.1.10:50000/;
       }
   
       location / {
          # 设置一个内嵌脚本的共享变量
          set $target '';   
          # 引入内嵌脚本
          access_by_lua_file /usr/local/openresty/nginx/lua/query_redis.lua;
          # 内嵌脚本结束
          resolver 8.8.8.8;
          # 进行请求转发（反向代理）
          proxy_set_header  Host  $host;
          proxy_set_header  X-Forwarded-For $host;
          proxy_set_header  Upgrade $http_upgrade;
          proxy_set_header  Connection 'Upgrade';
          proxy_set_header  X-Forwarded-Proto $scheme;
          proxy_set_header  X-Real-IP $remote_addr;
          proxy_http_version 1.1;
          proxy_pass http://$target$request_uri;
       #    proxy_redirect /lab? https://$host$uri/lab?;
       }
   }
   > /opt/openresty/conf.d/dynamicRouter.conf

   ```
   注意：当通过bind9给对应的内网服务配置域名后，应重启bind9对应Docker容器；

### wireguard


