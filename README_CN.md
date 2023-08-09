# 分布式数据平面验证系统

## 运行方式

- 通过`pom.xml`文件配置必要环境。

- 运行主类，主类位置位于： `src/main/java/Main.java`

- 建议在命令行模式下运行程序，可以使用参数`-h`查看所有可用参数。

### 突发更新 (Burst Update)

即一次性向相应交换机载入所有转发规则，并进行验证。

运行方法：`java -jar Tulkun.jar bs <network name> --show_result`， 在对应位置填入网络名字即可运行突发更新的模拟及评估程序并输出结果。

- 可运行的网络可以在config目录下查看，或者执行`java -jar Tunkun.jar list`查看所有可运行的网络。
- 可通过`-t`参数设置运行次数，以此获得稳定的评估结果。
- 可通过`-h`参数查看其他参数的用法。

### 增量更新 (Incremental Update)

在突发更新之后，产生规则更新，并逐一载入并验证它们。

运行方法：`java -jar Tulkun.jar is <network name> --show_result`， 在对应位置填入网络名字即可运行增量更新的模拟及评估程序并输出结果。

- 可运行的网络可以在config目录下查看，或者执行`java -jar Tunkun.jar list`查看所有可运行的网络。
- 可通过`-t`参数设置增量更新的最大次数。
- 可通过`-h`参数查看其他参数的用法。


