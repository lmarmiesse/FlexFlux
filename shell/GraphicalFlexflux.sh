#!/bin/bash


source config

eval "java -Djava.library.path=$CPLEX_shared_library:$GLPK_shared_library -cp bin/:lib/*:$CPLEX_JAR:$GLPK_JAR flexflux.applications.gui.GraphicalFlexflux"
