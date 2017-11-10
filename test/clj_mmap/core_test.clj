(ns clj-mmap.core-test
  (:require [clojure.test :refer :all]
            [clj-mmap.core :refer :all]
            [clojure.java.io :as io]
            [clj-mmap.core :as mmap]))

(deftest write-new-file-test
  (let [test-file (io/resource "empty.bin")]
    (with-open [empty (mmap/get-mmap test-file :read-write 3)]
      (mmap/put-bytes
        empty
        (byte-array [97 98 99])
        0))
    (is (= "abc" (slurp test-file)))
    (spit test-file "")))
