# jpuppeteer-generator
基于chrome devtools protocol协议生成客户端的maven插件（https://chromedevtools.github.io/devtools-protocol/tot/）

# maven 配置范例
```xml
<plugin>
  <groupId>jpuppeteer</groupId>
  <artifactId>jpuppeteer-generator</artifactId>
  <version>0.1</version>
  <configuration>
    <baseDir>${project.basedir}/src/main/java/</baseDir>
    <pkg>jpuppeteer.cdp.client</pkg>
    <browserProtocol>${project.basedir}/src/main/resources/browser_protocol.json</browserProtocol>
    <jsProtocol>${project.basedir}/src/main/resources/js_protocol.json</jsProtocol>
    <connectionClassName>jpuppeteer.cdp.CDPConnection</connectionClassName>
  </configuration>
</plugin>
```
# 配置说明
| 参数名称 | 参数含义 | 一般值 |
| ------ | ------ | ------ |
| baseDir | 生成出来的java类保存的根目录 | ${project.basedir}/src/main/java/ |
| pkg | 生成出来的java类的包名，相对于根目录存储 | 无 |
| browserProtocol | browser protocol json文件存放地址 | 无 |
| jsProtocol | js protocol json文件存放地址 | 无 |
| connectionClassName | CDPConnection类名 | jpuppeteer.cdp.CDPConnection |