# GDB Cheat Sheet — Debugging the Metaspace Leak in the slowdebug JVM

A copy‑paste reference for the **live GDB demo**: driving an interactive `gdb` session against the
locally built **slowdebug** JVM while it runs the `memoryLeakSample` metaspace leak. Everything below
is typed into the terminal on the presentation slide (`http://localhost:8081/gdb`) — or into a plain
gdb session started from a shell.

Every command here has been run against the real slowdebug build; the numbers shown in the output
blocks are actual captured values (yours will differ slightly but follow the same pattern).

> **In a hurry / presenting live?** `GDB-COMMANDS-IN-ORDER.md` is the terse, top‑to‑bottom command
> runbook (just the commands, no prose) — this file is the "why" behind each one.

---

## 0. What we are debugging

- **JVM:** `.../work_repos/jdk/build/linux-x86_64-server-slowdebug/images/jdk/bin/java` (OpenJDK 25,
  **slowdebug** — full symbols, `-O0`, every inline accessor is a real callable function, and the C++
  source is on disk so gdb can show it).
- **App:** `demo.Leaker` from `memoryLeakSample`. A hot loop repeatedly uses reflection to call the
  Java agent `agent.MetaspaceLeakAgent`, which calls `Instrumentation.redefineClasses()` on **every**
  iteration, swapping in a pre‑compiled version of `demo.Leaker` whose only difference is the
  iteration string it prints.
- **Interpreter is enough.** The breakpoints below sit on native VM entry points that are hit on
  *every* redefinition regardless of JIT state — nothing needs to be compiled.

### Why it leaks (the two facets you will show)

1. **The live class's constant pool grows every redefine.** `merge_cp_and_rewrite` allocates a
   brand‑new merged constant pool (worst‑case size = old + new) and installs it on the live class.
   `the_class->constants()->length()` climbs monotonically — over thousands of distinct versions this
   one pool becomes huge. *This is the directly visible leak (BP②).*
2. **The fresh metaspace allocated each redefine is (almost) never reclaimed.** Every redefinition
   allocates new metadata — the merged `ConstantPool` (BP③), a second shrunken pool in
   `set_new_constant_pool`, a new `ConstantPoolCache` and `resolved_references`. The old version's
   metadata is *retired* onto the class‑loader's deallocate list (BP⑤), but it is only freed during a
   **class‑unloading GC**. Because `demo.Leaker.main` stays on the stack and the loop allocates almost
   nothing on the heap, class‑unloading GCs almost never run — so the retired metadata piles up.
   *This is the "other place that also leaks".*

---

## 1. Starting the session (press Enter)

When you enter the slide the terminal is already connected and shows the gdb command, but **does not
run it yet**:

```
# Press Enter to launch the debugger
presenter@jvm-debug:~/jdk$ gdb -x gdb-init.gdb --args java -javaagent:agent.jar -cp swaps/00000 demo.Leaker
```

Press **Enter** to launch gdb (with the real full‑path invocation). You then type the commands in the
sections below. gdb starts the debuggee JVM only when you type `run`.

**Session lifecycle (no on‑screen buttons — the terminal fills the whole slide):**

- **start** — entering the slide auto‑connects and shows the prompt above; press **Enter** to launch gdb.
- **clear the screen** — press **Ctrl‑L** (gdb/readline clears and redraws).
- **stop** — leave the slide: the backend kills gdb *and* the debuggee JVM (and every child). Or type
  `kill` then `quit` in gdb.
- **restart** — leave and re‑enter the slide for a fresh "press Enter" prompt.

---

## 2. Session setup (already applied — shown for manual use)

The init file (`gdb-init.gdb`) applies this for you. Run it by hand if you start gdb yourself:

```gdb
set pagination off
set confirm off
set breakpoint pending on
set overload-resolution off
```

### Ignore the JVM's signals ⚠️ important

The JVM uses `SIGSEGV` (null checks, safepoint polling), `SIGBUS` and `SIGFPE` as *normal control
flow*. Tell gdb to pass them straight through so the debugger never stops on them:

```gdb
handle SIGSEGV nostop noprint pass
handle SIGBUS  nostop noprint pass
handle SIGFPE  nostop noprint pass
```

---

## 3. Displaying the C++ source (see the variables in context)

To make sense of a variable you usually want to see the code around it. Because this is a slowdebug
build the JVM's own C++ source is on disk, so gdb can show it whenever you are stopped.

Print the source around wherever you are currently stopped:

```gdb
list
```

```
1763               TRAPS) {
1764    // worst case merged constant pool length is old and new combined
1765    int merge_cp_length = the_class->constants()->length()
1766          + scratch_class->constants()->length();
1767
1768    // Constant pools are not easily reused so we allocate a new one
1769    // each time.
```

Jump to any function, file:line, or explicit range — and show more lines at once:

