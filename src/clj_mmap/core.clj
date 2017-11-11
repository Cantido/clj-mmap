(ns clj-mmap.core
  "Easily memory-map files via Java's NIO, and handle files
   larger than 2GB."
  (:require [clojure.java.io :as io])
  (:import (java.io RandomAccessFile Closeable File FileInputStream)
           (java.nio.channels FileChannel FileChannel$MapMode)
           (clojure.lang Indexed Seqable)
           (java.nio MappedByteBuffer ByteBuffer)))

(set! *warn-on-reflection* true)

(def ^:private bytes-per-map
  "The number of bytes a single MappedByteBuffer will store"
  Integer/MAX_VALUE)

(definterface ISize
  (^long size []))

(deftype Mmap [^RandomAccessFile fis ^FileChannel fc maps]
  ISize
  (size [this] (.size fc))

  Indexed
  (nth [this i] (get maps i))
  (nth [this i not-found] (get maps i not-found))

  Seqable
  (seq [this] (seq maps))

  Closeable
  (close
    [this]
    (do
      (.close fc)
      (.close fis))))

(def ^:private map-modes
  {:private    FileChannel$MapMode/PRIVATE
   :read-only  FileChannel$MapMode/READ_ONLY
   :read-write FileChannel$MapMode/READ_WRITE})

(def ^:private map-perms
  {:private    "r"
   :read-only  "r"
   :read-write "rw"})

(defn- raf
  ^RandomAccessFile [^File file map-mode]
  (RandomAccessFile.
    ^File (io/as-file file)
    (str (map-perms map-mode))))

(defn- build-mmap
  [^FileInputStream fis ^FileChannel fc map-mode size]
  (let [mmap (fn [pos n] (.map fc (map-modes map-mode) pos n))]
    (Mmap. fis fc (mapv #(mmap % (min (- size %)
                                      bytes-per-map))
                        (range 0 size bytes-per-map)))))

(defn get-mmap
  "Provided a file, mmap the entire file, and return an opaque type
   to allow further access. Remember to use with-open, or to call
   .close, to clean up memory and open file descriptors. The file
   argument can be any implementation of clojure.java.io/Coercions."
  ([file] (get-mmap file :read-only))
  ([file map-mode]
   (let [fis  (raf file map-mode)
         fc   (.getChannel fis)
         size (.size fc)]
     (build-mmap fis fc map-mode size)))
  ([file map-mode size]
   (let [fis  (raf file map-mode)
         fc   (.getChannel fis)]
     (build-mmap fis fc map-mode size))))

(defn- buf-put [^ByteBuffer buf src offset length]
  (.put buf src offset length))

(defn- buf-get [^ByteBuffer buf src offset length]
  (.get buf src offset length))

(defn- chunk-op [mmap fn buf pos n]
  (let [get-chunk   #(nth mmap (int (/ % bytes-per-map)))
        end         (+ pos n)
        chunk-term  (-> pos
                        (/ bytes-per-map)
                        int
                        inc
                        (* bytes-per-map))
        op-size     (- (min end chunk-term) ;; bytes to op in first chunk
                       pos)
        start-chunk ^ByteBuffer (get-chunk pos)
        end-chunk   ^ByteBuffer (get-chunk end)]

    (locking start-chunk
      (.position start-chunk (mod pos bytes-per-map))
      (fn start-chunk buf 0 op-size))

    ;; Handle ops that span MappedByteBuffers
    (if (not= start-chunk end-chunk)
      (locking end-chunk
        (.position end-chunk 0)
        (fn end-chunk buf op-size (- n op-size))))

    buf))

(defn get-bytes ^bytes [mmap pos n]
  "Retrieve n bytes from mmap, at byte position pos."
  (chunk-op mmap buf-get (byte-array n) pos n))

(defn put-bytes
  "Write n bytes from buf into mmap, at byte position pos.
   If n isn't provided, the size of the buffer provided is used."
  ([mmap ^bytes buf pos] (put-bytes mmap buf pos (alength buf)))
  ([mmap ^bytes buf pos n]
   (chunk-op mmap buf-put buf pos n)))

(defn loaded? [mmap]
  "Returns true if it is likely that the buffer's contents reside in physical memory."
  (every? (fn [^MappedByteBuffer buf]
            (.isLoaded buf))
          mmap))
