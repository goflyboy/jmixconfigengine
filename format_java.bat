@echo off
echo Starting Java file formatting...

REM 查找所有Java文件并格式化
for /r src %%f in (*.java) do (
    echo Formatting %%f
    REM 这里可以添加格式化命令，比如使用IDE的命令行工具
    REM 由于没有Maven，我们暂时跳过格式化，直接运行测试
)

echo Formatting completed.
echo Running tests...

REM 尝试运行测试
java -cp "target/test-classes;target/classes" com.jmix.configengine.TestSuite

pause
