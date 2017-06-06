;; Parser for cypher query language
(ns cypher.parser)
(use '[clojure.string])

;; Individual regular expression patterns
(def create-node-regex #"^\([a-zA-Z]\w*\s*:\s*[a-zA-Z]\w*\s*(\{([a-zA-Z]\w*\s*:\s*(('\w+')|([0-9]+)),)*(\w+\s*:\s*(('\w+')|([0-9]+)))\})?\)")

(def create-edge-regex #"^\([a-zA-Z]\w*\)-\[:[a-zA-Z]\w*\s*(\{.*\})?\]->\([a-zA-Z]\w*\)")

(def match-condition-regex #"^\([a-zA-Z]\w*(:[a-zA-Z]\w*)?\)")

(def property-fix-regex #"^[a-zA-Z]\w*\.[a-zA-Z]\w*\s*=\s*(('\w+')|([0-9]+))")

(def property-query-regex #"^[a-zA-Z]\w*\.[a-zA-Z]\w*")

(def type-regex #"^[a-zA-Z]\w*:[a-zA-Z]\w*")

(def id-regex #"^[a-zA-Z]\w*")

(def comma-regex #"^,")


(defn any-one-parser-factory [& args]
  (fn [data] ((reduce #(if (%1 data) %1 %2) args) data)))

(defn comma-parser [query]
  (let [comma-match (re-find comma-regex query)]
      (if comma-match
        [nil (trim (subs query 1))])))

(defn keyword-parser [query]
  (let [keywords ["create " "match " "where " "return " "delete " "remove " "set "]]
    (if (not-empty (filter #(starts-with? query %) keywords))
      (split query #"\s+" 2))))

(defn create-node-parser [query]
  (let [node-match (re-find create-node-regex query)]
      (if node-match 
        [(first node-match) (trim (subs query (count (first node-match))))])))

(defn create-edge-parser [query]
  (let [edge-match (re-find create-edge-regex query)]
      (if edge-match 
        [(first edge-match) (trim (subs query (count (first edge-match))))])))

(defn match-condition-parser [query]
  (let [match-condition (re-find match-condition-regex query)]
      (if match-condition 
        [(first match-condition) (trim (subs query (count (first match-condition))))])))

(defn property-fix-parser [query]
  (let [property-fix-match (re-find property-fix-regex query)]
      (if property-fix-match
        [(first property-fix-match) (trim (subs query (count (first property-fix-match))))])))

(defn property-query-parser [query]
  (let [property-query-match (re-find property-query-regex query)]
      (if property-query-match
        [property-query-match (trim (subs query (count property-query-match)))])))

(defn type-parser [query]
  (let [type-match (re-find type-regex query)]
      (if type-match
        [type-match (trim (subs query (count type-match)))])))

(defn id-parser [query]
  (let [id-match (re-find id-regex query)]
      (if id-match
        [id-match (trim (subs query (count id-match)))])))

(def value-parser (any-one-parser-factory keyword-parser create-node-parser create-edge-parser
                   match-condition-parser property-fix-parser property-query-parser type-parser id-parser
                   comma-parser))

(defn query-parser [input]
  (loop [query input parsed-vec []]
    (if (empty? query)
        parsed-vec
        (let [[consumed-str rest-str] (value-parser query)]
          (if consumed-str
              (recur rest-str (conj parsed-vec consumed-str))
              (recur rest-str parsed-vec))))))
