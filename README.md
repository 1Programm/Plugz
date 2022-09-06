# Plugz Version 2

A lightweight, self-made spring-boot clone that focuses more on the simplicity of "plugging" different dependencies together.
It uses a method to discover all runtime paths to scan all classes for specific annotations.

The "plugz-magic" is the core module which provides the most important class: the "MagicEnvironment"



A minimal example:

```java
import com.programm.plugz.magic.MagicEnvironment;

public static void main(String[] args){
   MagicEnvironment.Start(args);
}
```

This will set- and start-up the Environment.

The Environment will scan for classes with the @Config and @Service annotations and instantiate them.

These instantiated classes can be used to declare "lifecycle - Methods" which can be declared by annotating some method with the following annotations: @PreSetup, @PostSetup, @PreStartup, @PostStartup, @PreShutdown, @PostShutdown.