```gdb
list VM_RedefineClasses::merge_cp_and_rewrite    # by function
list jvmtiRedefineClasses.cpp:1765               # by file:line
list 1760,1780                                    # an explicit range
set listsize 30                                   # then `list` shows 30 lines
```

### Live source window (TUI) — the code follows execution

For a persistent source view that **auto‑follows** as you `continue`, turn on gdb's TUI. The audience
then always sees the exact C++ line you are on, right above the variables you print:

```gdb
layout src        # split screen: source window on top, gdb command line below
```

Handy while TUI is on:

```gdb
focus cmd         # send arrow keys / PgUp / PgDn to the command window (source window is default)
refresh           # (or Ctrl‑L) redraw if the screen ever gets garbled
tui disable       # leave TUI, back to the plain terminal
```

Pair the source view with `info args`, `info locals` and `info line` to list every variable in the
current frame — then `p <name>` to read one. This is the workflow used at each breakpoint below.

### See every field of an object

`info args` / `info locals` only *name* the variables in scope. To look **inside** one of them and
list **all of its fields**, dump the whole object. Use the live class `the_class` (in scope at BP②)
as the example — this is how you discover it carries a `constants` field (the metaspace
`ConstantPool` that grows):

```gdb
ptype the_class      # the TYPE: every field name + type — spot `ConstantPool* _constants`
p *the_class         # the VALUES: every field at once, `_constants = 0x...` among them
```

`ptype` answers *what fields exist* (best for discovery); `p *ptr` shows their current *values*. The
accessor `the_class->constants()` simply returns the `_constants` field you see here — so if an
inline accessor ever refuses to run, read the field directly: `p the_class->_constants`.

To walk a large object interactively, drilling into one field at a time, use gdb's explorer; add
`/o` to `ptype` to also print each field's byte **offset** and the struct size:

```gdb
explore the_class    # step through pointer → struct → each field interactively
ptype /o the_class   # every field with its byte offset + total size
```

---

## 4. Breakpoint ① — the hot loop / agent redefine call

Native entry point behind `Instrumentation.redefineClasses()`. Hit on **every** loop iteration, on
the **main Java thread**, in the interpreter. This is "a breakpoint inside the hot loop".

```gdb
break JvmtiEnv::RedefineClasses
run
```

gdb stops at the first redefinition:

```
Thread 2 "java" hit Breakpoint 1, JvmtiEnv::RedefineClasses (...) at jvmtiEnv.cpp:491
```

### Show the stack with HotSpot's own helpers

`help()` lists every built‑in VM debug helper:

```gdb
call (void)help()
```

`ps()` prints the **current thread's Java stack** — you will see `demo.Leaker.main` calling through
reflection into `agent.MetaspaceLeakAgent.redefine`:

```gdb
call ps()
```

`pns(sp, fp, pc)` prints the **native + Java mixed stack** (needs `set overload-resolution off`,
already applied). On Linux/amd64 pass the registers `$sp, $rbp, $pc`:

```gdb
call pns($sp,$rbp,$pc)
```

You will see the full chain: `JvmtiEnv::RedefineClasses` → `libinstrument` →
`agent.MetaspaceLeakAgent.redefine` → `demo.Leaker.main` → `JavaMain` → `libc`.

Other useful helpers: `pss()` (all thread stacks), `pp(ptr)` (identify a pointer),
`findclass("name", 0)`, `events()`. And remember `list` / `layout src` (§3) to show the C++ you are in.

---

## 5. Breakpoint ② — the constant pool grows (the money shot)

Every redefinition merges the old and new constant pools into a brand‑new, larger one. Break on the
merge:

```gdb
break VM_RedefineClasses::merge_cp_and_rewrite
continue
```

gdb stops (the redefinition runs as a VM operation at a safepoint) at
`jvmtiRedefineClasses.cpp:1765`. Show the code you are in and the variables in scope:

```gdb
list
info args
```

In scope: `the_class` (the **live** class) and `scratch_class` (the **incoming** version). List
**every field** of the live class to see the metadata it carries — this is where you can see it has a
`constants` field (the pool that grows); see §3 "See every field of an object":

```gdb
ptype the_class      # every field & type — spot `ConstantPool* _constants`
p *the_class         # every field's value at once
```

`the_class->constants()` is just the accessor for that `_constants` field. Now read the live class's
constant‑pool length — **this is the number that grows**:

```gdb
p the_class->constants()->length()
```

```
$1 = 202
```

Compare the incoming version and the worst‑case merged size:

```gdb
p scratch_class->constants()->length()
p merge_cp_length
```

```
$2 = 202          # the incoming version
$3 = 404          # worst case = old (202) + new (202) before de‑duplication
```

> **Field fallback.** If an inline accessor ever refuses to run in gdb, read the raw fields instead —
> they always work:
>
> ```gdb
> p the_class->_constants->_length
> ```

