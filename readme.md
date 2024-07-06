# Simple Chat Server and Client

## Overview
This repository contains a simple chat server and client application implemented in Java. The server supports multiple clients connecting simultaneously and communicating with each other in chat rooms. This project was developed as part of an AI course at university.

## Features
- **Multi-Client Support**: Multiple clients can connect to the server and join chat rooms.
- **Chat Rooms**: Clients can join and leave chat rooms.
- **Private Messaging**: Clients can send private messages to each other.
- **Nicknames**: Clients can set and change their nicknames.

## Components
### Server
The server manages client connections and communication. It uses Java NIO for non-blocking I/O operations and supports the following commands:
- `/nick [nickname]`: Set or change the client's nickname.
- `/join [room]`: Join a chat room.
- `/leave`: Leave the current chat room.
- `/bye`: Disconnect from the server.
- `/priv [nickname] [message]`: Send a private message to another client.

### Client
The client provides a graphical user interface for users to connect to the server and communicate. It uses Java Swing for the GUI and supports sending messages and commands to the server.

## Project Structure
- `ChatServer.java`: Main server class that handles client connections and communication.
- `ChatClient.java`: Main client class that provides a GUI for users to interact with the server.
- `Client.java`: Represents a client connected to the server.
- `ChatRoom.java`: Represents a chat room on the server.
- `State.java`: Enum representing the different states of a client (INIT, OUTSIDE, INSIDE).

## How to Run

1. Compile the code:
   ```bash
   javac *.java

2. Open the server:
   ```bash
   java ChatServer <port>

3. Run the client with the server's IP address and port number:
   ```bash
   java ChatClient <server-ip> <port>
