JAVAC=javac
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: $(classes)

clean :
	rm -f *.class

run :
	java WebServer

%.class : %.java
	$(JAVAC) $<