### Watch it grow — the loop to run live

Repeat this a handful of times so the audience sees the live class's pool climb on **every**
hotswap:

```gdb
continue
p the_class->constants()->length()
```

Captured across successive `continue`s:

```
$1 = 202
$4 = 202
$5 = 204
$6 = 206
...
```

A brief plateau, then a steady **+2 per redefine** (each distinct version contributes a new `Utf8`
and `String` constant that is merged into — and kept on — the live class). Show the native backtrace
to prove where you are:

```gdb
bt 3
```

```
#0 VM_RedefineClasses::merge_cp_and_rewrite (...)  jvmtiRedefineClasses.cpp:1765
#1 VM_RedefineClasses::load_new_class_versions ()  jvmtiRedefineClasses.cpp:1476
#2 VM_RedefineClasses::doit_prologue ()            jvmtiRedefineClasses.cpp:216
```

The class also carries a `ConstantPoolCache` that is rebuilt each redefine (see §9):

```gdb
p the_class->constants()->cache()
```

---

## 6. Breakpoint ③ — the metaspace allocation itself

The merged pool is carved out of **metaspace** by `ConstantPool::allocate`. This is the actual
allocation that is never reclaimed. It is also called during ordinary class loading, so arm it only
**after** the redefine loop is steady (e.g. once BP② has hit a few times), ideally as a temporary
breakpoint:

```gdb
tbreak ConstantPool::allocate
continue
p length
bt 3
```

```
Thread 2 "java" hit Temporary breakpoint, ConstantPool::allocate (loader_data=..., length=404) at constantPool.cpp:76
$1 = 404
#0 ConstantPool::allocate (...)                    constantPool.cpp:76
#1 VM_RedefineClasses::merge_cp_and_rewrite (...)   jvmtiRedefineClasses.cpp:1776   <-- allocated here, every redefine
#2 VM_RedefineClasses::load_new_class_versions ()   jvmtiRedefineClasses.cpp:1476
```

The backtrace proves the allocation comes straight from the redefine path (`:1776`). A **second**,
smaller pool is allocated later in the same redefinition by `set_new_constant_pool`
(`jvmtiRedefineClasses.cpp:3485`) — arm that too if you want to show both allocations:

```gdb
break VM_RedefineClasses::set_new_constant_pool
```

---

## 7. Breakpoint ④ — the merged pool is installed on the live class

After merging, the new pool is attached to the live class here (the old pool is swapped onto
`scratch_class` to be retired):

```gdb
break jvmtiRedefineClasses.cpp:4278
continue
list
p the_class->constants()->length()
```

```
$1 = 208          # the just‑installed, grown pool — the live class now owns it
```

This is the line that makes the growth *stick*: `the_class->set_constants(scratch_class->constants())`.

---

## 8. Breakpoint ⑤ — the retirement path (why it accumulates)

When a version is replaced, its old metadata is retired here:

```gdb
break InstanceKlass::add_previous_version
continue
```

gdb stops at `instanceKlass.cpp:4433`. In scope: `this` (the live class), `scratch_class` (the old
version being retired) and `emcp_method_count`. Show the source and inspect them:

```gdb
list
p emcp_method_count
p scratch_class->constants()->on_stack()
p scratch_class->methods()->length()
```

```
$1 = 3            # methods still equivalent (EMCP) in the retired version
$2 = true         # the old pool is still referenced by a running frame...
$3 = 3
```

Walk the retained‑versions chain:

```gdb
p this->previous_versions()
p this->previous_versions()->previous_versions()
```

```
$4 = (InstanceKlass *) 0x0        # or one node, then 0x0
```

### Read this out loud — the honest mechanism

`add_previous_version` first calls `purge_previous_version_list()`, which **frees** any retained
version whose constant pool is no longer `on_stack()`. So the `previous_versions()` chain does **not**
grow without bound — it stays short (often a single node). The old version is only *kept* while its
pool is `on_stack()` (a running frame still points at it) or it has obsolete methods; otherwise its
metadata is queued on the class‑loader's **deallocate list**.

The leak is therefore **not** an ever‑growing previous‑versions chain. It is that everything on the
deallocate list — old constant pools, `ConstantPoolCache`s, `resolved_references`, obsolete
`Method*`/`ConstMethod*` — is only actually freed during a **class‑unloading GC**, and in this
workload (main pinned on the stack, almost no heap allocation) class‑unloading GCs practically never
run. Combined with the ever‑growing live pool from BP②/④, metaspace climbs without bound.

---

## 9. Other things that also leak in this scenario (ranked)

Each redefinition allocates *these* in metaspace too; all are retired to the deallocate list and share
the same "rarely reclaimed" fate. Break where noted and read the size.

