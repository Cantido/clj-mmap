(ns clj-mmap.core-test
  (:require [clojure.test :refer :all]
            [clj-mmap.core :refer :all]
            [clojure.java.io :as io]
            [clj-mmap.core :as mmap])
  (:import (java.io File)
           (java.nio ByteBuffer)))

(def lowalpha (range 97 (+ 26 97)))

(defn reset-test-files []
  (spit
    (io/resource "alpha.bin")
    (apply str (map char lowalpha)))
  (spit
    (io/resource "empty.bin")
    ""))

(use-fixtures :each (fn [f] (reset-test-files) (f)))
(use-fixtures :once (fn [f] (f) (reset-test-files)))

(deftest read-bytes-test
  (testing "reading bytes from a file"
    (testing "when the read is entirely within existing bytes"
      (with-open [alpha (mmap/get-mmap (io/resource "alpha.bin"))]
        (is (= (map int [\l \m \n \o \p])
               (seq (mmap/get-bytes alpha 11 5))))))
    (testing "when all bytes in the file are being read"
      (with-open [alpha (mmap/get-mmap (io/resource "alpha.bin"))]
        (is (= lowalpha
               (seq (mmap/get-bytes alpha 0 26))))))))

(deftest put-bytes-test
  (testing "putting bytes into a file"
    (testing "that is smaller than the write size"
      (let [test-file (io/resource "empty.bin")]
        (with-open [empty (mmap/get-mmap test-file :read-write 3)]
          (mmap/put-bytes
            empty
            (byte-array [97 98 99])
            0))
        (is (= "abc" (slurp test-file)))))
    (testing "that is bigger than the write size"
      (let [test-file (io/resource "alpha.bin")]
        (with-open [empty (mmap/get-mmap test-file :read-write)]
          (mmap/put-bytes
            empty
            (byte-array [65 66 67])
            0))
        (is (= "ABC" (subs (slurp test-file) 0 3)))))))

(defn aseq [^ByteBuffer x]
  (seq (.array x)))

(defn bbuf
  ([xs] (ByteBuffer/wrap (byte-array xs))))

(deftest put-bytes-buffer-test
  (let [buf (bbuf 3)]
    (mmap/put-bytes [buf] (byte-array [1 2 3]) 0)
    (is (= [1 2 3]
           (aseq buf)))))

(deftest get-bytes-buffer-test
  (let [buf (bbuf [1 2 3])]
    (mmap/get-bytes [buf] 0 3)
    (is (= [1 2 3] (seq (mmap/get-bytes [buf] 0 3))))))
