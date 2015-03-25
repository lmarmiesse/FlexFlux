#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh CompFVA -s compFVA/coli_core.xml -cons compFVA/constraintsFBA.txt -cons2 compFVA/constraintsFBA2.txt -out compFVA/comp_FVA_result.txt -lib 5 -plot

