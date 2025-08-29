#!/bin/bash

# 获取提交信息
commitmessage="$1"

# 检查是否提供了提交信息
if [ -z "$commitmessage" ]; then
    echo "错误：请提供提交信息"
    echo "用法: ./git-commit-here.sh \"你的提交信息\""
    exit 1
fi

# 检查当前目录是否是 Git 仓库
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo "错误：当前目录不是 Git 仓库"
    exit 1
fi

# 获取当前分支
current_branch=$(git symbolic-ref --short HEAD 2>/dev/null)

# 检查是否在 master 分支上，如果不是则切换到 master 分支
if [ "$current_branch" != "main" ]; then
    echo "当前在 $current_branch 分支，正在切换到 master 分支..."
    #git checkout main
    #if [ $? -ne 0 ]; then
    #    echo "错误：无法切换到 master 分支"
    #    exit 1
    #fi
fi

# 添加所有修改和新增的文件
echo "正在添加所有修改的文件..."
git add .

# 执行提交
echo "正在提交更改..."
git commit -m "$commitmessage"

# 推送到远程仓库::::
echo "正在推送到远程 master 分支..."
git push origin main

if [ $? -eq 0 ]; then
    echo "✅ 提交成功！"
else
    echo "❌ 推送失败，请检查网络连接或权限设置"
fi
