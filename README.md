# android-jarjar-plugin

用于合并Gradle Android Library中的依赖至AAR文件的classes.jar.类似于Maven Shade Plugin. 

类似项目:https://github.com/johnrengelman/shadow (该插件暂时不支持Android,2016年1月13日)

## 说明

该插件使用了Transform API,参见:http://tools.android.com/tech-docs/new-build-system/transform-api ,要求Android Gradle Plugin插件版本不低于2.0.0-alpha3


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
   
