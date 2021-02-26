#!/usr/bin/env bash
export LANG="en_US.UTF-8"

#----------------------------------------------------------------------
# 常量配置信息
#----------------------------------------------------------------------

# 远程服务器 ip
#remote_server_ip='47.101.42.169'
#remote_server_ip='10.50.5.28'
remote_server_ip='10.0.102.250'
username='centos'
# !确保远程文件夹存在
#jar_store_dir='/home/jar-task/backend-service'
#jar_store_dir='/home/sjtu-dev/pd-micro-service/oss-service'
jar_store_dir='/home/centos/jar-project/oss-backend'

#----------------------------------------------------------------------
# 脚本
#----------------------------------------------------------------------

# 构建项目
echo "正在构建..."
gradle build -x test

if [ $? != 0 ]; then
  echo "构建失败"
  exit 1
fi
echo "构建成功"

# 将jar 包 scp 值目标服务器指定目录下
# shellcheck disable=SC2144
if [ -f ./build/libs/*.jar ]; then
  echo "正在上传 jar 包..."
#  cp run.sh build/libs/
  scp ./build/libs/*.jar ${username}@${remote_server_ip}:${jar_store_dir}
  # shellcheck disable=SC2181
  if [[ $? != 0 ]]; then
    echo "上传失败"
    exit 1
  fi
  echo "上传成功"
fi
read