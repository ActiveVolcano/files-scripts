@echo off
if defined JAVA_HOME (
	set JAVA=%JAVA_HOME%\bin\java
) else if defined JRE_HOME (
	set JAVA=%JRE_HOME%\bin\java
) else (
	set JAVA=java
)
:forever
%JAVA% -cp lib\commons-io-2.6.jar Compare2Folders.java %*
echo;
goto forever
