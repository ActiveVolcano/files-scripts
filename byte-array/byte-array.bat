@echo off
if defined JAVA_HOME (
	set JAVA=%JAVA_HOME%\bin\java
) else if defined JRE_HOME (
	set JAVA=%JRE_HOME%\bin\java
) else (
	set JAVA=java
)
:forever
%JAVA% -cp lib\commons-codec-1.15.jar ByteArray.java %*
echo;
goto forever
