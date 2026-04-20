# CS 351L Project 4: Assembly Silos

**Authors:** Kiana Tarter and Nevaeh Martinez

A grid of independent virtual machines that run concurrently, each executing a simple assembly language program. Silos communicate with their neighbors through directional ports and remain in lock-step synchronization with one another.

---

## Project Structure

```
project-4-assembly-silos/
├── src/
│   ├── Main.java                  # Entry point — launches the JavaFX GUI
│   ├── logic/
│   │   ├── Puzzle.java            # Grid, barrier sync, pause/step/stop controls, stream wiring
│   │   ├── Silo.java              # Individual silo thread, registers, port I/O
│   │   ├── Interpreter.java       # Executes one instruction at a time
│   │   ├── Instruction.java       # Record: type, arg1, arg2
│   │   └── Stream.java            # Record: name, row, col, values
│   ├── parser/
│   │   └── Parser.java            # Parses puzzle input files into a Puzzle object
│   └── gui/
│       ├── MainWindow.java        # JavaFX GUI — file chooser, silo grid, controls
│       └── styles.css             # Styling for all GUI components
├── test_input/
│   ├── simple_example1.txt
│   └── simple_example2.txt
└── TestRunner.java                # Runs puzzle N times and reports pass/fail
```

---

## How to Run

### Prerequisites

- JDK 25
- JavaFX SDK 25
- IntelliJ IDEA (recommended)

### IntelliJ Setup

1. Clone the repository and open the project in IntelliJ.
2. Go to **File > Project Structure > Libraries** and add your JavaFX SDK `lib` folder.
3. Go to **Run > Edit Configurations** and add the following VM options:
   ```
   --module-path "C:\Program Files\javafx-sdk-25.0.2\lib" --add-modules javafx.controls,javafx.fxml
   ```
4. Set the **Working Directory** to the project root (the folder containing `test_input/`).
5. Set the main class to `Main` — this is the only entry point.

### Selecting a Puzzle File

When the application launches, a **file chooser dialog** opens automatically. Select any `.txt` puzzle file. You can also load a different file at any time using the **LOAD FILE** button in the controls panel - the simulation will stop, the new puzzle will be parsed, and the grid will rebuild automatically.

---

## GUI Overview

The GUI displays the full simulation state in real time.

**Silo grid (center):** Each silo box shows its current ACC, BAK, and PC register values, its program instructions, and its current state. Arrows between silos light up when a port transfer is in progress, showing the value being passed.

**I/O Streams panel (left):** Lists all input and output streams with their values. The current (next-to-be-read) value is highlighted. Output mismatches are highlighted in red.

**Controls panel (left):**

| Button | Behavior |
|--------|----------|
| LOAD FILE | Opens a file chooser to load a new puzzle. Stops any running simulation first. |
| START ▶ | Begins or resumes execution |
| PAUSE ⏸ | Halts after the current instruction completes |
| STEP ≫ | Advances exactly one full barrier cycle (all silos execute one instruction), then pauses |
| STOP ⃠ | Permanently halts all silo threads. Use LOAD FILE to run again. |
| Delay field | Sets the artificial delay between steps (seconds) — not yet wired |

The simulation starts **paused** - press **START** to begin.

---

## Input File Format

Puzzle files use the following format:

```
[title]
[description lines...]
GRID
[numRows] [numCols]
[silo (0,0) instructions]
END
[silo (0,1) instructions]
END
...
END
INPUT.NAME
[row] [col]
[value]
[value]
END
OUTPUT.NAME
[row] [col]
[expected value]
[expected value]
END
END
```

Inline comments are supported using a semicolon — everything after `;` on a line is ignored by the parser.

Stream positions use out-of-bounds coordinates to indicate which edge they sit on:

| Coordinate | Meaning |
|------------|---------|
| row = -1 | Above the grid → connects to silo's UP port |
| row = grid height | Below the grid → connects to silo's DOWN port |
| col = -1 | Left of the grid → connects to silo's LEFT port |
| col = grid width | Right of the grid → connects to silo's RIGHT port |

A silo with no instructions is represented by an immediate `END`.

---

## Assembly Language

Each silo runs a simple assembly program. All keywords are case-sensitive.

### Registers

| Register | Read | Write | Description |
|----------|------|-------|-------------|
| ACC | yes | yes | Accumulator. Primary working register. Starts at 0. |
| BAK | yes | no (direct) | Backup storage. Use SAVE or SWAP to write. Starts at 0. |
| NIL | yes | yes (discarded) | Always reads 0. Writes are silently discarded. |

### Ports

Each silo has four ports: `UP`, `DOWN`, `LEFT`, `RIGHT`. Port communication is a **blocking rendezvous** — both the sender and receiver must arrive before either can proceed. A silo blocked on a port is considered idle and excluded from the synchronization barrier.

### Instruction Set

