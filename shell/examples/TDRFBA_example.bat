SET mypath=%~dp0


cd %mypath%

../Flexflux.bat TDRFBA -s TDRFBA/coli.xml -cons TDRFBA/constraintsFBA_coli.txt -reg TDRFBA/lacOperon.sbml -out TDRFBA/TDRFBA_result.txt -bio R_BIOMASS -e lacZ -x 0.011 -plot

