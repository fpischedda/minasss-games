(ns minasss-games.pixi.input
  "for this namespace I would like to expose two kind of interfaces
  one as a wrapper to low level javascript event handling methods
  one as a high level interface built on top of the low level one
  which provides higher level abstractions like:
  `(register-keys {:arrow-up ::up :w ::up :arrow-down ::down :s ::down}
    (fn [native-key-code translated-key-code event-type]
      (cond
        (and (=translated-key-code :up) (= event-type :key-up)) (jump))))`")

(def event-registry_ (atom {}))

(def event-mappings
  {:key-up "keyup"
   :key-down "keydown"
   :key-press "keypress"})

(defn add-key-handler
  [event-name event-identifier handler]
  (swap! event-registry_
    (fn [event-map]
      (.addEventListener js/document (get event-mappings event-name) handler)
      (assoc event-map [event-name event-identifier] handler))))

(defn remove-key-handler
  [event-name event-identifier]
  (swap! event-registry_
    (fn [event-map]
      (.removeEventListener js/document (get event-mappings event-name))
      (dissoc event-map [event-name event-identifier]))))
