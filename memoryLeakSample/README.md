# memoryLeakSample — Metaspace leak via HotSwap (with a Flight Recording)

A self-contained demo for a presentation: it **leaks JVM Metaspace** by repeatedly **hot-swapping**
(redefining) a running class, and produces a **Java Flight Recording** (`metaspace-leak.jfr`) of the
leak. Nothing outside this folder is required.

Observed on a Temurin 21 JDK: Metaspace grows from ~4 MB to **~2.0 GB in about a minute** (≈14 600
redefinitions), then stops at the configured target — a clear, steep curve — **without crashing**
(the compressed-class space stays ~1 MB, so the 1 GB class-space cap is never approached).

## How it works

`demo.Leaker` runs a loop in `main`. On **every iteration** it:

1. prints a line telling you the current swap index (the loop's print statement), and
2. uses **reflection** to call the Java agent, which calls `Instrumentation.redefineClasses(...)`
   to **hot-swap `Leaker` itself** (the caller class) to the next pre-compiled version.

The **only** difference between the pre-compiled versions (~40 000 by default) is that printed
string (`Metaspace leak - swap #NNNNN / 40000`) — a legal method-body-only redefinition.

Why it leaks: each redefinition merges the new version's constant pool into the live `Leaker` class,
and that merged metadata is retained in Metaspace. Because `main` stays on the stack while `Leaker`
is redefined and the loop allocates almost nothing on the heap, class-unloading GCs are rare, so the
leak accumulates far faster than it is reclaimed. The leak grows with the number of **distinct**
versions introduced (a few tens of KB per version early on, much more as the merged constant pool
gets large), so feeding it more versions leaks more. The loop stops automatically once the Metaspace
pool reaches the target size (default ~2 GB); the run duration is only a safety cap.

## Layout

```
memoryLeakSample/
├── run-leak.sh              # single entry point: build → generate → run with JFR → report
├── agent/
│   ├── MetaspaceLeakAgent.java   # premain stores Instrumentation; static redefine(Class, byte[])
│   └── manifest.mf               # Premain-Class + Can-Redefine-Classes: true
├── app/
│   ├── Leaker.java.template      # the sample class; __SWAP_LABEL__ is the only thing that changes
│   └── Generator.java            # pre-compiles the swap classes (~40000 by default)
├── build/                   # (generated) agent.jar + compiled Generator
├── swaps/                   # (generated) 00000/demo/Leaker.class ... 39999/demo/Leaker.class
└── metaspace-leak.jfr       # (generated) the flight recording
```

`build/`, `swaps/` and `*.jfr` are generated artifacts and are git-ignored.

## Requirements

- A **JDK 21** (Temurin/OpenJDK). Point `JAVA_HOME_21` at it, e.g.:
  ```bash
  export JAVA_HOME_21="/home/marco_sussitz/.jdkman/jdks/temurin/21/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10"
  ```
  (If unset, the script falls back to that same hard-coded path.)
- Only `javac`, `jar`, `java`, `jfr` from that JDK are used — no Gradle, no network.

## Run it

```bash
./run-leak.sh                          # default: leak to ~2 GB (target 2048 MB), print every iteration
./run-leak.sh 600000 40000 1 1024      # stop at ~1 GB instead
./run-leak.sh 600000 40000 200         # print every 200th iteration (less terminal spam)
./run-leak.sh --regen                  # force-rebuild the swap classes first
```

Arguments: `run-leak.sh [--regen] [DURATION_MS] [NUM_VERSIONS] [PRINT_EVERY] [TARGET_MB]`
(defaults: `600000` ms cap, `40000` versions, print every `1`, target `2048` MB). The loop stops as
soon as the Metaspace pool reaches `TARGET_MB`; set `TARGET_MB=0` to disable the target and simply
run for the full duration instead.

On the first run the script builds `build/agent.jar` and generates `swaps/`; later runs reuse them
(`--regen`, or a changed `NUM_VERSIONS`, rebuilds the swap classes).

The script prints a summary at the end (redefinition count, Metaspace committed over time, and the
leaking class loader's Metaspace over time) and writes `metaspace-leak.jfr`.

Tip: printing every iteration (`PRINT_EVERY=1`) is terminal-I/O bound; a larger `PRINT_EVERY`, or
redirecting stdout to a file, lets the leak run faster.

## The flight recording

The run enables the JVM's most comprehensive built-in configuration (`settings=profile`) plus
**Native Memory Tracking** (`-XX:NativeMemoryTracking=summary`) so the recording captures the
Metaspace *size*, not just the redefinition activity. (`-javaagent`, `-XX:StartFlightRecording` and
`-XX:NativeMemoryTracking` are the only JVM options used — all capture-related, none tune the leak.)

Inspect it:

```bash
# Metaspace size over time (the leak curve):
"$JAVA_HOME_21/bin/jfr" print --events jdk.NativeMemoryUsage metaspace-leak.jfr

# Per-class-loader Metaspace (AppClassLoader = the leaking one):
"$JAVA_HOME_21/bin/jfr" print --events jdk.ClassLoaderStatistics metaspace-leak.jfr

# Every redefinition operation:
"$JAVA_HOME_21/bin/jfr" print --events jdk.RedefineClasses metaspace-leak.jfr
```

Or open `metaspace-leak.jfr` in **JDK Mission Control** (Memory → Native Memory / Metaspace).
