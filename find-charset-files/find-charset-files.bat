@echo off
if defined JAVA_HOME (
	set JAVA=%JAVA_HOME%\bin\java
) else (
	set JAVA=java
)
%JAVA% -cp lib\commons-io-2.6.jar FindCharsetFiles.java
