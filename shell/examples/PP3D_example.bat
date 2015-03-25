SET mypath=%~dp0


cd %mypath%

../Flexflux.bat PP3D -s PP3D/coli_core.xml -cons PP3D/constraintsFBA.txt -out PP3D/Two_Reacs_result.txt -r R_EX_glc_e -init -20 -end 0 -f 0.5 -r2 R_EX_o2_e -init2 -20 -end2 0 -f2 0.5 -plot

