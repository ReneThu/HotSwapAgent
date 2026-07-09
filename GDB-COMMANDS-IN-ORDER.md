# GDB Commands — In Order

The exact gdb commands for the live metaspace‑leak demo, in the order you type them, top to bottom.
This is just the commands; see `GDB-CHEATSHEET.md` for the explanations, expected output and the "why".

Signals (`SIGSEGV` / `SIGBUS` / `SIGFPE`) and the session options (`set pagination off`,
`set overload-resolution off`, …) are applied automatically by `gdb-init.gdb` — you start at step 2.

```gdb
# 1. Launch — press Enter on the slide.
#    (runs: gdb -x gdb-init.gdb --args java -javaagent:agent.jar -cp swaps/00000 demo.Leaker)

# 2. Breakpoint ① — the hot loop (agent redefine call), then start the JVM.
break JvmtiEnv::RedefineClasses
run

# 3. Show the stacks with HotSpot's own helpers.
call (void)help()
call (void)ps()
call (void)pns($sp,$rbp,$pc)

# 4. Breakpoint ② — the constant‑pool merge (the money shot).
break VM_RedefineClasses::merge_cp_and_rewrite
continue

# 5. See the C++ you are stopped in + the variables in scope.
list
info args

# 6. List every field of the live class — this is where the `_constants` field lives.
ptype the_class
p *the_class

# 7. The number that grows on every hotswap.
p the_class->constants()->length()
p scratch_class->constants()->length()
p merge_cp_length

# 8. (optional) Live source window that follows execution.
layout src

# 9. Watch it grow — repeat this pair a handful of times.
continue
p the_class->constants()->length()
bt 3

# 10. Breakpoint ③ — the metaspace allocation itself.
tbreak ConstantPool::allocate
continue
p length
bt 3

# 11. Breakpoint ④ — the grown pool is installed on the live class.
break jvmtiRedefineClasses.cpp:4278
continue
p the_class->constants()->length()

# 12. Breakpoint ⑤ — the retirement path (the deeper leak).
break InstanceKlass::add_previous_version
continue
p scratch_class->constants()->on_stack()
p this->previous_versions()

# 13. Let metaspace balloon for the audience.
info breakpoints
delete
continue

# 14. Teardown (or just leave the slide).
kill
quit
```
