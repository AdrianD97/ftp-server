#!/bin/bash

cmds=$(echo -e "$1")

ftp -inv <<EOF
$cmds
bye
EOF
