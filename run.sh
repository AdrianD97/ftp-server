#!/bin/bash

export CLASSPATH=$CLASSPATH:/usr/local/JSON/json-simple-1.1.jar

make clean && make && make run
