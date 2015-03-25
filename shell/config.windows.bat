@echo off
SET mypath=%~dp0


::IF YOU ARE USING CPLEX YOU MUST SET THE RIGHE VALUE FOR THOSE TWO VARIABLES
::link to the CPLEX shared library (directory containing libcplex124.so)
set CPLEX_shared_library=
::link to CPLEX.jar
set CPLEX_JAR=


::IF YOU ARE USING GLPK YOU MUST SET THE RIGHE VALUE FOR THOSE TWO VARIABLES
::link to the GLPK shared library (directory containing glpk_4_50_java.dll)

if %PROCESSOR_ARCHITECTURE%==x86 (
  set GLPK_shared_library=%mypath%lib\glpk-4.50\w32
) else (
  set GLPK_shared_library=%mypath%lib\glpk-4.50\w64
)

::link to glpk jar
set GLPK_JAR=%mypath%lib\glpk-java-win.jar