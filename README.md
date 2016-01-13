# android-jarjar-plugin

用于Android Grade项目的JarJar插件.


## 使用

1. 引入依赖.

    在build.gradle中添加依赖:
    ``` groovy
    buildscript {
        repositories {
            maven {
                url "https://dl.bintray.com/zhangzhenli/maven/"
            }
            jcenter()
        }
        dependencies {
            classpath 'com.house365.build:android-jarjar-plugin:1.0.0'
        }
    }
    ```

2. 配置jarjar规则,示例如下.

    ``` groovy
   jarjar {
      // optional, the rules for the jarjar tool
      rules = [
              'rule com.squareup.okhttp.** repackaged.com.squareup.okhttp.@1',
              'rule okio.** com.house365.mylibrary.repackaged.okio.@1'
      ]
  
      skipManifest true
  
      // optional, exclude files from the dependency .jar files
      srcExcludes = ['META-INF/**']
    }
    ```

3. 其他配置请参考https://code.google.com/p/jarjar/wiki/GettingStarted
   
