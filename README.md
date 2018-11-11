# Download Server / Client

The application is a download server and client written in Java. The client will
connect to the server and be able to traverse directories and download any files found.

## Compilation

The program needs to be compiled from .java files to .class files in order to
be run.

### Client

`javac Client.java`

### Server

`javac Server.java`

### Both

`javac Client.java Server.java`

## Execution

The program files can then be run once compiled.

The server program should be executed before the client.

### Client

The client receives the host and optional port number as command line arguments.

By default, the client will attempt to connect to port 5001 of the given host specific port number is not specified.

`java Client <host> [port]`

### Server

The server receives an optional port number that specifies which server port
to bind to as a command line
argument.

By default, the server will attempt to bind to port 5001 if a specific port
number is not specified.

`java Server [port]`

## Design Overview

The project consists of two parts. The download server and the client. The
client serves to test the download server. The server program allows a
connected client to navigate its directories and download files.

The server is implemented with with a multi-threaded model. This enables multiple
clients to be connected at the same time without without server blocking requests.

## Server Protocol

### Client Commands

#### HELP

- Lists the valid client commands to the user
- Does not send any data to the server

#### BYE

- Tells the server to close connection and exits the client program
- Response: Server Closes Connection

#### PWD
- Tells the server to send the client's working directory.
- The client then sets the directory indicator to response's value
- Response: Directory path string.

#### DIR
- Tells the server to send a list of all files and folders in the current 
directory. The client displays this list to the user.
- Response: '/' separated string containing lines of files and folder names

#### CD <absolute path/relative path/..>
- Tells the server to switch to a new directory.
- Only accepts a single argument at a time. If there are multiple words, they
 are considered part of the same directory name
    - Such as `cd Hello World` 
- The ".." command moves up to the parent directory.
- Response: The server sends back the updated working path directory on 
success. On failure, it sends back the appropriate error code:
    - "DDNE" - Directory does not exist
    - "PD"  - Permission Denied
- On success, the client updates the current directory indicator to match the
 new server directory.


## Issues

No issues reported.
