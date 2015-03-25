SET mypath=%~dp0


cd %mypath%


../Flexflux.bat CompFVA -s compFVA/coli_core.xml -cons compFVA/constraintsFBA.txt -cons2 compFVA/constraintsFBA2.txt -out compFVA/comp_FVA_result.txt -lib 5 -plot

