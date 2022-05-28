## recipe

Extends [java.util.function.Supplier](https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html)
with higher-order methods that enable composition of suppliers and transformation and filtering of the results
produced by the `Supplier#get` functional method.

## Installation

Requires **Java 8** or higher.

Currently, the main option for installation is [Jitpack](https://jitpack.io/):

```groovy
dependencies {
    implementation 'com.github.nikolavojicic:recipe:<COMMIT-HASH>'
}
```

**TODO**: Publish to the *Maven Central Repository*.

## Examples