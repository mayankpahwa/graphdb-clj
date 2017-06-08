(ns graphdb-clj.core
  (:require [cypher.parser :as parser]
  	        [cypher.interpreter :as interpreter]
  	        [graphdb-clj.redis :as redis])
  (:gen-class))

(defn handler []
    (let [query-str (read-line)
    	    query-list (parser/query-parser query-str)
          query-dict (interpreter/query-interpreter query-list)
          query-key (first (keys query-dict))
          query-value (first (vals query-dict))]
      ((redis/MATCH-QUERY query-key) query-value)))

(defn -main
  []
  (while true
    (handler)))
