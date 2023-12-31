# rdroidtest

This is a custom Rust test harness which allows tests to be ignored at runtime based on arbitrary
criteria. The built-in Rust test harness only allows tests to be ignored at compile time, but this
is often not enough on Android, where we want to ignore tests based on system properties or other
characteristics of the device on which the test is being run, which are not known at build time.

## Usage

Unfortunately without the built-in support that rustc provides to the standard test harness, this
one is slightly more cumbersome to use. Firstly, add it to the `rust_test` build rule in your
`Android.bp` by adding the defaults provided:

```soong
rust_test {
    name: "mycrate.test",
    defaults: ["rdroidtest.defaults"],
    // ...
}
```

If you are testing a binary that has a `main` function, you'll need to remove it from the test
build:

```rust
#[cfg(not(test))]
fn main() {
    // ...
}
```

(If you're testing a library or anything else which doesn't have a `main` function, you don't need
to worry about this.)

Each test case should be marked with the `rdroidtest::test!` macro, rather than the standard
`#[test]` attribute:

```rust
use rdroidtest::test;

test!(one_plus_one);
fn one_plus_one {
    assert_eq!(1 + 1, 2);
}
```

To ignore a test, you can add an `ignore_if` clause with a boolean expression:

```rust
use rdroidtest::test;

test!(clap_hands, ignore_if: !feeling_happy());
fn clap_hands {
    assert!(HANDS.clap().is_ok());
}
```

Somewhere in your main module, you need to use the `test_main` macro to generate an entry point for
the test harness:

```rust
#[cfg(test)]
rdroidtest::test_main!();
```

You can then run your tests as usual with `atest`.
