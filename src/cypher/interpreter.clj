;;Interpreter for the cypher query language


(defn query-interpreter [query-list]
	(case (first query-list)
	  "create" (create-dict-builder (rest query-list))
	  "match" (match-dict-builder (rest query-list))
     (prn "Invalid Syntax")))
