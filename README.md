# ET Mtshiliba 
# ThobaPlug 💬

A real-time multi-user chat application built with Java, JavaFX, and SQL Server.
ThobaPlug demonstrates client-server architecture, multi-threading, secure authentication,
and persistent message storage — built as a portfolio project.

---

## Features

- Real-time messaging between multiple users simultaneously
- Secure registration and login with BCrypt password hashing
- Persistent chat history loaded from SQL Server on login
- Live online users list updated instantly on join/leave
- Typing indicators shown to other users in real time
- Private messaging between individual users
- Server-side logging to file with timestamps
- Graceful error handling and client disconnect recovery

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 25 + FXML + CSS |
| Networking | Java Sockets (TCP) |
| Concurrency | ExecutorService Thread Pool |
| Database | SQL Server (JDBC) |
| Security | BCrypt password hashing |
| Serialization | Gson (JSON protocol) |
| Version Control | Git + GitHub |

---

## Architecture
Client (JavaFX)          Server (Java Sockets)       Database (SQL Server)

─────────────────        ──────────────────────       ─────────────────────

LoginController    ──►   ClientHandler (Thread)  ──►  Userr table

RegisterController ──►   ClientHandler (Thread)  ──►  Messagee table

ChatController     ◄──   Server (ThreadPool)     ◄──  JDBC Connection

---

## Project Structure
src/

├── com.thobaplug.server/

│   ├── Server.java          # ServerSocket + thread pool

│   └── ClientHandler.java   # One thread per client

├── com.thobaplug.client/

│   ├── Main.java            # JavaFX entry point

│   ├── Client.java          # Socket connection + listener

│   ├── LoginController.java

│   ├── RegisterController.java

│   └── ChatController.java

├── com.thobaplug.model/

│   ├── User.java

│   └── Message.java

├── com.thobaplug.database/

│   ├── DatabaseManager.java # Singleton DB connection

│   ├── UserDAO.java         # User CRUD + BCrypt auth

│   └── MessageDAO.java      # Message CRUD + history

└── com.thobaplug.util/

└── Logger.java          # File + console logging

---

## Message Protocol

All client-server communication uses JSON over TCP sockets:

| Type | Direction | Purpose |
|---|---|---|
| REGISTER | Client → Server | Create new account |
| LOGIN | Client → Server | Authenticate user |
| AUTH_SUCCESS | Server → Client | Login confirmed |
| AUTH_FAIL | Server → Client | Login rejected |
| BROADCAST | Both | Global chat message |
| PRIVATE_MSG | Both | Direct message |
| USER_LIST | Server → Client | Online users update |
| HISTORY | Server → Client | Past messages on login |
| TYPING | Client → Server | Typing indicator event |
| DISCONNECT | Client → Server | Clean logout |

---

## Setup & Run

### Prerequisites
- JDK 21+
- JavaFX SDK 25
- SQL Server (SQLEXPRESS)
- Eclipse IDE

### Database Setup
Run this SQL in SSMS:
```sql
CREATE DATABASE dbThobaPlug;
USE dbThobaPlug;

CREATE TABLE Userr (
    userr_id      INT IDENTITY(1,1) PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME DEFAULT GETDATE()
);

CREATE TABLE Messagee (
    message_id   INT IDENTITY(1,1) PRIMARY KEY,
    sender_id    INT NOT NULL,
    recipient_id INT NULL,
    content      NVARCHAR(1000) NOT NULL,
    sent_at      DATETIME DEFAULT GETDATE(),
    is_private   BIT DEFAULT 0,
    FOREIGN KEY (sender_id) REFERENCES Userr(userr_id),
    FOREIGN KEY (recipient_id) REFERENCES Userr(userr_id)
);
```

### Run
1. Start SQL Server (SQLEXPRESS)
2. Run `Server.java` — server starts on port 5000
3. Run `Main.java` — client window opens
4. Register an account and start chatting
5. Run multiple `Main.java` instances to chat between users

---

## Key Design Decisions

**BCrypt over MD5/SHA** — BCrypt is adaptive and industry standard for password storage.
Recruiters check for this.

**ExecutorService over raw Thread** — Thread pool prevents unbounded thread creation
under heavy load.

**ConcurrentHashMap for client registry** — Thread-safe without locking the entire map
on every operation.

**PreparedStatement everywhere** — Prevents SQL injection on all database queries.

**JSON protocol** — Structured, human-readable, and easy to extend with new message types.

---

## Author

**Thobani** — [@edsonthobani-dev](https://github.com/edsonthobani-dev)

Second-year Computer Science & Informatics student 
