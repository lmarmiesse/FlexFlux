SET mypath=%~dp0


cd %mypath%

../Flexflux.bat PP2D -s PP2D/coli_core.xml -cons PP2D/constraintsFBA.txt -out PP2D/Reac_result.txt -r R_EX_o2_e -init -20 -end 0 -f 0.05 -plot
