(ns minasss-games.sound
  "Small wrapper around a JS sound library (Howl in this case)"
 (:require [minasss-games.pixi :refer [Resources]]
           [cljsjs.howler]
            [oops.core :refer [oget oget+ oset!]]) )

(defn make-sound
  "Return a sound from the resource cache, looking up by name"
  [resource-name]
  (js/console.log resource-name)
  (let [res (aget Resources resource-name)]
    (js/console.log res)
    (if (nil? res)
      (println "could not find sound " resource-name)
      (js/Howl. (clj->js {:src [(oget res "url")]})))))

(comment
  (def shot (make-sound "sfx/shmup/game/shot.ogg"))
  (js/console.log shot)
  (.play shot)
  )
