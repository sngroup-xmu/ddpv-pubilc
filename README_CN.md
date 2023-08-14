# 分布式数据平面验证系统


## 验证规划器

通过指定网络不变量以及网络拓扑，生成DPVNet。
DPVNet是一种可表示为有向无环图（DAG）的数据结构，Tulkun使用该数据结构来表示验证消息的转发方向。
DPVNet采用`.puml`格式的文件编写，可以通过[PlantUML](https://plantuml.com/)查看生成的有向无环图。

- 运行环境：Python 3.9+
- 依赖库： `pip install -r .\scripts\requirements.txt`

### 功能测试

该测试使用下图拓扑。

![demo-topology](./config/demo/demo-topology.png)

会一次性生成以下类型的DPVNet。

- **Reachability** (D, \[S], (exist >= 1, S.*D)))
- **Waypoint** (D, \[S], (exist >= 1, S.*W.*D))
- **Reachability with limited path length** (D, \[W], (exist >= 1,
  WD|W.D|W..D))
- **Different-ingress same reachability** (D, \[A, B], (exist >= 1,
  A.*D|B..D))
- **All-shortest-path reachability** (D, \[S], (equal, (S.*D,
  (==shortest))))
- **Non-redundant reachability** (D, \[S], (exist == 1, S.*D))

调用方法如下：

```shell
cd scripts
python planner_test.py
```

运行后可以会在`\config\demo\`下生成相应的puml文件，可使用相关工具查看生成的DPVNet。


### Evaluation数据集生成

在`.\scripts\`目录下运行`planner_test.py`文件，并附带网络名称的参数，可以自动生成Evaluation所需的所有点对可达性的DPVNet。

例如：

```shell
cd scripts
python planner_test.py i2
```

运行完成后会在`.\config\i2`下生成`DPVNet.puml`文件，无需重命名，验证器启动时会自动使用该名称的文件进行验证任务。

可使用的网络名称参数为目录`.\config\`下的任意目录名称，也可以自行根据任意目录下的topology文件格式自定义网络。

## 模拟分布式验证

Tulkun目标环境为分布式的网络环境，本项目提供一种模拟方法，实现在单机上运行分布式验证，并查看验证时间。

运行要求如下：

- 通过`pom.xml`文件配置必要环境。
- 运行主类，主类位置位于： `src/main/java/Main.java`
- 建议在命令行模式下运行程序，可以使用参数`-h`查看所有可用参数。
- 无论使用哪种方法运行，请保证运行时目录下拥有`config`目录，并且已生成通过
  *Evaluation数据集生成* 中提到的DPVNet。

### 突发更新 (Burst Update)

即一次性向相应交换机载入所有转发规则，并进行验证。

运行方法：`java -jar Tulkun.jar bs <network name> --show_result`，
在对应位置填入网络名字即可运行突发更新的模拟及评估程序并输出结果。

- 可运行的网络可以在config目录下查看，或者执行`java -jar Tunkun.jar
  list`查看所有可运行的网络。
- 可通过`-t`参数设置运行次数，以此获得稳定的评估结果。
- 可通过`-h`参数查看其他参数的用法。

### 增量更新 (Incremental Update)

在突发更新之后，产生规则更新，并逐一载入并验证它们。

运行方法：`java -jar Tulkun.jar is <network name> --show_result`，
在对应位置填入网络名字即可运行增量更新的模拟及评估程序并输出结果。

- 可运行的网络可以在config目录下查看，或者执行`java -jar Tunkun.jar
  list`查看所有可运行的网络。
- 可通过`-t`参数设置增量更新的最大次数。
- 可通过`-h`参数查看其他参数的用法。


