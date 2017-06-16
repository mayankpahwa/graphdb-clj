;;Interpreter for the cypher query language
(ns cypher.interpreter)
(use '[clojure.string])

(def property-fix-regex #"^\w+\.(\w+)\s*=\s*(.+)$")

(def property-query-regex #"^(\w+)\.(\w+)$")

(def type-regex #"^\w+\s*:\s*(\w+)$")

(def id-regex #"^\w+$")

(defn change-type [value-str]
  (if (starts-with? value-str "'") 
	  (replace value-str #"'" "") 
	  (Integer. value-str)))

(defn keyword-ify [[key-str value-str]]
	[(keyword key-str) (change-type value-str)])

(defn json-to-map-fn [json-str]
	(let [json-list (map trim (split json-str #","))
		  key-val-pair-list (map #(map trim (split % #":")) json-list)]
        (map keyword-ify key-val-pair-list)))

(defn node-parse-build-dict [q-string]
	(let [[full-match id-val type-val json-str] (re-find #"\((\w+):(\w+)\s*\{(.+)\}\)" q-string)]
	   (into (hash-map :id id-val :type type-val) (json-to-map-fn json-str))))

(defn node-builder [q-string]
	(hash-map :node (node-parse-build-dict q-string)))

(defn edge-parse-build-list [q-string]
  (let [[full-match head type property-dict tail] 
  	        (re-find #"\((\w+)\)-\[:(\w+)\s*(\{.+\})*\]->\((\w+)\)" q-string)]
  	 [head tail type property-dict]))

(defn edge-builder [q-string]
	(hash-map :edge (edge-parse-build-list q-string)))

(defn node-edge-builder [q-string]
	(if (.contains q-string "->")
		(edge-builder q-string)
		(node-builder q-string)))

(defn create-dict-builder [q-list]
	(hash-map :create (vec (map node-edge-builder q-list))))

(defn type-identify [data]
	(let [type-match (re-find #"\(\w+\s*:\s*(\w+)\)" data)
		  rel-match (re-find #"^\(\w*\)-\[:(\w*)\s*(\{.*?\})?\]->\(\w*\)" data)]
	   (if type-match
	       {:type (last type-match)}
	       (if rel-match
	       	   {:relationship (keyword (second rel-match))}
	       	   {})
	       )))

(defn property-identify [q-str]
	(let [[property-match property-key property-value] (re-find #"\w+\.(\w+)\s*\=\s*(.+)" q-str)]
	   (hash-map (keyword property-key) (change-type property-value))))

(defn value-builder [query-value]
	(let [property-fix-match (re-find property-fix-regex query-value)
		  property-query-match (re-find property-query-regex query-value)
		  type-match (re-find type-regex query-value)
		  id-match (re-find id-regex query-value)]
	(cond 
	  property-fix-match (hash-map :property (conj {} (keyword-ify (subvec property-fix-match 1 3))))
	  property-query-match (if (contains? #{"a" "b"} (second property-query-match))
		 	                   (hash-map :property (keyword (last property-query-match))
		 	                      	     :rel-type ({"a" "tlh" "b" "hlt"} (second property-query-match)))
		 	                   (hash-map :property (keyword (last property-query-match))))
	  type-match (let [type-label (second type-match)] 
		 	           (if (= type-label "type")
		    	           (hash-map :property :type)
		    	           (hash-map :property (hash-map :type type-label))))
	  id-match {})))

(defn keyword-dict-builder [match-where-dict q-list]
	(let [[query-type query-value] q-list]
		(hash-map (keyword query-type) (conj match-where-dict (value-builder query-value)))))

(defn where-dict-builder [match-dict q-list]
	(keyword-dict-builder (assoc match-dict :where (property-identify (first q-list))) (rest q-list)))

(defn match-dict-builder [q-list]
	(let [match-dict {:match (type-identify (first q-list))}
		  rest-list (rest q-list)]
		(if (= "where" (second q-list))
			(where-dict-builder match-dict (rest rest-list))
			(keyword-dict-builder (assoc match-dict :where {}) rest-list))))

(defn query-interpreter [query-list]
	(case (first query-list)
	  "create" (create-dict-builder (rest query-list))
	  "match" (match-dict-builder (rest query-list))
     (prn "Invalid Syntax")))

(defn driver []
    (let [query-string (read-line)]
      (prn (query-interpreter (read-string query-string)))))
