#include build/Makefile.separator
#UNITCLASSPATH = ".$(CPSEPARATOR)unit"
JARFILE = topnbigramcount.jar
JAVAFILES = *.java

.PHONY : jar
jar : all-classes
	jar cfm $(JARFILE) manifest.mf *.class

.PHONY : all-classes
all-classes :
	javac $(JAVAFILES)

clean :
	rm -rf $(JAVAFILES:.java=.class) $(JARFILE)
