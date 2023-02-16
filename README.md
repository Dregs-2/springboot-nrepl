# Clojure nREPL for springboot

此项目的目的是为基于Spring的项目提供一种不一样的调试方式

## 如何工作
### 基本流程
1. 打包此项目将jar包和一些必须的依赖项添加到你的项目`mvn clean install`
2. 在启动参数中添加一些环境变量来配置
3. 修改启动类注解属性添加包扫描`@SpringBootApplication(scanBasePackages = "debug")`也可以修改该项目代码来定制
4. 启动你的项目
5. 通过支持nrepl的任何工具和你的项目建立起连接
6. 对的项目进行调用调试
### 需要的依赖项
- 实际路径根据自身情况觉得，这里只举出默认项
- 版本号可根据需要自行更换，
- 主库没有的依赖或需要添加repository `https://repo.clojars.org/` 来获取
<table>
<thead><tr>
<td>groupId</td>
<td>artifactId</td>
<td>version</td>
<td>localPath</td>
</tr></thead>
<tbody>
<tr>
<td>nrepl</td>
<td>nrepl</td>
<td>1.0.0</td>
<td>～/.m2/repository/nrepl/nrepl/1.0.0/nrepl-1.0.0.jar</td>
</tr>
<tr>
<td>org.clojure</td>
<td>clojure</td>
<td>1.11.1</td>
<td>～/.m2/repository/org/clojure/clojure/1.11.1/clojure-1.11.1.jar</td>
</tr>
<tr>
<td>org.clojure</td>
<td>core.specs.alpha</td>
<td>0.2.62</td>
<td>～/.m2/repository/org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.jar</td>
</tr>
<tr>
<td>org.clojure</td>
<td>spec.alpha</td>
<td>0.3.218</td>
<td>～/.m2/repository/org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.jar</td>
</tr>
</tbody>
</table>

### 环境变量配置项

<table>
<thead><tr>
<td>变量名</td>
<td>缺省</td>
<td>描述</td>
</tr></thead>
<tbody><tr>
<td>NREPL_ENABLE</td>
<td>false</td>
<td>是否开启nrepl, 为true时会被spring加载工作</td>
</tr>

<tr>
<td>NREPL_PORT</td>
<td>7888</td>
<td>nrepl暴露的端口</td>
</tr>
<tr>
<td>NREPL_NS</td>
<td>user</td>
<td>启动后默认namespace</td>
</tr>
<tr>
<td>PLUGIN_DIRECTORY</td>
<td>null</td>
<td>插件目录, 默认为空, 非空时根据目录地址去加载目录下后缀为*.clj的文件作为插件</td>
</tr></tbody>
</table>

### 在idea中添加支持
1. 下载插件 [cursive](https://plugins.jetbrains.com/plugin/8090-cursive)
2. 在Global Libraries中添加上述依赖项
3. 将Global Libraries依赖项添加到项目模块中（这样做的好处是只在本地生效）
4. Edit Configurations -> Clojure REPL -> Remote
5. 选择nrepl, post配置问本机ip, port默认配置为7888, module选择当前项目模块
6. 启动项目后，运行repl的连接

### 内置函数样例
- spring-bean>
```clojure
;spring-bean> 
(spring-bean> "testController");根据名称获取spring托管bean（不主动设置默认是类名小驼峰）
(spring-bean> org.example.controller.TestController);根据类获取spring托管bean
```
- invoke>
```clojure
;invoke>
(invoke> "testController" test "arg1" "arg2");根据名称获取bean并调用test方法
(invoke> org.example.controller.TestController test "arg1" "arg2");根据类获取bean并调用test方法
```

### 插件编写
```java
package org.example.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    
    private String id;
    
    private String username;
    
    @JsonProperty("alias_name")
    private String aliasName;
    
    public String getId(){ return this.id; }

    public String getUsername(){ return this.username; }

    public String getAliasName(){ return this.aliasName; }

    public String setId(String id){ return this.id = id; }

    public String setUsername(String username){ return this.username = username; }

    public String setAliasName(String aliasName){ return this.aliasName = aliasName; }
    
}

```
假设有上述java类，这里编写一个map转json再转为javabean的序列化函数（可以通过导入新库来实现，但这里使用spring内置托管的ObjectMapper来实现，减少依赖项且更贴合项目本身）

函数定义
```clojure
(defn map2bean [^Class c m]
  (let [json (invoke> com.fasterxml.jackson.databind.ObjectMapper writeValueAsString (update-keys m name))]
    (invoke> com.fasterxml.jackson.databind.ObjectMapper readValue json c)))
```
使用
```clojure
(map2bean org.example.pojo.User {:id "id1","username" "name1",:alias_name "aliasName1"})

;bean为clojure标准库函数 可以将java对象转换成一个map  可以很方便的看到这个对象的内容
(bean (map2bean org.example.pojo.User {:id "id1" "username" "name1" :alias_name "aliasName1"}))
```




