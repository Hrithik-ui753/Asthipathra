<<<<<<< HEAD
# Asthipathra
=======
# Asthipathra - Secure Digital Asset Management System

Plain Java desktop application using:
- Java Swing (UI)
- JDBC (backend)
- SQLite (database)
- BCrypt (password hashing)
- AES (sensitive asset data encryption)

## Project Structure

- `src/db` - DB connection and schema initialization
- `src/model` - POJOs
- `src/dao` - JDBC CRUD/data access
- `src/service` - business logic
- `src/util` - security and utility helpers
- `src/ui` - Swing frames
- `src/ui/pages` - dashboard modules
- `src/main/resources/schema.sql` - SQLite schema
- `ER_DIAGRAM.md` - ER diagram (Mermaid)

## Prerequisites

1. Java 17+ installed
2. Place jars in `lib/`:
   - `sqlite-jdbc.jar`
   - `jbcrypt.jar`
   - `pdfbox-app.jar`

## Run

From project root on Windows:

```bat
run.bat
```

## Backend health check

Run end-to-end backend validation (DB init, JDBC, DAO/service chain):

```bat
run.bat health
```

Database file `asthipathra.db` is auto-created at root.

## Reports and analytics

- Open `Reports & Analytics` from dashboard.
- View visual analytics chart for all user records.
- Export full user records to PDF (assets, nominees, sharing, releases, notifications, audit logs).
>>>>>>> b33c57f (Initial commit - Asthipathra project)
