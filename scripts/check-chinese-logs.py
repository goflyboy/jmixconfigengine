#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Chinese Log Checker for JMix Config Engine
检查Java代码中的中文日志内容
"""

import os
import re
import sys
from pathlib import Path

def find_java_files(root_dir):
    """查找所有Java文件"""
    java_files = []
    for root, dirs, files in os.walk(root_dir):
        # 跳过target、.git等目录
        dirs[:] = [d for d in dirs if d not in ['target', '.git', '.idea', 'node_modules']]
        
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files

def check_chinese_logs(file_path):
    """检查单个Java文件中的中文日志"""
    issues = []
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # 查找所有日志语句
        log_patterns = [
            r'log\.info\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
            r'log\.warn\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
            r'log\.error\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
            r'log\.debug\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
            r'log\.trace\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']'
        ]
        
        for pattern in log_patterns:
            matches = re.finditer(pattern, content)
            for match in matches:
                line_num = content[:match.start()].count('\n') + 1
                issues.append({
                    'line': line_num,
                    'type': 'Chinese Log',
                    'message': match.group(1),
                    'pattern': pattern
                })
                
    except Exception as e:
        issues.append({
            'line': 0,
            'type': 'File Error',
            'message': f'Error reading file: {str(e)}',
            'pattern': 'N/A'
        })
    
    return issues

def main():
    """主函数"""
    print("🔍 JMix Config Engine - Chinese Log Checker")
    print("=" * 50)
    
    # 获取项目根目录
    project_root = Path(__file__).parent.parent
    print(f"Project root: {project_root}")
    
    # 查找Java文件
    java_files = find_java_files(project_root)
    print(f"Found {len(java_files)} Java files")
    
    # 检查每个文件
    total_issues = 0
    files_with_issues = 0
    
    for java_file in java_files:
        issues = check_chinese_logs(java_file)
        if issues:
            files_with_issues += 1
            total_issues += len(issues)
            
            print(f"\n❌ {java_file}")
            for issue in issues:
                if issue['type'] == 'Chinese Log':
                    print(f"   Line {issue['line']}: {issue['message']}")
                else:
                    print(f"   {issue['message']}")
    
    # 输出总结
    print("\n" + "=" * 50)
    if total_issues == 0:
        print("✅ No Chinese log messages found! All logs are in English.")
    else:
        print(f"❌ Found {total_issues} issues in {files_with_issues} files")
        print("\n📋 Summary:")
        print("   - All log messages must be in English")
        print("   - Chinese characters are only allowed in comments")
        print("   - Please fix these issues before committing")
        
        # 返回非零退出码，便于CI/CD集成
        sys.exit(1)
    
    print("\n🎯 Remember: Logs are for operations and developers, use English;")
    print("   Comments are for developers, Chinese is allowed.")

if __name__ == "__main__":
    main() 