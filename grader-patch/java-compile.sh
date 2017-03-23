#!/bin/sh
MAIN_CLASS=$(cat lang.txt | awk -F: '{print $2}')
if [ -z "$MAIN_CLASS" ]; then
    javac $@ *.java
else
    MAIN_CLASS_FILE=${MAIN_CLASS//.//}.java
    javac $@ -sourcepath . $MAIN_CLASS_FILE
fi
