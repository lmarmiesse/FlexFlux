SET mypath=%~dp0


cd %mypath%

../Flexflux.bat DR -s DR/coli_core.xml -out DR/DR_result.txt -plot

