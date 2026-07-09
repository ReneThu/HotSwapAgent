---
# You can also start simply with 'default'
#theme: seriph
# random image from a curated Unsplash collection by Anthony
# like them? see https://unsplash.com/collections/94734566/slidev
#background: https://cover.sli.dev
# some information about your slides (markdown enabled)
title: Swapping Code, Losing Memory A JVM Deep Dive
info: |
  ## Slidev Starter Template
  Presentation slides for developers.

  Learn more at [Sli.dev](https://sli.dev)
# apply unocss classes to the current slide

# https://sli.dev/features/drawing
drawings:
  persist: false
# slide transition: https://sli.dev/guide/animations.html#slide-transitions
transition: slide-up
# enable MDC Syntax: https://sli.dev/features/mdc
mdc: true
# <!-- Empty slide -->
layout: center
---

<style>
.headline {
    font-weight: 1000;
    text-align: center;
    font-size: 50px;
}

.headline-smol {
    font-weight: bold;
    text-align: center;
}

.addonestuff {
    font-weight: bold;
    text-align: center;
}
.centerLogo {
    justify-items: anchor-center;
    transform: scale(0.3);
}
.centerQrCOde {
    justify-items: anchor-center;
    transform: scale(0.3);
}
.image-container {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    margin-top: 1rem;
}
.image-container img {
    max-width: 90%;
    max-height: 480px;
    object-fit: contain;
}
</style>


<h1 class="headline">Swapping Code, Losing Memory: A JVM Deep Dive</h1><br />
<h2 class="headline-smol">Marco Sussitz</h2>

<div class="addonestuff">Senior Software developer at Dynatrace</div>
<br /><br />
  <div class="centerLogo">
      <img src="/Dynatrace_Logo.png" alt=""/>
  </div>

---
layout: center
transition: none
---

<v-clicks at="1">
<div>
```java{all|all|5-8|12}
package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        while (true) {
            Thread.sleep(1000);
            printHelloWorld();
        }
}

    public static void printHelloWorld() {
        System.out.println("Hello, World!");
    }
}
```
</div>
</v-clicks>

<!--
Lets start this right away with a little demo
-->

---
layout: two-cols
class: demo-slide
layoutClass: demo-layout
transition: none
---

<Counter />

::right::

<DemoIframe url="http://localhost:8080/class/list" />

<style>
:global(.demo-layout) {
  grid-template-rows: minmax(0, 1fr);
  grid-template-columns: minmax(0, 0.72fr) minmax(0, 1.28fr);
  align-items: stretch;
}
:global(.col-left.demo-slide),
:global(.col-right.demo-slide) {
  height: 100%;
  min-height: 0;
  padding: 0.8rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  overflow: hidden;
}
</style>



---
layout: center
transition: none
---

<v-clicks>

<h1>How does this work?</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<div class="image-container"><img src="/MercyMeme.png" alt="" /></div>

</v-clicks>

---
layout: center
transition: none
---

<v-click>
  <h1>What is the JVMTI?</h1>
</v-click>

<div>
  <ul>
    <li v-click>Place breakpoints</li>
    <li v-click>Read variables</li>
    <li v-click>Hot swap code</li>
    <li v-click>Many more</li>
  </ul>
</div>

---
layout: center
transition: none
---

<v-clicks>
<h1>How can you dynamically change Java classes?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>Observe when a class is loaded and change it</li>
    <li v-click>Trigger a reload of a class</li>
  </ul>
</div>


---
layout: center
transition: none
---

<v-click>
  <h1>Observe when classes are loaded</h1>
</v-click>

<v-clicks>
<div>
```java{all}
void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env,
        JNIEnv* jni_env,
        jclass class_being_redefined,
        jobject loader,
        const char* name,
        jobject protection_domain,
        jint class_data_len,
        const unsigned char* class_data,
        jint* new_class_data_len,
        unsigned char** new_class_data)
```
</div>

</v-clicks>

---
layout: center
transition: none
---

<v-click>
  <h1>Trigger a class reload</h1>
</v-click>

<v-clicks>
<div>

```java{all}
jvmtiError
RetransformClasses(jvmtiEnv* env,
            jint class_count,
            const jclass* classes)
```
</div>
</v-clicks>

<v-clicks>
<div>
```java{all}
typedef struct {
    jclass klass;
    jint class_byte_count;
    const unsigned char* class_bytes;
} jvmtiClassDefinition;

jvmtiError
RedefineClasses(jvmtiEnv* env,
    jint class_count,
    const jvmtiClassDefinition* class_definitions)
```
</div>
</v-clicks>


---
layout: center
transition: none
---

<v-clicks>

````md magic-move{lines: true}
```java{all}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
    } 
}
```

```java{all}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
        instrumentation.addTransformer(new HotSwapTransformer(), true);
    }
    
    public static void hotSwapClass(Class<?> klass) throws UnmodifiableClassException {
        instrumentation.retransformClasses(klass);
    }
}
```
````
</v-clicks>

---
layout: center
transition: none
---

<v-clicks at="1">
<div>
````md magic-move{lines: true}
```java{all|all|3-7|3|4|5|6|7|8|all}
public class HotSwapTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        return transformClassFile(classfileBuffer);
    }
}
```
```java{all}
public class HotSwapTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        return transformClassFile(classfileBuffer);
    }
    
    public byte[] transformClassFile(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new CaptureInjector(writer), 0);
        return writer.toByteArray();
    }
}
```
````
</div>
</v-clicks>

---
layout: center
transition: none
---

<v-clicks>
<h1>Where do we get the bytecode from?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>The Java compiler Javac</li>
    <li v-click>The compiler API javax.tools</li>
    <li v-click>Javassist</li>
    <li v-click>
        <span v-mark.underscore.orange=7>
            ASM
        </span>
    </li>
    <li v-click>
            Byte Buddy
    </li>
  </ul>
</div>

---
layout: center
transition: slide-up
---

<v-clicks>
  <h1>What does it do?</h1>
</v-clicks>


<v-clicks at="2">
<div>
````md magic-move{lines: true}
```java{all|all|3|all}
public class Main {
    public static void print(String message) {
        System.out.print(message);
    } 
}
```

```java{all}
public class Main {
    public static void print(String message) {
        Capture.log(message);
        System.out.print(message);
    } 
}
```
````
</div>
</v-clicks>


---
layout: center
transition: slide-up
---

<v-clicks>
<h1>We got a Problem!</h1>
<div style="font-size: 8rem; line-height: 1;">😿</div>
</v-clicks>


---
layout: center
transition: slide-up
---

<v-clicks>
<h1>How many breakpoints can you place?</h1>
</v-clicks>

<v-click>
<h2 style="display: inline-block; margin: 0;">a) 500</h2><br />
</v-click>

