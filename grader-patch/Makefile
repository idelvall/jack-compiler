JAVACFLAGS=
EXEC=VMTranslator

all: compile

compile:
	./java-compile.sh $(JAVACFLAGS)
	chmod +x $(EXEC)


clean:
	find . -name "*.class" -delete
	find . -name "*~" -delete
