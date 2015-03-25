#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh KO -s KO/coli_core.xml -cons KO/constraintsFBA.txt -out KO/KO_result.txt -mode 1 -plot

