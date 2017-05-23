;;Interpreter for the cypher query language
(ns cypher.interpreter)
(use '[clojure.string])

n.property1 = value1
n:Person
n
n.property,n:type

(def property-fix-regex #"^\w+\.(\w+)\s*=\s*(.+)$"

(def property-query-regex #"^\w+\.(\w+)$")

(def type-regex #"^\w+\s*:\s*(\w+)$")

(def id-regex #"^\w+$")

(def change-type [value-str]
  (if (starts-with? value-str #"'") 
	  (replace value-str #"'" "") 
	  (Integer. value-str)))

(def keyword-ify [[key-str value-str]]
	[(keyword key-str) (change-type value-str)])

(defn json-to-map-fn [json-str]
	(let [json-list (map trim (split json-str #","))
		  key-val-pair-list (map #(map trim (split % #":")) json-list)]
		 (map keyword-ify key-val-pair-list)))

(defn node-parse-build-dict [q-string]
	(let [[full-match id-val type-val json-str] (re-find #"\((\w+):(\w+)\s*\{(.+)\}\)" q-string)]
		(into (hash-map :id id-val :type type-val) (json-to-map-fn json-str))))

(defn node-builder [q-string]
	(hashmap :node (node-parse-build-dict q-string)))

(defn edge-parse-build-list [q-string]
  (let [[full-match head type tail] (re-find #"\((\w+)\)-\[:(\w+)\]->\((\w+)\)" q-string)]
  	[head tail type]))

(defn edge-builder [q-string]
	(hashmap :edge (edge-parse-build-list q-string)))

(defn node-edge-builder [q-string]
	(if (.contains q-string "{")
		(node-builder q-string)
		(edge-builder q-string)))

(defn create-dict-builder [q-list]
	(hashmap :create (vec (map node-edge-builder q-list))))

(defn type-identify [data]
	(let [type-match (re-find #"\(\w+\s*:\s*(\w+)\)" data)]
	   (if type-match
	       {:type (last type-match)}
	       {})))

(defn property-identify [q-str]
	(let [[property-match property-key property-value] (re-find #"\w+\.(\w+)\s*\=\s*(.+)" q-str)]
		(hash-map (keyword property-key) (change-type property-value))))

(defn where-dict-builder [match-dict q-list]
	(keyword-dict-builder (assoc match-dict :where (property-identify (second q-list))) (rest (rest q-list))))

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
