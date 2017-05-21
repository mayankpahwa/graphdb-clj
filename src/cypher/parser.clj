;; Parser for cypher query language
(ns cypher.parser)
(use '[clojure.string])

;; Individual regular expression patterns
(def create-node-regex #"^\([a-zA-Z]\w*\s*:\s*[a-zA-Z]\w*\s*(\{([a-zA-Z]\w*:(('\w+')|([0-9]+)),)*(\w+:(('\w+')|([0-9]+)))\})?\)")

(def create-edge-regex #"^\([a-zA-Z]\w*\)-\[:[a-zA-Z]\w*\]->\([a-zA-Z]\w*\)")

(def match-condition-regex #"^\([a-zA-Z]\w*(:[a-zA-Z]\w*)?\)")

(def property-fix-regex #"^[a-zA-Z]\w*\.[a-zA-Z]\w*\s*=\s*(('\w+')|([0-9]+))")

(def property-query-regex #"^[a-zA-Z]\w*\.[a-zA-Z]\w*")

(def type-regex #"^[a-zA-Z]\w*:[a-zA-Z]\w*")

(def id-regex #"^[a-zA-Z]\w*")


(defn any-one-parser-factory [& args]
	(fn [data] ((reduce #(if (%1 data) %1 %2) args) data)))

(defn keyword-parser [query]
  (let [keywords ["create " "match " "where " "return " "delete " "remove " "set "]]
    (if (not-empty (filter #(starts-with? query %) keywords))
    	(split query #"\s+" 2))))