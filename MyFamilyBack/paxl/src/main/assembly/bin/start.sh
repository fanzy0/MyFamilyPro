#!/bin/bash

# 获取脚本所在目录的父目录（即解压后的根目录）
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
JAR_FILE=$(ls "$APP_HOME"/*.jar 2>/dev/null | head -1)
CONFIG_FILE="$APP_HOME/resources/application.properties"
LIB_DIR="$APP_HOME/lib"
LOG_DIR="$APP_HOME/logs"
LOG_FILE="$LOG_DIR/app.log"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到JAR文件，请检查 $APP_HOME 目录"
    exit 1
fi

# 检查lib目录是否存在
if [ ! -d "$LIB_DIR" ]; then
    echo "错误: 找不到lib目录，请检查 $APP_HOME 目录"
    exit 1
fi

# 创建日志目录
mkdir -p "$LOG_DIR"

# 构建classpath：包含lib目录下的所有jar包
CLASSPATH="$JAR_FILE"
for jar in "$LIB_DIR"/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# 启动应用
echo "正在启动应用..."
echo "应用目录: $APP_HOME"
echo "JAR文件: $JAR_FILE"
echo "配置文件: $CONFIG_FILE"
echo "日志文件: $LOG_FILE"

java -cp "$CLASSPATH" \
    -Dspring.config.location="$CONFIG_FILE" \
    -Dlogging.file.path="$LOG_DIR" \
    -Dlogging.file.name="app.log" \
    com.my.family.paxl.PaxlApplication \
    > "$LOG_FILE" 2>&1 &

# 获取进程ID
PID=$!
echo "应用已启动，进程ID: $PID"
echo "日志输出到: $LOG_FILE"
echo "查看日志: tail -f $LOG_FILE"