1. **The retired old constant pools** — the biggest contributor after the live pool. Each is the
   already‑grown pool from the previous redefine.
   ```gdb
   p scratch_class->constants()->length()      # at BP⑤: the size being retired
   ```
2. **A second, shrunken constant pool** allocated by `set_new_constant_pool`
   (`jvmtiRedefineClasses.cpp:3485`) on every redefine.
   ```gdb
   break VM_RedefineClasses::set_new_constant_pool
   ```
3. **`ConstantPoolCache` + `resolved_references`** — rebuilt for the new pool each redefine.
   ```gdb
   p the_class->constants()->cache()                       # at BP②/④
   p the_class->constants()->resolved_references()
   ```
4. **Obsolete `Method*` / `ConstMethod*`** kept reachable via the retired version.
   ```gdb
   p scratch_class->methods()->length()        # at BP⑤
   ```
5. **`_cached_class_file`** — the redefinition bytes copied onto the live class.
   ```gdb
   p the_class->get_cached_class_file()         # at BP②/④
   ```

---

## 10. Driving the leak & inspecting

```gdb
continue            # advance to the next hotswap / breakpoint
c                   # short form
info breakpoints    # list breakpoints & hit counts
delete 2            # remove breakpoint #2 to let the leak run freely
disable 3           # keep a breakpoint but stop stopping on it
info threads        # list JVM threads
thread 3            # switch to another thread
frame 1             # select a stack frame
info args           # arguments of the selected frame
info locals         # locals of the selected frame
finish              # run until the current function returns
p merge_cp_length   # any in‑scope variable
```

Tip: once the breakpoints have made their point, `delete` them and `continue` so the audience can
watch metaspace balloon in your monitoring view (e.g. a JFR/`jcmd` panel).

---

## 11. Teardown

- **From the slide:** just leave the slide — the backend kills gdb and the slowdebug JVM (and every
  child).
- **From gdb:**

```gdb
kill        # kill the debuggee JVM
quit        # leave gdb
```

---

## 12. Bonus — HotSpot's built‑in `break` via a JVM argument

HotSpot has a C++ `break` facility you can arm from the command line.
`-XX:CompileCommand=break,<method>` makes the VM call the (empty) `os::breakpoint()` function so a
debugger can stop:

```
-XX:CompileCommand=break,agent/MetaspaceLeakAgent.redefine
```

```gdb
break os::breakpoint      # or: break breakpoint
```

⚠️ Caveat: this fires when the method is *compiled*, on the **compiler thread** — noisy in a loop that
constantly deoptimises. The native breakpoints in §4–§8 are the reliable, interpreter‑friendly way to
stop in the hot loop; prefer those for the live demo.

---

## Quick reference

| Goal | Command |
| --- | --- |
| Ignore JVM signals | `handle SIGSEGV nostop noprint pass` (also `SIGBUS`, `SIGFPE`) |
| Session options | `set pagination off` · `set confirm off` · `set overload-resolution off` |
| **Show C++ source here** | `list` (also `list <func>` / `list <file:line>`) |
| **Live source window** | `layout src` … `tui disable` (`refresh` / Ctrl‑L to redraw) |
| More source lines | `set listsize 30` |
| Variables in frame | `info args` · `info locals` |
| **All fields of an object** | `ptype the_class` (types) · `p *the_class` (values) · `explore the_class` |
| ① Break in hot loop (main thread) | `break JvmtiEnv::RedefineClasses` |
| Start / resume | `run` / `continue` (`c`) |
| VM helper index | `call (void)help()` |
| Java stack | `call (void)ps()` |
| Native + Java stack | `call (void)pns($sp,$rbp,$pc)` |
| ② Break in CP merge | `break VM_RedefineClasses::merge_cp_and_rewrite` |
| **CP length (grows!)** | `p the_class->constants()->length()` |
| CP length (field fallback) | `p the_class->_constants->_length` |
| Incoming version / worst case | `p scratch_class->constants()->length()` · `p merge_cp_length` |
| ③ Metaspace allocation | `tbreak ConstantPool::allocate` → `p length` |
| Second (shrunken) pool | `break VM_RedefineClasses::set_new_constant_pool` |
| ④ Merged pool installed | `break jvmtiRedefineClasses.cpp:4278` |
| ⑤ Retirement path | `break InstanceKlass::add_previous_version` |
| On‑stack? / EMCP methods | `p scratch_class->constants()->on_stack()` · `p emcp_method_count` |
| Retained‑versions chain | `p this->previous_versions()` |
| CP cache / resolved refs | `p the_class->constants()->cache()` · `->resolved_references()` |
| Native backtrace | `bt` / `bt 3` |
| Breakpoints / threads | `info breakpoints` / `info threads` |
| Clear the screen | `Ctrl‑L` |
| Stop everything | `kill` then `quit` (or leave the slide) |
