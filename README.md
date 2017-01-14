Cuckoo
======

Cuckoo is a simple library that helps you implement the [Delegation pattern](https://en.wikipedia.org/wiki/Delegation_pattern).

Its API is inspired by [Kotlin's Class Delegation](https://kotlinlang.org/docs/reference/delegation.html).

How to use
----------

### Create Delegation Template

Say you have an interface `Greeter` and its implementation class `EnglishCreeter` like below,

```java
public interface Greeter {
    String sayHello();
    String sayHello(String toWhom);
}

public class EnglishGreeter implements Greeter {
    @Override
    public String sayHello() {
        return sayHello("ANONYMOUS");
    }

    @Override
    public String sayHello(String toWhom) {
        return String.format("Hello %s !", toWhom);
    }
}
```

Then create an *abstract* class like below using `@Delegate` and `@By` annotation.

```java
@Delegate
public abstract class EnglishGreeterWithRecording implements Greeter {
    @By
    final EnglishGreeter englishGreeter = new EnglishGreeter();

    @Override
    public String sayHello(String toWhom) {
        // do anything you like here
        return englishGreeter.sayHello(toWhom);
    }
}
```

Cuckoo automatically generates the code that overrides all unimplemented methods instead of you, with delegationg all the actual processing to the field annotated with `@By`.

You can use the generated class like below.

```java
EnglishGreeterWithRecording greeter = EnglishGreeterWithRecordingImpl.newInstance();

greeter.sayHello();
greeter.sayHello("SHIROYAMA");
```

If you have any constructors in your base class, Cuckoo generates the initializer methods with the same signature.

```java
@Delegate
public abstract class EnglishGreeterWithRecording implements Greeter {
    @By
    final EnglishGreeter englishGreeter;

    public EnglishGreeterWithRecording(EnglishGreeter englishGreeter) {
        this.englishGreeter = englishGreeter;
    }
}

// in your code
EnglishGreeterWithRecording greeter = EnglishGreeterWithRecordingImpl.newInstance(new EnglishGreeter());
```

### License

```
Copyright 2017 Fumihiko Shiroyama

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
