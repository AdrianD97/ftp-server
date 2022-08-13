#!/bin/bash

type="$1" # task type
base_path="$2" # path to the directory which contains input files and is intended for storing output files
in="$base_path"/"$type"/in/
out="$base_path"/"$type"/out/
file="$base_path"/"$type"/"$type".csv

rm -rf "$out"/*;
rm -rf "$file";

for file_in in "$in"/*
do
    nr=$(echo "$file_in" | tr -dc '0-9')
    file_out="$out"/"$type"_"$nr".csv;
    echo "$nr is starting"
    node ./index.js "$type" "$file_in" "$file_out" "$file"
    echo -e "$nr clients have finished\n\n"
done
