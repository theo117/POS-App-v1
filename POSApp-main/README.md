# JavaPOS

JavaPOS is a desktop point-of-sale application built with Java Swing and SQLite.
It includes product management, cashier checkout, inventory tracking, refunds,
sales history, closeout summaries, backup/restore tools, and an optional browser UI.

## Requirements

- JDK 16 or newer
- Apache Ant, if you want to use the NetBeans build targets

The SQLite JDBC driver is already included in `lib/`.

## Build

From this directory:

```sh
ant clean jar
```

If Ant is not installed, you can still compile the source directly:

```sh
javac -cp lib/sqlite-jdbc-3.49.1.0.jar -d /tmp/javapos-classes src/*.java
```

## Run

After building with Ant:

```sh
java -cp "dist/JavaPOS.jar:lib/sqlite-jdbc-3.49.1.0.jar" JavaPOS
```

On Windows, use `;` instead of `:` in the classpath.

## First Login

Fresh databases are seeded with temporary accounts:

- `admin` / `admin123`
- `cashier` / `cashier123`

Both accounts are forced to change their password on first login.

## Local Data

The SQLite database is stored outside the project folder:

- Windows: `%LOCALAPPDATA%\JavaPOS\javapos.db`
- Other platforms: `~/.javapos/javapos.db`

This keeps sales and inventory data separate from source code and build output.
