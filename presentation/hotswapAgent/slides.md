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
</style>


<h1 class="headline">How does code hot swapping work, and what are its pitfalls?</h1><br />
<h2 class="headline-smol">Marco Sussitz</h2>

<div class="addonestuff">Software developer at Dynatrace</div>
<br /><br />
<div class="addonestuff">https://www.sussitzm.com</div>

---
layout: center
transition: none
---

<h1>TODO add grid to show sample code and the web page</h1>

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
    <li v-click>Linking</li>
    <li v-click>Verify</li>
    <li v-click>Prepare</li>
    <li v-click>(Optionally) Resolve</li>
  </ul>
</div>

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

---
layout: center
transition: none
---

<v-clicks>

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

</v-clicks>

---
layout: center
transition: none
---

```java{all}
jvmtiError
RetransformClasses(jvmtiEnv* env,
            jint class_count,
            const jclass* classes)
```

---
layout: center
transition: none
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
transition: none
---

<v-click>
  <div class="image">
      <img src="./pictures/sufferFromCpp.png" alt=""/>
  </div>
</v-click>

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
  </ul>
</div>

---
layout: center
transition: none
---

````md magic-move {lines: true}
```java{all}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
    } n
}
```

```java{all}
public class HotSwapAgent {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation instrumentationObject) {
        instrumentation = instrumentationObject;
    }
    
    public static void hotSwapClass(Class<?> classRef, byte[] classfileBuffer)
            throws UnmodifiableClassException, ClassNotFoundException {
        instrumentationObject.redefineClasses(new ClassDefinition(classRef, classfileBuffer));
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
    
    public static void hotSwapClass(Class<?> classRef) throws UnmodifiableClassException {
        instrumentationObject.retransformClasses(classRef);
    }
}
```
````

---
layout: center
transition: none
---

```java{all}
public class HotSwapTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return transformClassFile(classfileBuffer);
    }
}
```

---
layout: center
transition: none
---

<v-clicks>

<h1>Show the transform as well as the redefine calls so 3 functions.</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>So to recape we need 2 things to performe a hotswap</h1>
<h2>Trigger the hotswap via one of the two APIs</h2>
<h2>valied bydecode TODO easy right? yeah but getting the bydeocde is hard.</h2>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>Where do we get correct bydecode from? Javac or we use ASM or a like to genberate it on the fly.</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>Hot swapping is great and super easy!</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>If you actully start using it you will see excpetions and errors that you have never seen before.</h1>
<h1>And you cannot to everything. There are some liminations to what you can change in the class file</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>TODO add a list of limitations</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>Also every hot swap has a memeory impact on your JVM this will cost you memeory in the meta space.</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>We use it to dynamicly capture local variable in some situations.</h1>

</v-clicks>

---
layout: center
transition: none
---

<v-clicks>

<h1>We use it to dynamicly capture local variable in some situations.</h1>

</v-clicks>
