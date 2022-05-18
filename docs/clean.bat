REM Mr. A. Maganlal
REM Computer Science 2A 2018-2022
REM This batch file cleans a project for submission

REM Turn echo off and clear the screen
@echo off
cls

REM Good batch file coding practice
setlocal enabledelayedexpansion

REM Move to correct folder
cd ..

REM Variables for batch
set ERRMSG=
set PRAC_BIN=.\bin
set PRAC_DOCS=.\docs
set PRAC_JDOC=JavaDoc
set PRAC_LIB=.\lib\*
set PRAC_SRC=.\src

REM Clean all class files from bin folder and remove JavaDoc
:CLEAN
echo ~~~ Cleaning project ~~~
DEL /S %PRAC_BIN%\*.class
@ECHO ON
RMDIR /Q /S %PRAC_DOCS%\%PRAC_JDOC%
@ECHO OFF
IF /I "%ERRORLEVEL%" NEQ "0" (
    echo !!! Error cleaning project !!!
)

REM Something went wrong, display error
:ERROR
echo !!! Fatal error with project !!!
echo %ERRMSG%

REM Move back to docs folder and wait
:END
echo ~~~ End ~~~
cd %PRAC_DOCS%
pause