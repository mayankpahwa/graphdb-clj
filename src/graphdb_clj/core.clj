(ns graphdb-clj.core
  (:require [cypher.parser :as parser]
  	        [cypher.interpreter :as interpreter]
  	        [graphdb-clj.main :as main])
  (:gen-class))

(defn handler []
    (let [query-str (read-line)
    	    query-list (parser/query-parser query-str)
          query-dict (interpreter/query-interpreter query-list)
          query-key (first (keys query-dict))
          query-value (first (vals query-dict))]
      ((main/MATCH-QUERY query-key) query-value))
  (main/save-to-file)
  (main/save-to-redis))

(defn -main
  []
  (while true
    (handler)))
