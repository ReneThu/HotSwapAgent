# HotSwapAgent
This program serves as a showcase for code hot-swapping in Java.
## Requirements
- Java 21
- Gradle 8+
## Running this application
There is a runMain gradle task at the top level that should compile all of the needed subprojects and run a sample hello world program with the hot-swap agent attached to it.
## Project structure
### agent
This is the core of the hot swap agent, responsible for loading the micronaut application, capturing the loaded classes, the decompiler, the compiler, and the class reloading.
### micronaut
The webserver that is used by the agent to trigger hot swaps. This part of the code is loaded by a custom classloader in the agent.
### agentBridge
This contains the interfaces that are used by the agent and the micronaut server.
### codeRunner
This is only used by the presentation to run the sample code