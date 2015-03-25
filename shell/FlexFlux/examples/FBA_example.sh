#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh FBA -s FBA/coli.xml -cons FBA/constraintsFBA_coli.txt -out FBA/FBA_result.txt -reg FBA/lacOperon.sbml -plot

