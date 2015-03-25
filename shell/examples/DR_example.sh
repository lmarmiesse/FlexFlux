#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh DR -s DR/coli_core.xml -out DR/DR_result.txt -plot

