# Download Server / Client

The project is a download server written in Java. The client will connect to
the server and be able to traverse directories and download any files found.

## Compilation

The program needs to be compile from .java files to .class files in order to
be run.

### Client

`javac Client.java`

### Server

`javac Server.java`

## Execution

The program files can then be run once compiled.

The server program should be executed before the client.

### Client

The client receives the host and optional port number as command line arguments.

By default, the client will attempt to connect to port 5001 if a specific port
number is not specified.

`java Client host [port]`

### Server

The server receives an optional port number that specifies which server port
to bind to
to as a command line
argument.

By default, the server will attempt to connect to the host on port 5001 if a
specific port number is not specified.

`java Server [port]`

## Design Overview

The project consists of two parts. The download server and the client. The
client serves to test the download server. The server program allows a
connected client to navigate its directories and download files.

The server is implemented with with a multi-threaded model. This enables multiple clients to be connected at the same time without without server blocking.

## Server Protocol

### Client Commands

#### HELP

- Lists the valid client commands to the user
- Does not send any data to the server

#### BYE

- Tells the server to close connection and exits the client program
- Response: Server Closes Connection

## Issues

No reported issues at the moment.
