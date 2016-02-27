#!/bin/sh

astyle --options=./astylerc --exclude=app/src/main/java/org/xvdr/audio  --exclude=app/src/main/java/org/xvdr/msgexchange -r "app/src/main/java/*"


