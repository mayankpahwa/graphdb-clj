(ns graphdb-clj.main)

(def graph-state (atom (eval (read-string "src/database/graph1.txt"))))

(def QUERY-STATE {:create create-fn :return return-fn})


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


;;Return Functions-------------------

(defn return-fn [data]
	(case data
	    '*' @graph-state
	    (@graph-state data)))

;;Delete Functions-------------------

(defn helper-delete [node-id]
	(let [in-nodes (map first ((@graph-state node-id) :out-edge))
		  out-nodes (map first ((@graph-state node-id) :in-edge))]
 (map 
 	 (fn [n] (swap! graph-state update-in [n :in-edge] (filter 
                                                       #(not (contains? % node-id)) 
                                                       ((@graph-state n) :in-edge)))) 
 	  in-nodes)
 (map 
 	 (fn [n] (swap! graph-state update-in [n :out-edge] (filter 
 		                                              #(not (contains? % node-id)) 
 	                                                  ((@graph-state n) :out-edge)))) 
 	  out-nodes))
	(swap! graph-state dissoc node-id))

(defn delete-fn [data]
	(let [all-nodes (keys @graph-state)
		  match-dict (data :match)
		  match-filter-nodes (if (empty? match-dict) 
                                  all-nodes
                                  (filter #(= (match-dict :type) (% :type)) all-nodes))
		  [where-key where-val] (flatten (vec (data :where)))
		  where-filter-nodes (filter #(= where-val (% where-key)) match-filter-nodes)
		  ]
		(map helper-delete where-filter-nodes)))