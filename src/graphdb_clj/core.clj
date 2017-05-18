(ns graphdb-clj.core
  (:use [graphdb-clj.main])
  (:gen-class))

(defn -main
  [query]
  (handler query))