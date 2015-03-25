@echo off

SET mypath=%~dp0

cd %mypath%

call config.windows.bat


java -Djava.library.path=%GLPK_shared_library%;%CPLEX_shared_library%  -cp bin/;%CPLEX_JAR%;%GLPK_JAR%;lib/* flexflux.applications.gui.GraphicalFlexflux
