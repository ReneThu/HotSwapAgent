---
# You can also start simply with 'default'
#theme: seriph
# random image from a curated Unsplash collection by Anthony
# like them? see https://unsplash.com/collections/94734566/slidev
#background: https://cover.sli.dev
# some information about your slides (markdown enabled)
title: How does code hot swapping work, and what are its pitfalls?
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
    transform: scale(0.7);
}
.centerQrCOde {
    justify-items: anchor-center;
    transform: scale(0.3);
}
</style>


<h1 class="headline">How does code hot swapping work,<br /> and what are its pitfalls?</h1><br />
<h2 class="headline-smol">Marco Sussitz</h2>

<div class="addonestuff">Software developer at Dynatrace</div>
<br /><br />
  <div class="centerLogo">
      <img src="./pictures/Dynatrace_Logo.png" alt=""/>
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

---
layout: iframe-right
url: http://localhost:8080/class/list

transition: none

class: zoom-custom-85
---

<style>
.zoom-custom-85 {
    zoom: 0.85
}
</style>




<div class="grid grid-rows-[auto_auto_1fr]">
<div>
```java{all}
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
<Counter />
</div>



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

<v-click>
  <div class="image">
      <img src="./pictures/applePie.png" alt=""/>
  </div>
</v-click>

<!--
There is qute from Carl Sagan.
If you wish to make an apple pie from scratch, you must first invent the universe.

If we really want to understand how code hotswapping works we first need to understand
how classes are loaded in the JVM
-->

---
layout: center
transition: none
---

<v-click>
  <h1>What happens during class loading?</h1>
</v-click>

<div>
  <ul>
      <li v-click>
        <span v-mark.underscore.orange=7>
          Loading
        </span>
      </li>
    <li v-click>Verify</li>
    <li v-click>Prepare</li>
    <li v-click>(Optionally) Resolve</li>
  </ul>
</div>

<!--
chaper 12 of the java langues specification as well as chaper 5 of the java virtual machine specification talk about that.
So there are 5 steps that are taken.

The first one loading. 
So if a class is requested that is not laoded classloader will be used to look for a bianry representation of the class.

Verification:
This means that the class is checkt that it is well-formated. So with a proper symbol table and so on.

Preparation:
static storage and any data structures

Resolution(optional):

checking symbolic references from the class to other
classes and interfaces.

The Loading step is the one we are interested in.
-->

---
layout: center
transition: none
---

<v-clicks>
<h1>How can you dynamically change java classes?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>With the JVMTI!</li>
  </ul>
</div>

<!--
How here as worked with the JVMTI before raise of hand?
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
```
</div>
</v-clicks>


---
layout: center
transition: none
---

<v-clicks>
<h1>Is there a better way?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>Agents</li>
    <li v-click>java.lang.instrument</li>
    <li v-click>java -javaagent:agent.jar -jar helloWorld.jar</li>
  </ul>
</div>

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

```java{all|all|7-11}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
    }
    
    public static void hotSwapClass(Class<?> klass, byte[] classfileBuffer)
            throws UnmodifiableClassException, ClassNotFoundException {
        instrumentationObject.redefineClasses(new ClassDefinition(klass, classfileBuffer));
    }
}
```

```java{all|9|10|6}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
        instrumentation.addTransformer(new HotSwapTransformer(), true);
    }
    
    public static void hotSwapClass(Class<?> klass) throws UnmodifiableClassException {
        instrumentationObject.retransformClasses(klass);
    }
}
```
````
</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

```java{all|3-7|8}
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

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>
<h1>What do we need for a hotswap?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>Trigger the hotswap via one of the two APIs</li>
    <li v-click>
    <span v-mark.underscore.orange=4>
      Get a valid class representation from somewhere.
    </span>
  </li>
  </ul>
</div>

---
layout: center
transition: none
---

<v-clicks>
<h1>Where do we get the bytecode from?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>The java compiler javac</li>
    <li v-click>The compiler API javax.tools</li>
    <li v-click>ASM</li>
    <li v-click>Javassist</li>
    <li v-click>Byte Buddy</li>
  </ul>
</div>

---
layout: center
transition: none
---

<v-clicks>
<h1>What can you use it for?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>For observability
      <ul>
        <li>Turn tracing on or off</li>
        <li>Capture local variables</li>
      </ul>
    </li>
    <li v-click>For development
      <ul>
        <li>Debugging locally</li>
        <li>Fast code changes</li>
      </ul>
    </li>
  </ul>
</div>

---
layout: center
transition: none
---

<v-clicks>
<h1>What to watch out for?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>Cannot add or remove fields and methods</li>
    <li v-click>Cannot change the signature of methods</li>
    <li v-click>Cannot change the inheritance</li>
    <li v-click>Performance cost</li>
    <li v-click>Meta space cost</li>
    <li v-click>Generating bytecode is difficult</li>
  </ul>
</div>

---
layout: center
transition: none
---

<v-clicks>
<h1>What do we need for a hotswap?</h1>
</v-clicks>

<div>
  <ul>
    <li v-click>Attach a java agent to a JVM</li>
    <li v-click>Trigger a class reload</li>
    <li v-click>Generate our new bytecode</li>
  </ul>
</div>

---
transition: none
---

<style>

.centerLogoFinalSlide {
    transform: scale(0.34);
}
.centerQrCOde {
    transform: scale(0.2);
}
</style>



<div class="size-full grid grid-rows-2 gap-16 justify-items-center">
<div class="flex gap-16 -mb-16 justify-center items-center">
    <svg height="70%" aria-hidden="true" viewBox="0 0 24 24" version="1.1" width="70%" data-view-component="true" class="v-align-middle fill-white">
        <path d="M12.5.75C6.146.75 1 5.896 1 12.25c0 5.089 3.292 9.387 7.863 10.91.575.101.79-.244.79-.546 0-.273-.014-1.178-.014-2.142-2.889.532-3.636-.704-3.866-1.35-.13-.331-.69-1.352-1.18-1.625-.402-.216-.977-.748-.014-.762.906-.014 1.553.834 1.769 1.179 1.035 1.74 2.688 1.25 3.349.948.1-.747.402-1.25.733-1.538-2.559-.287-5.232-1.279-5.232-5.678 0-1.25.445-2.285 1.178-3.09-.115-.288-.517-1.467.115-3.048 0 0 .963-.302 3.163 1.179.92-.259 1.897-.388 2.875-.388.977 0 1.955.13 2.875.388 2.2-1.495 3.162-1.179 3.162-1.179.633 1.581.23 2.76.115 3.048.733.805 1.179 1.825 1.179 3.09 0 4.413-2.688 5.39-5.247 5.678.417.36.776 1.05.776 2.128 0 1.538-.014 2.774-.014 3.162 0 .302.216.662.79.547C20.709 21.637 24 17.324 24 12.25 24 5.896 18.854.75 12.5.75Z"></path>
    </svg>
  <img class="h-full" src="./pictures/qr_code.png" alt=""/>
</div>
<div class="centerLogoFinalSlide">
  <img src="./pictures/Dynatrace_Logo.png" alt=""/>
</div>
</div>

