# cartus

[![Clojars Project](https://img.shields.io/clojars/v/io.logicblocks/cartus.svg)](https://clojars.org/io.logicblocks/cartus)
[![Clojars Downloads](https://img.shields.io/clojars/dt/io.logicblocks/cartus.svg)](https://clojars.org/io.logicblocks/cartus)
[![GitHub Contributors](https://img.shields.io/github/contributors-anon/logicblocks/cartus.svg)](https://github.com/logicblocks/cartus/graphs/contributors)

A structured logging abstraction for logging data rich events with support for 
multiple backends, currently including:
* a test logger for collecting logs in memory and asserting against 
  them; and
* a [`cambium`](https://cambium-clojure.github.io/) logger for logging 
  out via [`SLF4J`](http://www.slf4j.org/) and 
  [`logback`](http://logback.qos.ch/).
  
Heavily inspired by [JUXT's blog post on logging](https://juxt.pro/blog/logging).

## Installation

Add the following to your `project.clj` file:

```clojure
[io.logicblocks/cartus.1 "0.1.9"]
```

Depending on which backends you plan to use, add one or more of the following to
your `project.clj` file:

```clojure
[io.logicblocks/cartus.1 "0.1.9"]
[io.logicblocks/cartus.1 "0.1.9"]
```

The [`cambium`](https://cambium-clojure.github.io/) backend requires further
configuration. See the 
[Getting Started](https://logicblocks.github.io/cartus/getting-started.html)
guide for more details.

## Documentation

* [API Docs](http://logicblocks.github.io/cartus)
* [Getting Started](https://logicblocks.github.io/cartus/getting-started.html)

## Usage

```clojure
(require '[cartus.core :as log])
(require '[cartus.cambium])

(cartus.cambium/initialise)

(def logger (cartus.cambium/logger))

(log/info logger ::usage.demonstrated
  {:feature-coverage "3%" :customer-satisfaction :low})
(let [logger (log/with-context logger
               {:see "getting started guide" :for "more information"})]
  (log/debug logger ::usage.redirecting {:to "API docs"})
  (log/error logger ::usage.irritating
    {:joke "running thin"}
    {:exception (ex-info "Not funny!" {:stick-to "day job"})}))
```

## License

Copyright &copy; 2020 LogicBlocks Maintainers

Distributed under the terms of the 
[MIT License](http://opensource.org/licenses/MIT).
