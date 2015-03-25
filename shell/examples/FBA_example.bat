SET mypath=%~dp0


cd %mypath%

../Flexflux.bat FBA -s FBA/coli.xml -cons FBA/constraintsFBA_coli.txt -out FBA/FBA_result.txt -reg FBA/lacOperon.sbml -plot