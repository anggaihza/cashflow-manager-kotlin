@rem Gradle startup script for Windows
@echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
)

"%JAVACMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
