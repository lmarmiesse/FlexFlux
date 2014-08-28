#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


source $path/config

cmd="java -Djava.library.path=$CPLEX_shared_library:$GLPK_shared_library -cp $GLPK_JAR:$path/lib/flexflux.jar flexflux.applications.gui.GraphicalFlexflux"

echo $cmd

eval $cmd