(ns graphdb-clj.main)

(def graph-state (atom (eval (read-string "src/database/graph1.txt"))))

(def QUERY-STATE {:create create-fn})


;Create Functions------------------

(defn create-node [node-dict]
	(swap! graph-state assoc-in (node-dict :id) (assoc (dissoc node-dict :id) :in-edge [] :out-edge [])))

(defn create-edge [[head tail type]]
	(swap! graph-state update-in [head :out-edge] conj [tail type])
	(swap! graph-state update-in [tail :in-edge] conj [head type]))


(defn helper-create [data-dict]
	(if (contains? data-dict :node)
		(create-node (data-dict :node))
		(create-edge (data-dict :edge))))

(defn create-fn [data]
	(map helper-create data))

;----------------------------------

