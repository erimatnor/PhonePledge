JARFILE = phonepledge.jar
MANIFEST = Manifest.txt
SOURCEFILES = $(shell cd src; find . -name *.java -print)
RESOURCES = $(shell find src -name *.png -print) $(shell find src -name *.jpg -print)
JAR_LIBS = libs/dom4j-1.6.1.jar libs/json.jar libs/jsoup-1.5.2.jar libs/jaxen-1.1.3.jar libs/jtidy.jar
colon:= :
empty:=
space:= $(empty) $(empty)
CLASSPATH = $(subst $(space),$(colon),$(JAR_LIBS)):src
CLASSES = $(addprefix bin/,$(SOURCEFILES:%.java=%.class))
SOURCES := $(addprefix src/,$(SOURCEFILES))

$(JARFILE): $(CLASSES) classes.list sources.list resources.list $(MANIFEST) Makefile
	@echo "Creating Jar file"
	@(cd bin; jar cfm ../$@ ../$(MANIFEST) @../classes.list)
	@(cd src; jar uf ../$@ @../resources.list)
	@mkdir -p temp
	@FS=':'
	@for jar in $(JAR_LIBS); do \
		cd temp; \
		jar xf ../$$jar; \
		rm -rf META-INF; \
		cd ..; \
	done
	@(cd temp; jar uf ../$@ *)
	@rm -rf temp

$(CLASSES): $(SOURCES)
	@mkdir -p bin
	javac -cp $(CLASSPATH) -d bin $(SOURCES)

classes.list: $(CLASSES)
	@(cd bin; find . -name "*.class" -print > ../$@)

sources.list: $(SOURCES)
	@(cd src; find . -name "*.java" -print > ../$@)

resources.list: $(RESOURCES)
	@(cd src; find . -name "*.png" -print > ../$@)
	@(cd src; find . -name "*.jpg" -print >> ../$@)

clean:
	rm -f *~
	rm -rf bin/*
	rm -f $(JARFILE)
	rm -f classes.list sources.list resources.list
