(ns minasss-games.experiments.shmup.entities)

(defn create-registry
  []
  {:entities {}
   :current-id 0
   })

(defn create-entity
  ([registry]
   (create-entity registry {}))
  ([{:keys [current-id entities] :as registry} state]
   (let [id (inc current-id)]
     (assoc registry
       :current-id id
       :entities (assoc entities id state)))))
