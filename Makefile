all:
	javac Project.java
	java Project 1 19 0.01 4096 4 0.5 64 > output02-full.txt
	java Project 2 19 0.01 4096 4 0.5 64 > output03-full.txt
	java Project 8 19 0.01 4096 4 0.75 32 > output04-full.txt
	java Project 8 101 0.001 16384 4 0.5 128 > output05-full.txt