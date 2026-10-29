# 机器人开发学习路线：从可重复的小实验开始

**状态：原创草稿，待后台审核。**

## 1. 打好基础

学习 Python 或 C++、Linux 命令行、Git 和基本调试。数学方面先掌握向量、矩阵、坐标变换、微积分和概率的常用概念。目标是能解释数据如何从传感器到控制器，而不是只会复制命令。

## 2. 学习 ROS 2

从节点、Topic、Service 和 Action 开始，练习参数、launch、日志、TF 和 rosbag。使用仿真环境完成移动或机械臂任务，再学习 URDF 和 `ros2_control`。每个练习都应包含复现步骤和预期输出。

## 3. 逐步接触实体硬件

先读厂商手册，确认供电、电压等级、接口、急停和限位，再连接低风险传感器。控制真实执行器前，准备隔离区域、限速、急停、人工接管和断电流程。

## 4. 建议项目

1. 发布温度或 IMU 数据并用 RViz/PlotJuggler 查看。
2. 在仿真中完成移动底盘的目标点导航。
3. 描述一个简单机械臂并验证关节范围。
4. 用 rosbag 记录并复现实验数据。
5. 为一个真实任务定义成功率、延迟和安全边界。

## 参考

- [ROS 2 官方教程](https://docs.ros.org/en/jazzy/Tutorials.html)
- [ROS 2 官方概念文档](https://docs.ros.org/en/jazzy/Concepts.html)
- [Modern Robotics：课程与教材](https://modernrobotics.northwestern.edu/)
- [ros2_control 官方文档](https://control.ros.org/jazzy/doc/ros2_control/doc/index.html)
