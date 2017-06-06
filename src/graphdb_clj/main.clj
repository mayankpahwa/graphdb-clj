(ns graphdb-clj.main)

(def graph-state (atom (read-string (slurp "src/database/graph1.txt"))))

;Save to file Function-------------

(defn save-to-file []
  (spit "src/database/graph1.txt" @graph-state))

;Create Functions------------------

(defn create-node [node-dict]
	(swap! graph-state assoc-in [(node-dict :id)] (assoc (dissoc node-dict :id) :in-edge [] :out-edge [])))

(defn create-edge [[head tail type property-dict]]
  (if property-dict
      (do (swap! graph-state update-in [head :out-edge] conj [tail type (read-string property-dict)])
          (swap! graph-state update-in [tail :in-edge] conj [head type (read-string property-dict)]))
      (do (swap! graph-state update-in [head :out-edge] conj [tail type])
	        (swap! graph-state update-in [tail :in-edge] conj [head type]))))


(defn helper-create [data-dict]
	(if (contains? data-dict :node)
		(create-node (data-dict :node))
		(create-edge (data-dict :edge))))

(defn create-fn [data]
    (doall (map helper-create data)))

;;Match-Where Filter-----------

(defn match-where-filter [data]
    (let [all-nodes (keys @graph-state)
          match-dict (data :match)
          match-filter-nodes (if (empty? match-dict)
                                  all-nodes
                                  (filter #(= (match-dict :type) ((@graph-state %) :type)) all-nodes))
          [where-key where-val] (flatten (vec (data :where)))
          where-filter-nodes (if where-key
          	                     (filter #(= where-val ((@graph-state %) where-key)) match-filter-nodes)
          	                     match-filter-nodes)]
        where-filter-nodes))

;;Set Functions-------------------

(defn helper-set [property node-id]
    (let [[property-key property-value] (flatten (vec property))]
        (swap! graph-state assoc-in [node-id property-key] property-value)))

(defn set-fn [data]
    (let [filtered-nodes (match-where-filter data)]
        (doall (map #(helper-set (data :property) %) filtered-nodes))))

;;Return Functions-------------------

(defn helper-return [property node-id]
    (prn (if property
        (get-in @graph-state [node-id property])
        (get @graph-state node-id))))

(defn return-fn [data]
    (let [filtered-nodes (match-where-filter data)]
        (doall (map #(helper-return (data :property) %) filtered-nodes))))

;;Remove Functions-------------------

(defn helper-remove [property node-id]
     (swap! graph-state assoc-in [node-id] (dissoc (@graph-state node-id) property)))

(defn remove-fn [data]
    (let [filtered-nodes (match-where-filter data)]
        (doall (map #(helper-remove (data :property) %) filtered-nodes))))

;;Delete Functions-------------------

(defn delete-in-edge [in-node node-id]
    (let [updated-edge-list (vec (filter #(not= node-id (first %)) ((@graph-state in-node) :in-edge)))]
        (swap! graph-state assoc-in [in-node :in-edge] updated-edge-list)))

(defn delete-out-edge [out-node node-id]
    (let [updated-edge-list (vec (filter #(not= node-id (first %)) ((@graph-state out-node) :out-edge)))]
        (swap! graph-state assoc-in [out-node :out-edge] updated-edge-list)))

(defn helper-delete [node-id]
    (let [in-nodes (map first ((@graph-state node-id) :out-edge))
          out-nodes (map first ((@graph-state node-id) :in-edge))]
     (doall (map #(delete-in-edge % node-id) in-nodes))
     (doall (map #(delete-out-edge % node-id) out-nodes))
     (swap! graph-state dissoc node-id)))

(defn delete-fn [data]
    (let [filtered-nodes (match-where-filter data)]
      (doall (map helper-delete filtered-nodes))))


(def MATCH-QUERY {:create create-fn :return return-fn :set set-fn :remove remove-fn :delete delete-fn})
