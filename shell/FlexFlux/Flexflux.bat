@echo OFF

SET mypath=%~dp0




call %mypath%config.windows.bat



set ana=%1
shift

set args=

:continue
if "%~1"=="" goto fin
set args=%args% %1
shift
goto continue

:fin


java -Djava.library.path=%GLPK_shared_library%;%CPLEX_shared_library%   -cp %mypath%bin/;%CPLEX_JAR%;%GLPK_JAR%;%mypath%lib/* flexflux.applications.Flexflux%ana% %args%
