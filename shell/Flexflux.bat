@echo off


call config.windows.bat

set ana=%1
shift

set args=

:continue
if "%~1"=="" goto fin
set args=%args% %1
shift
goto continue

:fin


java -Djava.library.path=%GLPK_shared_library%;%CPLEX_shared_library%   -cp bin/;%CPLEX_JAR%;%GLPK_JAR%;lib/* flexflux.applications.Flexflux%ana% %args%
