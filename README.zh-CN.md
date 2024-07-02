# DCSCA

这是一个双云架构下的安全可控云相册应用。

该项目不仅支持云端图像的加密存储、加密共享和浏览，还提供了用户澄清、云端文件完整性校验以及数据恢复等功能。目前使用腾讯云+阿里云的双云对象存储服务，但它可拓展至任意的对象存储服务。该项目使用 Java 进行开发。

以下是本项目的系统框架示意图：

![System Architecture](system_architecture.PNG)

## 安装

在安装之前，首先要确保正确安装了 Java 和 Android Studio。具体配置如下：

- java -- 18.0.1.1
- Android Gradle Plugin Version -- 7.1.3
- Gradle Version -- 7.2

接着将项目下载到本地，下载方式为：

```
git clone https://github.com/szu-security-group/double-cloud-security-cloud-album.git
```

## 使用

用Android Studio打开此项目，点击 `Sync Project Gradle Files `按钮,自动下载依赖插件。

找到 `app/src/main/java/com/example/sca/Config.java` 路径下的`Config.java` 文件，修改其中的配置参数，其中参数的具体申请方法见[腾讯云对象存储 准备工作 ](https://cloud.tencent.com/document/product/436/56390)，[阿里云对象存储(OSS) 配置访问凭证](https://help.aliyun.com/zh/oss/developer-reference/oss-java-configure-access-credentials?spm=a2c4g.11186623.0.i6#ef3b9ec0ed3f0)

运行`MainActivity.java`即可使用本app