| Instruction | Description |
|-------------|-------------|
| `NOOP` | Does nothing. Consumes one step. |
| `MOVE [SRC] [DST]` | Reads from SRC and writes to DST. Blocks if either is a port. |
| `ADD [SRC]` | ACC = ACC + SRC |
| `SUB [SRC]` | ACC = ACC - SRC |
| `NEGATE` | ACC = -ACC |
| `SAVE` | BAK = ACC |
| `SWAP` | Exchange ACC and BAK |
| `JUMP [LABEL]` | Unconditional jump to label |
| `JEZ [LABEL]` | Jump if ACC == 0 |
| `JNZ [LABEL]` | Jump if ACC != 0 |
| `JGZ [LABEL]` | Jump if ACC > 0 |
| `JLZ [LABEL]` | Jump if ACC < 0 |
| `JRO [SRC]` | Relative jump by offset. Labels are not counted. Wraps around. |
| `SLEEP [INT]` | Pauses all silos for the given number of seconds. |
| `:[LABEL]:` | Label declaration. Not an executable instruction. |

---

## Architecture & Design Decisions

### Entry Point

`Main.java` is the single entry point. It calls `Application.launch(MainWindow.class, args)` to hand off to JavaFX immediately. There is no headless mode — all interaction goes through the GUI.

### File Loading

`MainWindow` opens a `FileChooser` dialog on startup and again whenever LOAD FILE is pressed. `loadPuzzle(file)` stops any existing puzzle, parses the new file, rebuilds the silo grid and streams panel, and starts the silo threads in a paused state. The window title updates to show the loaded filename.

### Threading

Each silo runs on its own daemon thread. Silo threads are started by `Puzzle.start()` and run in an infinite loop, executing one instruction per iteration. Empty silos (no instructions) exit immediately without touching the barrier.

### Synchronization Barrier

`Puzzle` implements a custom hand-rolled barrier in `awaitBarrier()`. Every non-idle silo calls this after each instruction. When all active silos have arrived, the barrier trips and all silos are released for the next step.

Key design choices:
- `activeSilos` is an `AtomicInteger` tracking how many silos are currently active (not blocked on a port). The barrier trips when `silosWaiting >= activeSilos`.
- The `activeSilos` count is snapshotted **before** incrementing `silosWaiting` to close a race window where a port-blocking silo could decrement the count between the two lines.
- When a silo blocks on a port, it decrements `activeSilos` before blocking. The run loop re-increments it **after** the next barrier trip.
- Empty silos exit `run()` immediately without touching `activeSilos`.

### Pause / Step / Stop

`Puzzle` exposes four methods that the GUI buttons call directly:

| Method | Effect |
|--------|--------|
| `pause()` | Sets `paused = true`. Silos block inside `awaitBarrier()` after the next barrier trip. |
| `resume()` | Clears `paused` and `stopped`, wakes all waiting silos. |
| `step()` | Sets `stepOnce = true`. The barrier allows exactly one generation through, then re-pauses automatically. |
| `stop()` | Sets `stopped = true`. Every silo's `awaitBarrier()` throws `InterruptedException`, exiting the run loop cleanly. |

The simulation always starts paused — `loadPuzzle()` calls `puzzle.pause()` before `puzzle.start()` so silos block at the first barrier until the user presses START.

### Port Communication

Neighboring silos share a single `SynchronousQueue<Integer>` per edge. `wireNeighbors()` creates these after parsing. Input and output streams are wired the same way — each gets its own feeder or collector daemon thread that puts/takes values on the relevant queue. Each output stream has its own independent collector thread so fast streams don't block slow ones.

### Parser

`Parser.parse()` is split into three methods: `parse()` for the top-level structure, `parseSilo()` for one silo's instruction list, and `parseStream()` for one input/output stream. Inline comments (`;` to end of line) are stripped before tokenizing.

---

## Known Limitations / TODO

- **Delay field** — the text field exists in the UI but is not yet wired into the barrier. Steps execute as fast as the JVM allows.
- **Live register display** — ACC, BAK, PC, and STATE labels are set once when the grid is built and do not update as the simulation runs.
- **Live stream highlighting** — the current-value highlight in the streams panel is static.
- **Arrow transfer animation** — `showTransfer()` is implemented in `MainWindow` but not yet called from `Silo` during port transfers.

---

## Testing

`TestRunner.java` runs the puzzle N times back to back and reports pass/fail for each run. Uses a `CountDownLatch` to detect completion and a configurable timeout to detect deadlocks.

Set `TestRunner` as the main class in IntelliJ and configure at the top of the file:

```java
static final String TEST_FILE = "test_input/simple_example1.txt";
static final int NUM_RUNS = 20;
static final int TIMEOUT_MS = 8000;
```

Sample output:

```
============================================================
  TestRunner — 20 runs of test_input/simple_example1.txt
============================================================
Run  1: PASS
Run  2: PASS
...
============================================================
  SUMMARY
============================================================
  PASS:     20 / 20
  FAIL:      0 / 20
  DEADLOCK:  0 / 20
------------------------------------------------------------
  All runs passed!
============================================================
```

---
## Known Bugs
1. `Stop` button does not work correctly. 
2. Highlighting of the output stream is highlighted purple when it shouldn't be highlighted.