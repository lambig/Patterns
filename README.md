[![Build Status](https://travis-ci.com/lambig/Patterns.svg?branch=main)](https://travis-ci.com/lambig/Patterns)
# Patterns
easy implementation of pattern-matcher works as a function
Note: this README is written through the help of ChatGPT.  

## Overview
Patterns is a functional utility library for handling conditional logic in a more declarative and readable way.  
It helps you to deal with the complex control flow by using pattern matching.

## Features

- Conditional mapping based on predicates
- Default value when no conditions are met (`orElse`)
- Custom exception throwing (`orElseThrow`)
- Handling/Denying null values
- Support for type-based pattern matching

## Installation(gradle)

```gradle
implementation group: 'io.github.lambig', name: 'Patterns', version: '1.3.0'
```


## Usage

### Examples

```java
Patterns<Integer, String> target = patterns(
    when(equalsTo(3), then("b")),
    when(equalsTo(4), thenSupply(() -> "c")),
    when(i -> i > 0, thenApply(Object::toString)),
    when(i -> i < 0, thenApply(sequenceOf((UnaryOperator<Integer>) i -> i + 1, Number::longValue, Object::toString))),
    orElse(then("a"))
);

List<String> actual = Stream.of(-1, 0, 1, 2, 3, 4)
    .map(target)
    .collect(toList()); // actual will contain ["0", "a", "1", "2", "b", "c"]
```

```java
//SetUp
AtomicReference<Integer> integer1 = new AtomicReference<>(); // will have 3
AtomicReference<Integer> integer2 = new AtomicReference<>(); // will have 4
AtomicReference<Integer> integer3 = new AtomicReference<>(); // will have 2

ConsumingPatterns<Integer> target =
        ConsumingPatterns.of(
                when(equalsTo(3), thenAcceptWith(integer1::set)),
                when(equalsTo(4), thenAcceptWith(integer2::set)));

Stream.of(-1, 0, 1, 2, 3, 4).forEach(target.orElseDo(integer3::set));
```
