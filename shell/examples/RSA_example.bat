SET mypath=%~dp0


cd %mypath%


../Flexflux.bat RSA -reg RSA/lacOperon.sbml -cons RSA/ConstraintsRSA.txt -out RSA/steady_states_result.txt -plot
