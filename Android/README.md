# 拍乐云实时音视频 Demo 使用指导

## 1. 注册拍乐云账号
进入拍乐云 [控制台](https://console.pano.video/) 页面，根据指导创建拍乐云账号。

## 2. 创建应用
登录拍乐云 [控制台](https://console.pano.video/) ，进入应用管理页面创建一个新的应用，获得 APPID，APPID 用以区分不同的实时音视频应用。

## 3. 下载 SDK+Demo 源码
拍乐云Demo源代码托管在 [github](https://github.com/PanoVideo) 上，用户可从github上clone。

## 3. 生成临时token
用户在创建应用获取到 APPID 后，还需要 token 才可以使用拍乐云实时音视频服务。对于 Demo 用户可在拍乐云控制台为应用生成临时token。

## 4. 编译 Demo
使用 Android Studio（3.2 以上的版本）打开源码工程BasicAudioCall，然后将 APPID 和 token 拷贝到 Demo 中相应位置，即可开始编译运行。

## 5. 开发环境要求
- 最低兼容 Android 4.4（SDK API Level 19），建议使用 Android 5.0 （SDK API Level 21）及以上版本
- Android Studio 3.2 或以上版本
