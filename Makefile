
JAVA_COMPILER = javac
JAVA_RUNTIME = java
MAIN_CLASS = snake_server.Server

all: compile run

compile:
	$(JAVA_COMPILER) -d . *.java

install:
	cp exampleconfig.yml config.yml

run:
	$(JAVA_RUNTIME) $(MAIN_CLASS)

clean:
	rm -rf *.class
