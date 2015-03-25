#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh FVA -s FVA/coli_core.xml -cons  FVA/constraintsFBA.txt -out  FVA/FVA_result.txt -lib 10 -plot
