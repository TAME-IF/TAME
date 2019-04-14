@echo off
setlocal
set TAME_HOME=%~dp0\..
"%TAME_HOME%\jre\bin\java.exe" -cp "%TAME_HOME%\jar\{{JAR_ONE_FILENAME}}" com.tameif.tame.console.TAMEConsoleShellMain %*
