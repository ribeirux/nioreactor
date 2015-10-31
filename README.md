# NIO Reactor

## About

The goal of this project is to explore java non-blocking I/O capabilities without using external dependencies and 
deliver a tinny library that can be used to build scalable network services.

### Multithreaded Design

Since the reactor thread can saturate doing IO, nioreactor uses an acceptor thread that forwards new connections 
to a pool of reactors that can handle reads and writes in non-blocking mode. 

## Building distribution

### Requirements

* [Maven](http://maven.apache.org/) 2.2.0 or above
* Java 8 or above

To build:

1. git clone https://github.com/ribeirux/nioreactor.git
2. `mvn clean install`

### Demo

This projects comes with 2 examples:
  - EchoServer which sends back any received data
  - PongServer which send a "Pong" message back whenever a client connects to the server