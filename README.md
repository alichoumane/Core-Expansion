# Core-Expansion
This program implements the community detection algorithm called "Core Expansion".

Article: Choumane, A., Awada, A. & Harkous, A. Core expansion: a new community detection algorithm based on neighborhood overlap. Soc. Netw. Anal. Min. 10, 30 (2020). https://doi.org/10.1007/s13278-020-00647-6


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


Contact us for any question: ali.choumane@ul.edu.lb
