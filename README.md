# cartus

[![Clojars Project](https://img.shields.io/clojars/v/io.logicblocks/cartus.svg)](https://clojars.org/io.logicblocks/cartus)
[![Clojars Downloads](https://img.shields.io/clojars/dt/io.logicblocks/cartus.svg)](https://clojars.org/io.logicblocks/cartus)
[![GitHub Contributors](https://img.shields.io/github/contributors-anon/logicblocks/cartus.svg)](https://github.com/logicblocks/cartus/graphs/contributors)

A structured logging abstraction for logging data rich events with support for 
multiple backends, currently including:
* A test implementation for collecting logs in memory and asserting against 
  them.
* A cambium implementation for logging out via SLF4J and logback.

Heavily inspired by [JUXT's blog post on logging](https://juxt.pro/blog/logging).

## Installation

Add the following to your `project.clj` file:

```clj
[io.logicblocks/cartus "0.1.3"]
```

## Documentation

* [API Docs](http://logicblocks.github.io/cartus)

## Usage

TODO

## License

Copyright &copy; 2020 LogicBlocks Maintainers

Distributed under the terms of the 
[MIT License](http://opensource.org/licenses/MIT).