<v-click>
<span v-mark.highlight.orange=6>
<h2 style="display: inline-block; margin: 0;">b) 4000</h2>
</span>
</v-click>
<v-click at=7>
<span style="display: inline-block; margin-left: 8px;">(With my setup on my dev machine)</span>
</v-click>
<br />

<v-click>
<h2 style="display: inline-block; margin: 0;">c) 13000</h2><br />
</v-click>

<v-click>
<h2 style="display: inline-block; margin: 0;">d) There is no limit</h2>
</v-click>

---
layout: center
transition: none
---

<v-clicks>
<div class="image-container"><img src="/HeapMemoryUsed.png" alt="" /></div>
</v-clicks>

---
layout: center
transition: none
---

<v-clicks>
<div class="image-container"><img src="/MetaSpaceMemoryUsed.png" alt="" /></div>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks>
<div class="image-container"><img src="/MobbyDickMeme.png" alt="" /></div>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks>
<h1>We need a reproducer!</h1>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks at="1">
<div>
````md magic-move{lines: true}
```java{all|all}
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
    }
}
```

```java{all}
public class Main {
    public static void main(String[] args) throws Exception {
        printHelloWorld();
    }
    
    public static void printHelloWorld() {
        System.out.println("Hello, World!");
    }
}
```

```java{all}
public class Main {
    public static void main(String[] args) throws Exception {
        while (true) {
            Thread.sleep(100);
            printHelloWorld();
        }
    }
    
    public static void printHelloWorld() {
        System.out.println("Hello, World!");
    }
}
```

