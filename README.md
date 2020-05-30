# Core-Expansion
This program implements the community detection algorithm called "Core Expansion".


#############
Compile
#############

To compile the source code again, execute ./compile.sh under Linux. Make sure you have JDK 8 installed.

#############
Run
#############

The command to run the algorithm is:
java -jar CoreExpansion.jar -f network.dat

where network.dat is an undirected, unweighted network (one edge per line in the form "Source	Target").
