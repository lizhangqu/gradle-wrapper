gradle-wrapper
=======

支持传参指定gradle version

### 使用方法

 1. 拷贝此项目中的 [gradle-wrapper.jar](dist/gradle-wrapper.jar) 文件覆盖 gradle/wrapper/gradle-wrapper.jar
 2. 添加 gradle/wrapper/gradle-wrapper-local.properties ，在此文件覆写 gradle-wrapper.properties 中的属性值
 3. 将 gradle/wrapper/gradle-wrapper-local.properties 文件添加到 .gitignore 中不进行版本管理

或通过参数 --gradle-version 传递需要使用的gradle版本，如

```
./gradlew --gradle-version=5.3.1 clean
```