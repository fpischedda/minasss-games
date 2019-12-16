(ns minasss-games.experiments.shmup.enemy-ai
  "This namespace collects all enemy ai related functions and
  state management. Still not sure how this is going to be
  implemented, I expect it to be higlhy experimental and prone
  to changes in structure and functionality.")

(defmulti update-ai
  (fn [entity] (get-in entity [::ai ::behaviour-type])))

(defmethod update-ai :default
  [entity]
  entity)

(defmethod update-ai ::horizontal-ping-pong
  [{:keys [position direction] :as entity}]
  (let [dir-x (first direction)
        pos-x (first position)]
    (js/console.log dir-x)
    (js/console.log pos-x)
    (cond
      (> dir-x 0) (if (< 350 pos-x)
                    (assoc entity :direction [-1 0])
                    entity)
      (< dir-x 0) (if (> 200 pos-x)
                    (assoc entity :direction [1 0])
                    entity)
      :else entity)))

;; this multi method is used to create the initial state
;; for the specified behaviour type and "attach" it to the ::ai key
;; of the entity; this is needed because the main idea, for now,
;; is to change entity properties to reflect selected behaviour.
(defmulti init-state
  (fn [behaviour-type _entity] behaviour-type))

(defmethod init-state ::horizontal-ping-pong
  [behaviour-type entity]
  (-> entity
    (assoc
      ::ai {::behaviour-type behaviour-type}
      :direction [1 0]
      :speed 10)))
