(ns minasss-games.pixi.input
  "for this namespace I would like to expose two kind of interfaces
  one as a wrapper to low level javascript event handling methods
  one as a high level interface built on top of the low level one
  which provides higher level abstractions like:
  `(register-keys {:arrow-up ::up :w ::up :arrow-down ::down :s ::down}
    (fn [event-type native-key-code translated-key-code]
      (cond
        (and (=translated-key-code :up) (= event-type :key-up)) (jump))))`")

(def event-registry_ (atom {}))

(def event-mappings
  {:key-up "keyup"
   :key-down "keydown"
   :key-press "keypress"})

(defn add-key-handler
  [event-name handler-identifier handler]
  (swap! event-registry_
    (fn [event-map]
      (.addEventListener js/document (get event-mappings event-name) handler)
      (assoc event-map [event-name handler-identifier] handler))))

(defn remove-key-handler
  [event-name handler-identifier]
  (swap! event-registry_
    (fn [event-map]
      (.removeEventListener js/document (get event-mappings event-name))
      (dissoc event-map [event-name handler-identifier]))))

(defn register-keys
  "Tries to provide a higher level abstraction for key management"
  [keys-map handler-identifier handler]
  (let [handler-fn (fn [event-type event]
                     (let [native (.-key event)
                           translated (get keys-map native)]
                       (when (some? translated)
                         (handler event-type native translated))))]
    (add-key-handler :key-down handler-identifier (partial handler-fn :key-down))
    (add-key-handler :key-press handler-identifier (partial handler-fn :key-press))
    (add-key-handler :key-up handler-identifier (partial handler-fn :key-up))))

(defn unregister-key-handler
  "Remove all the handlers added by register-keys for the
  specified handler identifier"
  [handler-identifier]
  (remove-key-handler :key-down handler-identifier)
  (remove-key-handler :key-press handler-identifier)
  (remove-key-handler :key-up handler-identifier))
