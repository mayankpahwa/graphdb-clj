;; Parser for cypher query language
(ns cypher.parser)
(use '[clojure.string])

(defn any-one-parser-factory [& args]
	(fn [data] ((reduce #(if (%1 data) %1 %2) args) data)))

(defn keyword-parser [query]
  (let [keywords ["create " "match " "where " "return " "delete " "remove " "set "]]
    (if (not-empty (filter #(starts-with? query %) keywords))
    	(split query #"\s+" 2))))



