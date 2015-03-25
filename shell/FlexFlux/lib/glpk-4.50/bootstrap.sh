#!/bin/sh
aclocal
libtoolize --force --copy
autoconf
automake

