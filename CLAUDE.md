# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```sh
# Run locally (compiles and executes)
./your_program.sh

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassNameTest

# Compile only
mvn -q -B package
```

**Note**: Requires Java 25+ with preview features enabled (`--enable-preview`).

## Architecture

This is a Java implementation of a POSIX-compliant shell for the Codecrafters "Build Your Own Shell" challenge.

### Core Flow (Main.java)
The shell operates as a REPL loop:
1. `read()` - reads user input from stdin
2. `parse()` - tokenizes input, handling single/double quotes
3. `eval()` - executes the command (builtin or external)

### Command Model
- `Command.java` - Data class representing parsed commands. Tracks whether a command is a builtin, executable path, arguments, and workspace directory. Uses `Command.findExecutable()` to locate external programs via `PATH` environment variable.

### Builtin Commands
- `CCRunnable` interface - Contract for builtin commands (`void run(Command cmd)`)
- Builtins are singletons registered in `Main.builtinMap`: `echo`, `exit`, `type`, `pwd`, `cd`
- Each builtin implements `CCRunnable` and uses singleton pattern (`getInstance()`)

### Key Builtin Behaviors
- `CdCommand` - Handles `~` expansion (uses `$HOME`), absolute/relative paths, updates `Command.currentWorkspace`
- `TypeCommand` - Reports whether a command is builtin or executable
- `PwdCommand` - Prints current working directory from `Command.getCurrentWorkspace()`
- `EchoCommand` - Joins args with spaces, prints newline
- `ExitCommand` - Terminates the shell

### External Commands
Executed via `ProcessBuilder` using the resolved executable path from `Command.findExecutable()`.

## Code Layout

```
src/main/java/
├── Main.java          # REPL loop, parse(), eval()
├── Command.java       # Command model, executable lookup
├── CCRunnable.java    # Builtin command interface
├── CdCommand.java     # Builtin: change directory
├── EchoCommand.java   # Builtin: echo arguments
├── ExitCommand.java   # Builtin: exit shell
├── PwdCommand.java    # Builtin: print working directory
└── TypeCommand.java   # Builtin: command type info

src/test/java/
├── TestUtils.java     # stdout capture, test helpers
└── *Test.java         # JUnit tests for each builtin
```
