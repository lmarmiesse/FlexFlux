#!/bin/bash

path="${0/GraphicalFlexflux.sh/}" 

cd $path

source config

eval "java -Djava.library.path=$CPLEX_shared_library:$GLPK_shared_library -cp $CPLEX_JAR:$GLPK_JAR:lib/* flexflux.applications.gui.GraphicalFlexflux"
