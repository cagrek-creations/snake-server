
JAVA_COMPILER = mvn
JAVA_RUNTIME = java
MAIN_CLASS = snake_server.Server

all: compile run

compile:
	$(JAVA_COMPILER) compile

run:
	mvn exec:java -Dexec.mainClass="snake_server.Server"

clean:
	mvn clean
