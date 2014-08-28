#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


foo=""
for i in `seq 2 $#`
do
	if [[ "${!i}" == *\ * ]]
	then
	
	foo="$foo \"${!i}\"" 
	else
	foo="$foo ${!i}" 
	fi
    
done

source config

cmd="java -Djava.library.path=$CPLEX_shared_library:$GLPK_shared_library -cp $GLPK_JAR:$path/lib/flexflux.jar flexflux.applications.Flexflux$1 $foo"

echo $cmd

eval $cmd