```java{all}
public class Main {
    public static void main(String[] args) throws Exception {
        while (true) {
            Thread.sleep(100);
            printHelloWorld();
        }
    }
    
    public static void printHelloWorld() {
        System.out.println("Hello, World!");

        try {
            Class<?> agentClass = Class.forName("com.agent.HotSwapAgent");
            java.lang.reflect.Method method = agentClass.getMethod(
                    "triggerHotSwap", Class.class, String.class
            );
            method.invoke(null, Main.class, pathToClassFilesLocation);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
```
````
</div>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks>
    <h1>What is the meta space</h1>
    <h2>Metaspace is a native (as in: off-heap) memory manager in hotspot. It is used to manage memory for class metadata. Class metadata are allocated when classes are loaded. </h2>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks>
    <h1>Check the specs</h1>
    <h2>- The Java Language Specification</h2>
    <h2>- The Java Virtual Machine Specification</h2>
    <h2>- The JVMTI documentation</h2>
</v-clicks>


---
layout: center
transition: slide-up
---

```java{all}
typedef struct {
    jclass klass;
    jint class_byte_count;
    const unsigned char* class_bytes;
} jvmtiClassDefinition;

jvmtiError
RedefineClasses(jvmtiEnv* env,
            jint class_count,
            const jvmtiClassDefinition* class_definitions)
```

---
layout: center
transition: slide-up
---

<v-click>
    <h1>Redefine Classes</h1>
</v-click>

<v-click>
    <h2>Redefinition can cause new versions of methods to be installed.</h2>
    <h2><span v-mark.underline.orange=3>Old method versions may become obsolete.</span></h2>
    <h2>The new method version will be used on new invokes.</h2>
    <h2>If a method has active stack frames, those active frames continue to run the bytecodes of the original method version.</h2>
    <h2>If resetting of stack frames is desired, use PopFrame to pop frames with obsolete method versions.</h2>
</v-click>

<v-click>
<br />
</v-click>

---
layout: center
transition: slide-up
---

<v-clicks>
    <h1>OpenJDK Wiki</h1>
    <h2>It is used to manage memory for class metadata. Class metadata are allocated when classes are loaded.
    Their lifetime is usually scoped to that of the loading classloader - when a loader gets collected, all class metadata it accumulated are released in bulk.</h2>
</v-clicks>

---
layout: center
transition: slide-up
---

<v-clicks>
    <h1>How do you debug your JVM?</h1>
    <h2 style="font-family: monospace; background-color: #525252;  padding: 10px; border-radius: 5px; display: inline-block;">git clone https://git.openjdk.org/jdk</h2>
    <h2 style="font-family: monospace; background-color: #525252;  padding: 10px; border-radius: 5px; display: inline-block;">bash configure --with-debug-level=slowdebug</h2>
    <h2 style="font-family: monospace; background-color: #525252;  padding: 10px; border-radius: 5px; display: inline-block;">make images</h2>
    <h2>Enjoy?</h2>
</v-clicks>

---
layout: default
class: gdb-slide
transition: slide-up
---

<GdbTerminal url="http://localhost:8081/gdb" />

<style>
:global(.slidev-layout.gdb-slide) {
  padding: 0 !important;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #1e1e2e;
}
</style>

<!--
Live GDB demo against the slowdebug JVM running the metaspace leak. See GDB-CHEATSHEET.md.
The terminal starts the session itself: on entering the slide it shows the gdb start command and
waits — press Enter to launch gdb. Leaving the slide stops gdb and the slowdebug JVM.

1. Press Enter to launch gdb (SIGSEGV/SIGBUS/SIGFPE pass through).
2. `break JvmtiEnv::RedefineClasses` — a breakpoint in the hot loop / agent redefine call.
3. `run` — gdb stops at the first redefinition on the main thread.
4. `call (void)help()` / `call (void)ps()` / `call (void)pns($sp,$rbp,$pc)` — VM + Java stacks.
5. `break VM_RedefineClasses::merge_cp_and_rewrite`, then `continue` — the constant-pool merge.
   `p the_class->constants()->length()` grows on every `continue` (the leak the audience watches).
6. `break InstanceKlass::add_previous_version` — `p scratch_class->constants()->on_stack()` is true,
   so every old version is retained (the deeper leak). Walk `p this->previous_versions()`.
7. `delete` the breakpoints and `continue` to let metaspace balloon.
-->

---
layout: center
transition: slide-up
---

<v-click>
    <div class="image-container"><img src="/rating.png" alt="" /></div>
</v-click>

---
layout: center
---

<h1 class="headline">Questions?</h1>

<br />
<div class="centerLogo">
    <img src="/Dynatrace_Logo.png" alt=""/>
</div>