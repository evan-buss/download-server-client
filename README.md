# Download Server / Client

The project is a download server written in Java. The client will connect to
the server and be able to traverse directories and download any files found.

## Compilation

The program can be compiled into jar files or .class files.

### Client

#### Jar

Add proper commands here

#### Class File

`javac Client.java`

### Server

#### Jar

Add proper commands here

#### Class File

`javac Server.java`

## Execution

The program can be run via compile jar files or via compiled .class files.

### Client

The client receives the host and optional port number as command line arguments.

By default, the client will attempt to connect to port 5001 if a specific port
number is not specified.

#### Jar

`java -jar Client host [port]`

#### Class File

`java Client host [port]`

### Server

The server receives an optional port number to bind to as a command line
argument.

By default, the server will attempt to connect to the host on port 5001 if a
specific port number is not specified.

#### Jar

`java -jar Server [port]`

#### Class File

`java Server [port]`

## Design Overview

The project consists of two parts. The download server and the client. The
client serves to test the download server. The server program allows a
connected client to navigate its directories and download files.

## Server Protocol

### Client Commands

#### HELP

- Lists the valid client commands to the user
- Does not send any data to the server

#### BYE

- Tells the server to close connection and exits the client program
- Response: Server Closes Connection

## Issues
