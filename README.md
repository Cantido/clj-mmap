# clj-mmap

A Clojure library designed to allow you to easily mmap files via Java's NIO, and to handle files larger than 2GB.

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.cantido/clj-mmap.svg)](https://clojars.org/org.clojars.cantido/clj-mmap)
[![Build Status](https://travis-ci.org/Cantido/clj-mmap.svg?branch=master)](https://travis-ci.org/Cantido/clj-mmap)

## Usage
```clojure
(with-open [mapped-file (clj-mmap/get-mmap "/tmp/big_file.txt")]
  (let [some-bytes (clj-mmap/get-bytes mapped-file 0 30)]
    (println (str "First 30 bytes of file, '" (String. some-bytes "UTF-8") "'"))))
```

## License

Licensed under the [MIT license](https://opensource.org/licenses/MIT).

Copyright (C) 2012-2013 Alan Busby
