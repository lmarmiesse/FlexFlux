#!/bin/bash

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $path

../Flexflux.sh RSA -reg RSA/lacOperon.sbml -cons RSA/ConstraintsRSA.txt -out RSA/steady_states_result.txt -plot

