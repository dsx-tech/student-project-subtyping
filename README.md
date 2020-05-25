# Java Subtyping
A plugin for a javac which helps Java developers to avoid errors with usage of primitive types. The plugin introduces a pluggable type system based on Java annotation processing.    

The main features are listed below.
- Declare custom types that are used to mark up local variables, fields, method parameters, and return values of methods.
- Declare subtyping relation on custom types.
- Prohibit the usage of custom types in unary and binary operations.
- Check whether custom types are used correctly.    
## How to enable the plugin
To enable the plugin download the [jar]() and follow current steps to enable annotation processing for [IntelliJ IDEA](https://www.jetbrains.com/help/idea/annotation-processors-support.html) or [Gradle](https://docs.gradle.org/current/userguide/java_plugin.html) project.   
## How to use the plugin to prevent errors
Declaring custom types:
```java
@MetaType
class UserId {}

class AdultUserId extends UserId {}
```
All custom types should be marked by the `@MetaType` annotation. Notice that if a supertype is declared with a `@MetaType` annotation, the subtype also has the annotation implicitly.   

If you want to declare variable or parameter with a custom type, use an annotation `@Type` with corresponding `.class` argument as in example below.
```java
class IdSender {
    void sendPersonId(@Type(AdultUserId.class) String id) {
        // do something
    }

    void doSomething() {
        //...
        @Type(UserId.class)
        String personId = "personId";

        sendPersonId(personId); // error; incompatible types: 
                                // 'UserId' cannot be converted to 'AdultUserId'
        //...
    }
}
```
By default, the usage of annotated value is prohibited in binary and unary operation such as `+`, `-`, etc. To permit the type in operations use appropriate annotations such as `@Plus`, `@Minus`, etc. Notice that if a supertype is declared with the corresponding annotation, the subtype also has the annotation implicitly.    
|`@Annotation`|Corresponding operations|
|:------------|:-----------------------|
|`@Plus`|`+`|
|`@Minus`|`+`|
|`@Divide`|`+`|
|`@Multiply`|`+`|
|`@Remainder`|`%`|
|`@UnaryPlus`|`+`|
|`@UnaryMinus`|`-`|
|`@Increment`|`++`|
|`@Decrement`|`--`|
|`@BitwiseComplement`|`~`|
|`@LeftShift`|`<<`|
|`@RightShift`| `>>` |
|`@UnsignedRightShift`|`>>>`|
|`@Equal`|`!=`, `==`, `>`, `<`, `<=`, `>=`|
|`@And` | `&` |
|`@Or`|`|`|
|`@Xor`|`^`|
|`@ConditionalAnd`|`&&`|
|`@ConditionalOr`|`||`|
|`@LogicalComplement`|`!`|

## Type inference and rules for correct use of the types    
The plugin enforces subtyping rules and permission in operations. 
A particular type can be safely used wherever its supertype is expected.
The type can be safety used in particular operation if the operation is permitted for the type.
In order to reduce the number of `@Type` annotations in the source code, the plugin can infer the type of the local variable, using type information obtained from the variable's initializer. 
Moreover, you can use _raw type_ variables (without custom type such as common variable) and literals to initialize variables of custom type, as well as arguments methods with custom type parameters. Example below demonstrate this.
```java
class IdSender {
    void sendPersonId(@Type(AdultUserId.class) String id) {
        // do something
    }

    void doSomething() {
        //...
        @Type(UserId.class)
        String personId = "personId";

        sendPersonId("otherPersonId"); // correct
        //...
    }
}
```
In arithmetic operations, the type of one of the operands must be a subtype of the other. 
After the plugin has checked this, the operand type is moved to the corresponding supertype of the other operand. 
This behavior is the same as calculating the type of arithmetic expressions in Java.
```java
class BankAccount { 
    @Type(Dollar.class)
    private int val1 = 100;

    @Type(Currency.class)
    private int val2 = 100;

    void evaluateSum() {
        //... 
        int sum = val1 + val2; // variable sum has type Currency
        //...
    }
}
```
When one of the operands in an arithmetic expression has a _raw type_, the entire expression loses its type and gets the _unknown type_. 
This expression can be used as a common variable or argument, but it cannot be used as initializer for a custom type variable. 
The example below demonstrate it.
```java
class BankAccount { 
    @Type(Dollar.class)
    private int val1 = 100;
    
    private int val2 = 100;

    void evaluateSum() {
        //... 
        int sum = val1 + val2; // variable sum has unknown type

        @Type(Euro.class)
        int val3 = sum; // error: cannot use an unknown type expression to initialize a custom type variable
        //...
    }
}
```