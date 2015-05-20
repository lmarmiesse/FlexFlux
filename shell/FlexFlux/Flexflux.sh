#!/bin/bash


path="${0/Flexflux.sh/}" 

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

source $path/config

eval "java -Djava.library.path=$CPLEX_shared_library:$GLPK_shared_library -cp $path/bin/:$CPLEX_JAR:$GLPK_JAR:$path/lib/* flexflux.applications.Flexflux$1 $foo"
