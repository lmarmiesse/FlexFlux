#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh TDRFBA -s TDRFBA/coli.xml -cons TDRFBA/constraintsFBA_coli.txt -reg TDRFBA/lacOperon.sbml -out TDRFBA/TDRFBA_result.txt -bio R_BIOMASS -e lacZ -x 0.011 -plot

