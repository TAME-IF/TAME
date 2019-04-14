@echo off
setlocal
set TAME_HOME=%~dp0\..
java -cp "%TAME_HOME%\jar\{{JAR_ONE_FILENAME}}" com.tameif.tame.compiler.TAMECompilerMain %*
