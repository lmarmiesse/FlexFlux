SET mypath=%~dp0


cd %mypath%

../Flexflux.bat Pareto -s pareto/coli_core.xml -cons pareto/constraintsPareto.txt -exp pareto/expFile.txt -out pareto/pareto/ -plot

