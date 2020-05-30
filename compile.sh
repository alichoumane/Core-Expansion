#!/bin/sh

if [ -d "bin" ]
then
	rm -R bin
fi

mkdir bin

echo "Compiling to bin ..."

javaFiles=""
for file in $(find src -name "*.java" -type f)
do
	echo $file
	javaFiles=$javaFiles" "$file
done

javac -d bin $javaFiles

echo "Creating jar ..."

cd bin
jar cfe ../CoreExpansion.jar algorithms.CoreExpansionAlgorithm algorithms/*.class networkanalysis/*.class utils/*.class
