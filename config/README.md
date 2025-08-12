## 项目启动流程

### 在虚拟机上配置好jdk1.8.0_461，maven和scala2.12.13。jdk版本不能太低，8u202就不可以。

### 把maven的settings.xml复制到~/.m2/，里面是阿里源的镜像。

```shell
cp config/settings.xml ~/.m2/
```

### 在shell里打包。

```shell
mvn clena package -DskipTests # -DskipTests很重要，不然要花很多时间测试
```

### IDEA远程连接服的务器geomesa-sql项目：

1. 安装scala依赖，重启IDEA
2. 指定scala位置
3. 指定java位置
4. 设置里的编译器设置里的scala编译器里的Incrementality type指定为IDEA（这点也很重要）
5. 在geomesa-sql-core里找个测试，执行一下看看效果