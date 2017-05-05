#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
HOME_DIR=`pwd`
LIB_DIR=$HOME_DIR/lib
CONF_DIR=$HOME_DIR/conf
LOG_DIR=$HOME_DIR/log
PID=`ps -f|grep java|grep $CONF_DIR|awk '{print $2}'`
if [ -n "$PID" ];
then
   echo '服务已经启动！'
   exit 1
fi
if [ ! -d $LOG_DIR ];
then
   mkdir $LOG_DIR
fi
STD_OUT_FILE=$LOG_DIR/stdout.log
JAVA_DATA=" -Dlog.dir=$LOG_DIR"
LIB_JARS=`ls $LIB_DIR| grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`
JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn256m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
    JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi
echo "starting server....."
exec $JAVA_MEM_OPTS $JAVA_MEM_OPTS -classpath $CONF_DIR:$LOG_DIR:$LIB_JARS com.kanven.conveyor.Bootstrap
if [ $?==0 ];then
   echo "服务正常退出"
else
   echo "服务异常退出"
fi
exit $?
