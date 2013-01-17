---
title: Java SDK | 七牛云存储
---

# Java SDK 使用指南

此SDK适用于Java 6及以上版本。基于 [七牛云存储官方API](/v3/api/) 构建。使用此 SDK 构建您的网络应用程序，能让您以非常便捷地方式将数据安全地存储到七牛云存储上。无论您的网络应用是一个网站程序，还是包括从云端（服务端程序）到终端（手持设备应用）的架构的服务或应用，通过七牛云存储及其 SDK，都能让您应用程序的终端用户高速上传和下载，同时也让您的服务端更加轻盈。

SDK下载地址：[https://github.com/qiniu/java-sdk/tags](https://github.com/qiniu/java-sdk/tags)

**目录**

- [环境准备](#env_preparation)
- [接入](#turn-on)
    - [配置密钥（AccessKey / SecretKey）](#establish_connection!)
- [使用](#Usage)
    - [文件上传](#upload)
        - [生成上传授权凭证（uploadToken）](#generate-upload-token)
        - [上传文件](#upload-server-side)
        - [断点续上传](#resumable-upload)
    - [文件下载](#download)
        - [公有资源下载](#download-public-files)
        - [私有资源下载](#download-private-files)
            - [生成下载授权凭证（downloadToken）](#download-token)
        - [高级特性](#other-download-features)
            - [断点续下载](#resumable-download)
            - [自定义 404 NotFound](#upload-file-for-not-found)
    - [文件管理](#file-management)
        - [查看单个文件属性信息](#stat)
        - [复制单个文件](#copy)
        - [移动单个文件](#move)
        - [删除单个文件](#delete)
        - [批量操作](#batch)
            - [批量获取文件属性信息](#batch-get)
            - [批量复制文件](#batch-copy)
            - [批量移动文件](#batch-move)
            - [批量删除文件](#batch-delete)
    - [云处理](#cloud-processing)
        - [图像](#image-processing)
            - [查看图片属性信息](#image-info)
            - [查看图片EXIF信息](#image-exif)
            - [图像在线处理（缩略、裁剪、旋转、转化）](#image-mogrify-for-preview)
            - [图像在线处理（缩略、裁剪、旋转、转化）后并持久化存储](#image-mogrify-for-save-as)
        - 音频(TODO)
        - 视频(TODO)
- [贡献代码](#Contributing)
- [许可证](#License)


<a name="env_preparation"></a>

## 环境准备

需要在Eclipse工程中，导入七牛云存储的 SDK。目前，七牛云存储的 SDK 依赖于一下第三方包：

- commons-codec-1.6.jar
- commons-logging-1.1.1.jar
- fluent-hc-4.2.jar
- httpclient-4.2.jar
- httpclient-cache-4.2.jar
- httpcore-4.2.1.jar
- httpcore-4.2.jar
- httpcore-ab-4.2.1.jar
- httpcore-nio-4.2.1.jar
- httpmime-4.2.jar
- json-lib-2.4-jdk15.jar
- org.json-chargebee-1.0.jar

七牛云存储 SDK 中的 qbox/lib 目录默认已经包含这些第三方包，您直接使用就行。但是，也有可能因为你本地编译环境问题，需要重新载入这些包。

<a name="turn-on"></a>

## 接入

<a name="establish_connection!"></a>

### 配置密钥（AccessKey / SecretKey）

要接入七牛云存储，您需要拥有一对有效的 Access Key 和 Secret Key 用来进行签名认证。可以通过如下步骤获得：

1. [开通七牛开发者帐号](https://dev.qiniutek.com/signup)
2. [登录七牛开发者自助平台，查看 Access Key 和 Secret Key](https://dev.qiniutek.com/account/keys) 。

在获取到 Access Key 和 Secret Key 之后，编辑com.qiniu.qbox包下的Config.java文件，确保其包含您从七牛开发者平台所获取的 Access Key 和 Secret Key：

    public static String ACCESS_KEY = "<Please apply your access key>";
    public static String SECRET_KEY = "<Please apply your secret key>";

<a name="Usage"></a>

## 使用

<a name="upload"></a>

### 文件上传

**注意**：如果您只是想要上传已存在您电脑本地或者是服务器上的文件到七牛云存储，可以直接使用七牛提供的 [qrsync](/v3/tools/qrsync/) 上传工具。如果是需要通过您的网站或是移动应用(App)上传文件，则可以接入使用此 SDK，详情参考如下文档说明。

<a name="generate-upload-token"></a>

#### 生成上传授权凭证（uploadToken）

要上传一个文件，首先需要调用 SDK 提供的`com.qiniu.qbox.auth`包下的`AuthPolicy`这个类来获取一个经过授权用于临时匿名上传的 `upload_token`——经过数字签名的一组数据信息，该 `upload_token` 作为文件上传流中 `multipart/form-data` 的一部分进行传输。 

 示例代码如下： 

	String bucketName = "bucket" ; 
	AuthPolicy policy = new AuthPolicy(bucketName, 3600); 
	String upToken = policy.makeAuthTokenString(); 

#### 服务端上传文件

通过 `com.qiniu.qbox.rs` 包下的 `putFileWithToken` 方法可在客户方的业务服务器上直接往七牛云存储上传文件。

 示例代码如下：

	String key = "upload.jpg" ;
	String dir = System.getProperty("user.dir") ;
	String absFilePath = dir + "/" + key ;

	String bucketName = "bucket" ;
	AuthPolicy policy = new AuthPolicy(bucketName, 3600);
	String uptoken = policy.makeAuthTokenString();
	
	PutFileRet putRet = null ;
	putRet = RSClient.putFileWithToken(uptoken, bucketName, key, absFilePath, "", "", "", "2") ;

**返回值**

上传成功，返回如下一个 Hash：

    {"hash"=>"------ to fill -------- "}

上传失败，会返回相应的错误。


<a name="resumable-upload"></a>

##### 断点续上传

用户在上传文件的时候也可以根据需求选择断点续上传的方式，此处所说的断点上传是指用户在某次上传过程中出现故障（比如断网，断点等异常情况）导致上传失败，再重新上传的时候只需要从上次上传失败处上传即可。用户可以根据具体应用的需求通过修改配置文件改变上传块（`com.qiniu.qbox``包下的 `Config` 文件中的 `PUT_CHUNK_SIZE` 对应的值）的大小来适应用户所处的网络环境。具体的示例代码可以参见我们在SDK中提供的 `ResumableGUIPutDemo` 以及 `ResumablePutDemo` 两个例子。

<a name="download"></a>

### 文件下载



#### 公有资源下载

    [GET] http://<bucket>.qiniudn.com/<key>

或者，

    [GET] http://<绑定域名>/<key>

绑定域名可以是自定义域名，可以在 [七牛云存储开发者自助网站](https://dev.qiniutek.com/buckets) 进行域名绑定操作。

注意，尖括号不是必需，代表替换项。

<a name="download-private-files"></a>

#### 私有资源下载

私有资源只能通过临时下载授权凭证(downloadToken)下载，下载链接格式如下：

    [GET] http://<bucket>.qiniudn.com/<key>?token=<downloadToken>

或者，

    [GET] http://<绑定域名>/<key>?token=<downloadToken>

<a name="download-token"></a>

##### 生成下载授权凭证（downloadToken）

`<downloadToken>` 可以使用 SDK 提供的如下方法生成：

    

**参数**



#### 高级特性

<a name="resumable-download"></a>

##### 断点续下载

七牛云存储支持标准的断点续下载，参考：[云存储API之断点续下载](/v3/api/io/#download-by-range-bytes)

<a name="upload-file-for-not-found"></a>

##### 自定义 404 NotFound

您可以上传一个应对 HTTP 404 出错处理的文件，当用户访问一个不存在的文件时，即可使用您上传的“自定义404文件”代替之。要这么做，您只须使用JAVA_SDK中的上传文件函数上传一个 `key` 为固定字符串类型的值 `errno-404` 即可。

除了使用 SDK 提供的方法，同样也可以借助七牛云存储提供的命令行辅助工具 [qboxrsctl](/v3/tools/qboxrsctl/) 达到同样的目的：

    qboxrsctl put <Bucket> <Key> <LocalFile>

将其中的 `<Key>` 换作  `errno-404` 即可。

注意，每个 `<Bucket>` 里边有且只有一个 `errno-404` 文件，上传多个，最后的那一个会覆盖前面所有的。


<a name="file-management"></a>

### 文件管理

文件管理包括对存储在七牛云存储上的文件进行查看、复制、移动和删除处理。

<a name="stat"></a>

#### 查看单个文件属性信息

您可以调用资源表对象的 Stat() 方法并传入一个 Key（类似ID）来获取指定文件的相关信息。

示例代码如下 :

	// 实例化一个资源表对象，并获得一个相应的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
	RSService rs = new RSService(conn, bucketName);
	
	// 获取资源表中特定文件信息
	StatRet statRet = rs.stat(key);

如果请求成功，得到的 statRet 数组将会包含如下几个字段：

	hash: <FileETag>
	fsize: <FileSize>
	putTime: <PutTime>
	mimeType: <MimeType>


<a name="copy"></a>

#### 复制单个文件

   

**参数**



**返回值**




<a name="move"></a>

#### 移动单个文件

   

**参数**



**返回值**

如果请求失败，返回 `false`；否则返回 `true` 。


<a name="delete"></a>

### 删除单个文件

要删除指定的文件，只需调用资源表对象的 Delete() 方法并传入 文件ID（key）作为参数即可。

如下示例代码：

	// 实例化一个资源表对象，并获得一个删除资源表中特定文件的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
	RSService rs = new RSService(conn, bucketName);

	// 删除资源表中的某个文件
	DeleteRet deleteRet = rs.delete(key);


<a name="batch"></a>

### 批量操作

   

**参数**



**返回值**



<a name="batch-get"></a>

#### 批量获取文件属性信息

   

**返回值**


#### 批量复制文件

    

**返回值**




<a name="batch-move"></a>

#### 批量移动文件

   

**返回值**

如果批量删除成功，返回 `true` ，否则为 `false` 。


<a name="batch-delete"></a>

#### 批量删除文件

   

**返回值**

如果批量删除成功，返回 `true` ，否则为 `false` 。


<a name="cloud-processing"></a>

### 云处理

<a name="image-processing"></a>

#### 图像

<a name="image-info"></a>

##### 查看图片属性信息

    

**参数**



**返回值**



<a name="image-exif"></a>

##### 查看图片EXIF信息

    

**参数**



**返回值**

如果参数 `url` 所代表的图片没有 EXIF 信息，返回 `false`。否则，返回一个包含 EXIF 信息的 Hash 结构。

<a name="image-mogrify-for-preview"></a>

##### 图像在线处理（缩略、裁剪、旋转、转化）


**参数**



**返回值**



#### 图像在线处理（缩略、裁剪、旋转、转化）后并持久化存储



**参数**


**返回值**

示例代码：




<a name="Contributing"></a>

## 贡献代码



<a name="License"></a>

## 许可证

