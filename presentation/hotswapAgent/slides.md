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
I think that every programmer has their something they like the most. Their little opsesion.
That might be a favorite technology, a favorite langues, a favorite tool.

FOr me that would be Java. I know not everyone likes it but it is by far my favoirte progamming langues.
ANd what I like the most about it is defently the JVM. I just love what you can do with it. The virutal machin gives you the option to do so many things.

And I have done a lot of mischife with the JVM

THis is why I was escpally happy to be asked to work on the liveDebugger at our comapny. Long story short this tool alows you to look into your runny application.
But it is honestly not too imortaned what you can do with it for this talk.

The importaned thing is that it used run time generated code and code hotswaping. 
Two of my faorite things.

So I got right to work.

Lets imagine this little sample progamm

......
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

<!--
How does it work?


Next slide

with the JVMTI
-->

---
layout: center
transition: none
---

<v-clicks>

<div class="image-container"><img src="/MercyMeme.png" alt="" /></div>

</v-clicks>

<!--
Who here has worked with the JVMTI before?
-->

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

<!--
You need two things!
-->


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

<!--
But I am not going to torutoe you with too much C++ for now we can look at Java code.
-->

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
        return todoImplement();
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


<!--
So what does the LiveDebugger do to a method?
-->

---
layout: center
transition: slide-up
---

<v-clicks>
<h1>We got a Problem!</h1>
<div style="font-size: 8rem; line-height: 1;">😿</div>
</v-clicks>


<!--
Tell story how I added a jenkins memory leak test.

But we have a problem I did manule testing.
-->

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

<!--
Every good story needs a villel. And I have just found mine.

Question to the audince what was the longes amound of time you have ever spend on a bug?
One week? One month? two months? longer?

I thing I worked on this for almost two months when I first found it. Not non stop but I keeped it on my mind.
This was my white whale.

And hunting such a bug is very different from a normal bug.
To fix such a problem we need to pay blood.

NEXT SLIDE!!!!!!!!

We have to give some thing up. And the first thing I gave up was my time.


-->

---
layout: center
transition: slide-up
---

<v-clicks>
<h1>We need a reproducer!</h1>
</v-clicks>

<!--
We have to give some thing up. And the first thing I gave up was my time.
Click!!!

We need a repdocuer.
-->
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


<!--
But even with access to a repducer I was not able to find the problem. I found a JVM bug which was already fix on a later JVM
I found a bunch of other stuff but nothing helped me in understanding why excetly the meta space grew and if I could even fix this.

So I had to give up another thing. MY Agency. I spend hours reding documentation about the meta space

quick refresher what the neta space is. It is a repclament for the permGen 
-->

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


<!--
SO I checkt the JLS and the JVMS but nothing they dont talk about hotswapping at all
But the JVMTI was already a bit of a hit. It would hind at the problem already.
-->

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

<!--
So everything that we have tried so far has faild. We where not able to figruou out why this is happening and if we can stop it.
In a heors jounry this would be the Abyss. The lowest point of our story.
The last step before our hero solves the problem.

But this is not a Heros jounre. It is a fight agains nature or a machine in this case.
And I have one mor thing I can sacrifies my sanatie.


Some of you might have noticed that we are deep in implementation details of our virtual machine.
THose bevoirues are not covert by any specs. And if your progam relais on those things you made mutible bad desitions.
But sometimes you gotte do what you gotte do.


At the end of the day the JVM is also just a prgoam. A program that is written in a prommaing lanuges.
C++ in our case. And we can debug that. With GDB for example. 

So I did just that!


Explain how to debug a JVM
-->

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
break JvmtiEnv::RedefineClasses

run

call help()

call ps()

call pns($sp,$rbp,$pc)

break VM_RedefineClasses::merge_cp_and_rewrite

continue

list

info args

ptype the_class

p *the_class

p the_class->constants()->length()
p scratch_class->constants()->length()
p merge_cp_length

info b
dis b
-->

---
layout: center
transition: slide-up
---

<v-click>
<h1>Volker Simonis - HotSpot Debugging at the OS Level</h1>
    <div class="image-container"><img src="/gdbDebugging.png" alt="" /></div>
</v-click>

---
layout: center
transition: slide-up
---

<v-click>
    <h1>What was the problem?</h1>
</v-click>

<v-clicks>

````md magic-move{lines: true}
```text{all|2}
Constant pool:
  #1 = String             #1            
  #2 = Utf8               {...........}
```
```text{2}
Constant pool:
  #1 = String             #2            
  #2 = Utf8               {...........}
```
```text{2,4}
Constant pool:
  #1 = String             #3            
  #2 = Utf8               {...........}
```
```text{2,4,6}
Constant pool:
  #1 = String             #4            
  #2 = Utf8               {...........}
```
```text{2,4,6,8}
Constant pool:
  #1 = String             #5            
  #2 = Utf8               {...........}
```
````

<!--
Because of this single thing the class meta data for this one class blow up
If I would remove and add a single breakpoint a 100 times there would be a 100 different constants
in the constand pool and a bunch of unecesary data would be stored.
-->

</v-clicks>

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