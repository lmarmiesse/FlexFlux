SET mypath=%~dp0


cd %mypath%

../Flexflux.bat KO -s KO/coli_core.xml -cons KO/constraintsFBA.txt -out KO/KO_result.txt -mode 1 -plot

