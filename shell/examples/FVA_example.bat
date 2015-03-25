SET mypath=%~dp0


cd %mypath%


../Flexflux.bat FVA -s FVA/coli_core.xml -cons  FVA/constraintsFBA.txt -out  FVA/FVA_result.txt -lib 10 -plot
