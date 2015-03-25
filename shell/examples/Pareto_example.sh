#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh Pareto -s pareto/coli_core.xml -cons pareto/constraintsPareto.txt -exp pareto/expFile.txt -out pareto/pareto/ -plot

