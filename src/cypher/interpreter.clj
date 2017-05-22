;;Interpreter for the cypher query language

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

(defn query-interpreter [query-list]
	(case (first query-list)
	  "create" (create-dict-builder (rest query-list))
	  "match" (match-dict-builder (rest query-list))
     (prn "Invalid Syntax")))
