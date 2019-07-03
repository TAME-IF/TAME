@echo off
setlocal
set TAME_HOME=%~dp0\..
"%TAME_HOME%\jre\bin\java.exe" -cp "%TAME_HOME%\jar\{{JAR_ONE_FILENAME}}" "-Dtame.project.template.path=%TAME_HOME%\templates" com.tameif.tame.project.TAMEProjectMain %*
