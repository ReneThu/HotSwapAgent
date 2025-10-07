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
        transform: scale(0.3);
    }
    .centerQrCOde {
        justify-items: anchor-center;
        transform: scale(0.3);
    }
    </style>
    
    
    <h1 class="headline">How does code hot swapping work,<br /> and what are its pitfalls?</h1><br />
    <h2 class="headline-smol">Marco Sussitz</h2>
    
    <div class="addonestuff">Senior Software developer at Dynatrace</div>
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
            <span v-mark.underscore.orange=6>
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
    <h1>How can you dynamically change Java classes?</h1>
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
    <h1>Is there a better way?</h1>
    </v-clicks>
    
    <div>
      <ul>
        <li v-click>Java Agents</li>
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
    
    ```java{all}
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
    
    ```java{all}
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
            //TODO
            return null;
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
        <li v-click>The Java compiler Javac</li>
        <li v-click>The compiler API javax.tools</li>
        <li v-click>ASM</li>
        <li v-click>
            <span v-mark.underscore.orange=7>
                Javassist
            </span>
        </li>
        <li v-click>
            <span v-mark.underscore.orange=7>
                Byte Buddy
            </span>
        </li>
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
            <v-clicks at="2">
                <li>For observability
                    <ul>
                        <v-clicks at="3">
                            <li>Turn tracing on or off</li>
                        </v-clicks>
                        <v-clicks at="4">
                            <li>Capture local variables</li>
                        </v-clicks>
                    </ul>
                </li>
            </v-clicks>
            <v-clicks at="5">
                <li>For development
                    <ul>
                        <v-clicks at="6">
                            <li>Debugging locally</li>
                        </v-clicks>
                        <v-clicks at="7">
                            <li>Fast code changes</li>
                        </v-clicks>
                    </ul>
                </li>
            </v-clicks>
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
        <li v-click>Attach a Java agent to a JVM</li>
        <li v-click>Trigger a class reload</li>
        <li v-click>Generate our new bytecode</li>
      </ul>
    </div>
    
    ---
    transition: none
    ---
    
    <style>
    
    .centerLogoFinalSlide {
        transform: scale(0.52);
    }
    .centerQrCOde {
        transform: scale(0.2);
    }
    </style>
    
    
    
    <div class="size-full grid grid-rows-1 gap-16 justify-items-center">
    <div class="flex gap-16 -mb-16 justify-center items-center">
    <v-clicks>
      <img class="h-full" src="./pictures/qr_code.png" alt=""/>
    </v-clicks>
    </div>
    <div class="centerLogoFinalSlide">
      <img src="./pictures/Dynatrace_Logo.png" alt=""/>
    </div>
    </div>
    
    
    <!--
    This has been added for a commbined talked
    -->
    
    ---
    layout: center
    transition: slide-up
    ---
    
    <v-click>
      <h1 class="addonestuff">Live Debugger</h1>
    </v-click>
    <v-click>
      <h2>Access the complet state of your application without restarting</h2>
    </v-click>
    
    <!--
    I am sure you know the feeling when you are testing new code with an amazing functionality.
    
    A couple of Months ago I did just that. I was working on some fancy new feature called the Live Debugger
    
    Long story short with this you can access the state of your application without restarting it. And you could even use it with an ok overhead in a production environment.
    -->
    
    ---
    layout: center
    transition: slide-up
    ---
    
    <v-clicks>
    <v-click hide at="2">
      <h1>What does it do?</h1>
    </v-click>
    </v-clicks>
    
    
    <v-clicks at="1">
    <div>
    ````md magic-move{lines: true}
    ```java{all|all|all|3|all}
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
      <h1>How would this look like?</h1>
    </v-clicks>
    
    
    <v-clicks at="2">
    <div>
    ````md magic-move{lines: true}
    ```java{all|all|4|all}
    public class HotSwapAgent {
        public static void premain(
                String arguments,
                Instrumentation inst) {
        }
    }
    ```
    
    ```java{all}
    public class HotSwapAgent {
        private static Instrumentation instrumentation;
    
        public static void premain(
                String arguments,
                Instrumentation inst) {
            instrumentation = inst;
        }
    }
    ```
    
    ```java{all}
    public class HotSwapAgent {
        private static Instrumentation instrumentation;
    
        public static void premain(
                String arguments,
                Instrumentation inst) {
            instrumentation = inst;
        }
    
        public static void triggerHotSwap(
                Class<?> clazz,
                String classFilePath) {
            byte[] classBytes = getnewByteCode();
            ClassDefinition def = new ClassDefinition(clazz, classBytes);
            instrumentation.redefineClasses(def);
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
    transition: slide-up
    ---
    
    <v-clicks>
        <h1>Heap space</h1>
        <div >
          <img src="./pictures/HeapSpace.png" alt=""/>
        </div>
        <div >
          <img src="./pictures/HeapSpacePostGc.png" alt=""/>
        </div>
    </v-clicks>
    
    ---
    layout: center
    transition: slide-up
    ---
    
    <v-clicks>
        <h1>Meta space</h1>
        <div >
          <img src="./pictures/MetaSpace.png" alt=""/>
        </div>
    </v-clicks>
    
    ---
    layout: center
    transition: slide-up
    ---
    
    <v-clicks>
    <h1>
      <span v-mark.strike-through.red=2>
        We have a memory leak
      </span>
    </h1>
    </v-clicks>
    
    <v-clicks at=3>
      <h1>We have an opportunity for infinit growth</h1>
    </v-clicks>
    <v-clicks at=4>
      <h1>🥳🥳🥳🥳🥳🥳🥳</h1>
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
    layout: center
    transition: slide-up
    ---