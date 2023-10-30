# A Tutorial on J-NVM

The tutorial below explains how to use [J-NVM](https://github.com/jnvm-project/jnvm) with IntelliJ (>= 2022.1).
It follows the running example in the slides [here](https://docs.google.com/presentation/d/1YGDK3urnd682Qbk_IQxq9kyZppJUARxna4K_vCgf140/edit?usp=sharing).
Please note that some of the class names are different---as the slides make some necessary simplifications. 

## Before we start

First, we install J-NVM locally for IntelliJ.
We also create a block on disk that serve to mimic persistent memory (PMEM).
(Of course, if you have actual PMEM feel free to use it!).
These commands are to be executed from the root of the project.

````
mvn install:install-file -Dfile=src/main/resources/jnvm-core-1.0-SNAPSHOT.jar -DgroupId=eu.telecomsudparis.jnvm -DartifactId=jnvm-core -Dversion=1.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

dd if=/dev/zero of=/tmp/pmem0 bs=1024 count=1024; sync
````

## A Simple persistent class

Below, we create a basic class then re-write it to be persistent with J-NVM.

1. The Simple class

Create a class named `Simple` in the `eu.telcomsudparis.jnvm` package.
This class holds a unique integer field `x`.
Add a constructor to the class that takes as parameter an integer and initializes `x` appropriately.
Include accessors `getX` and `setX` to access field `x`, as well as a method `inc()` that increments `x` by one.
In addition, override `toString` to return the value of `x`. 

2. Making the class persistent

We are now going to make Simple persistent with J-NVM.

3. Copy the code of Simple.java into a new class named `OffHeapSimple`.

The layout (in persistent memory) for `OffHeapSimple` will be as follows:

````
/* PMEM Layout :
*  | Index | Offset | Bytes | Name    |
*  |-------+--------+-------+---------|
*  | 0     | 0      | 4     | x       |
*  end: 8 bytes
*/
````

4. Define a private static final array `long[] offsets` that stores the offset of the unique field as defined above.
In addition, add a private static final integer `SIZE` to hold the size of an `OffHeapSimple` object in persistent memory.
The size of an integer in memory is given by the native `Integer.BYTES`.

5. Remove the single attribute of `OffHeapSimple`.
Change the logic in the setters and getters, to use direct access to the persistent memory.
To this end, we will use the following methods provided by the parent class (OffHeapObjectHandle).

````
setLongField()
getLongField(int offset)
````

Add then an implementation of `inc()` using the accessors.
Correct the code of the constructor to use the setter.

6. Complete your class by making it extend `OffHeapObjectHandle`.
Then add the code below to your class.

````
    // resurrector
    public OffHeapSimple(Void __unused, long offset){
        super(offset);
    }

    @Override
    public long size() {
        return SIZE;
    }

    @Override
    public long classId() {
        return CLASS_ID;
    }

    @Override
    public void descend() {
    }

    @Override
    public void destroy() {
        super.destroy();
    }
````

7. We are now ready to store/load `OffHeapSimple` instances to/from persistent memory.
Add the following program to the `OffHeapSimple` class.

````
    public static void main(String[] args)  {
        OffHeap.finishInit();
        RecoverableMap<OffHeapString, OffHeapObject> root = OffHeap.rootInstances;
        OffHeapString name = new OffHeapString("simple");
        OffHeapSimple simple;
        if (root.size()==0) {
            simple = new OffHeapSimple(42);
            root.put(name, simple);
        } else {
            simple = (OffHeapSimple) root.get(name);
            simple.inc();
            System.out.println(simple);
        }
    }
````

J-NVM works with custom Java 8 Virtual Machine.
For that reason, we will run everything into a Docker image.
This [page](https://www.jetbrains.com/help/idea/running-a-java-app-in-a-container.html) explains how to do so with IntelliJ.
We will use the following Docker image : `0track/jnvm-jdk:latest`.

In the run options of Docker, do not forget to mount the persistent memory.  
This is done as detailed below.
To reset the memory, you can use the `dd` command at the beginning of this tutorial.

````
--entrypoint= -v /tmp/pmem0:/pmem0
````

