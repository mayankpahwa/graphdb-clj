(ns graphdb-clj.redis
    (:require [taoensso.carmine :as car :refer (wcar)])
    (:require [clojure.data.json :as json]))

(def server-connection {:pool {}
                        :spec {:host     "localhost"
                               :port     6379
                               :timeout  4000}})

;;Get/Set Helper Redis Functions---------------

(defn set-data [k v]
  (wcar server-connection (car/set k (json/write-str v))))

(defn get-data [k]
   (json/read-str (wcar server-connection (car/get k)) :key-fn keyword))

;Create Functions------------------

(defn create-node [node-dict]
  (set-data (node-dict :id) (assoc (dissoc node-dict :id) :in-edge [] :out-edge [])))

(defn create-edge [[head tail type property-dict]]
 (let [h-dict (get-data head)
       t-dict (get-data tail)]
  (if property-dict
      (do (set-data head (assoc h-dict :out-edge (conj (h-dict :out-edge) [tail type (read-string property-dict)])))
          (set-data tail (assoc t-dict :in-edge (conj (t-dict :in-edge) [head type (read-string property-dict)]))))
      (do (set-data head (assoc h-dict :out-edge (conj (h-dict :out-edge) [tail type])))
          (set-data tail (assoc t-dict :in-edge (conj (t-dict :in-edge) [head type])))))))


(defn helper-create [data-dict]
	(if (contains? data-dict :node)
		(create-node (data-dict :node))
		(create-edge (data-dict :edge))))

(defn create-fn [data]
    (doall (map helper-create data)))

;;Match-Where Filter-----------

(defn match-where-filter [data]
    (let [all-nodes (wcar server-connection (car/keys "*"))
          match-dict (data :match)
          match-filter-nodes (if (contains? match-dict :type)                                
                                  (filter #(= (match-dict :type) ((get-data %) :type)) all-nodes)
                                  all-nodes)
          [where-key where-val] (flatten (vec (data :where)))
          where-filter-nodes (if where-key
          	                     (filter #(= where-val ((get-data %) where-key)) match-filter-nodes)
          	                     match-filter-nodes)]
        where-filter-nodes))

;;Set Functions-------------------

(defn helper-set [property node-id]
    (let [[property-key property-value] (flatten (vec property))]
        (set-data node-id (assoc (get-data node-id) property-key property-value))))

(defn set-fn [data]
    (let [filtered-nodes (match-where-filter data)]
        (doall (map #(helper-set (data :property) %) filtered-nodes))))

;;Return Functions-------------------

(defn out-edges [s-node relationship]
  (map first (filter #(= relationship (keyword (second %))) ((get-data s-node) :out-edge))))

(defn in-edges [s-node relationship]
  (map first (filter #(= relationship (keyword (second %))) ((get-data s-node) :in-edge))))

(defn relation-filter [some-nodes rel-type relationship]
  (cond
    (= rel-type "hlt") (vec (set (apply concat (map #(out-edges % relationship) some-nodes))))
    (= rel-type "tlh") (vec (set (apply concat (map #(in-edges % relationship) some-nodes))))))

(defn helper-return [property node-id]
    (prn (if property
            ((get-data node-id) property)
            (get-data node-id))))

(defn return-fn [data]
    (let [filtered-nodes (match-where-filter data)]
       (if (contains? data :rel-type)
           (doall (map #(helper-return (data :property) %) 
                        (relation-filter filtered-nodes (data :rel-type) ((data :match) :relationship))))
           (doall (map #(helper-return (data :property) %) filtered-nodes)))))

;;Remove Functions-------------------

(defn helper-remove [property node-id]
     (set-data node-id (dissoc (get-data node-id) property)))

(defn remove-fn [data]
    (let [filtered-nodes (match-where-filter data)]
        (doall (map #(helper-remove (data :property) %) filtered-nodes))))

;;Delete Functions-------------------

(defn delete-in-edge [in-node node-id]
    (let [updated-edge-list (vec (filter #(not= node-id (first %)) ((get-data in-node) :in-edge)))]
        (set-data in-node (assoc (get-data in-node) :in-edge updated-edge-list))))

(defn delete-out-edge [out-node node-id]
    (let [updated-edge-list (vec (filter #(not= node-id (first %)) ((get-data out-node) :out-edge)))]
        (set-data out-node (assoc (get-data out-node) :out-edge updated-edge-list))))

(defn helper-delete [node-id]
    (let [in-nodes (map first ((get-data node-id) :out-edge))
          out-nodes (map first ((get-data node-id) :in-edge))]
     (doall (map #(delete-in-edge % node-id) in-nodes))
     (doall (map #(delete-out-edge % node-id) out-nodes))
     (wcar server-connection (car/del node-id))))

(defn delete-fn [data]
    (let [filtered-nodes (match-where-filter data)]
      (doall (map helper-delete filtered-nodes))))
;;---------

(def MATCH-QUERY {:create create-fn :return return-fn :set set-fn :remove remove-fn :delete delete-fn})
