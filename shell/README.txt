FlexFlux installation


For FlexFlux to work, you must have already installed a solver.
At this point, FlexFlux supports CPLEX and GLPK.

To use FlexFlux you need Java to be installed.

LINUX


1) make Flexflux.sh and GraphicalFlexflux.sh executables by typing the commands :
chmod +x Flexflux.sh
chmod +x GraphicalFlexflux.sh

2) Test your solvers by typing :

./Flexflux.sh Test

If a solver is OK, you are all set ! Skip step 3), you can use FlexFlux.

If no solver is OK, go to step 3).


3) Open the configuration file : config

Set the right value(s) for your solver, ignore the others.

Try the ./Flexflux.sh Test again.


4) Use the graphical version of Flexflux by executing the file "GraphicalFlexflux.sh"

		OR

Launch the analysis you want by typing : 

./Flexflux.sh your_analysis your_parameters

Example : 

./Flexflux.sh FBA -s network.xml -cond conditions.txt -plot


WINDOWS


1) Test your solvers by running the windows command file "GraphicalFlexflux"

OR by typing

Flexflux.bat Test

If a solver is OK, you are all set ! Skip step 2), you can use FlexFlux.

If no solver is OK, go to step 2).


2) Open the configuration file : config.windows

Set the right value(s) for your solver, ignore the others.

Try the Flexflux.bat Test or the GraphicalFlexflux again.


3)  Use the graphical version of Flexflux by running the windows command file "GraphicalFlexflux"

		OR

Launch the analysis you want by typing : 

Flexflux.bat your_analysis your_parameters

Example : 

Flexflux.bat FBA -s network.xml -cond conditions.txt -plot
