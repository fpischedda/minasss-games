(ns minasss-games.sound
  "Small wrapper around a JS sound library (Howl in this case)"
 (:require [cljsjs.howler]))

(defn load
  "Return a sound from the resource cache, looking up by name"
  [resource-name]
  (js/Howl. (clj->js {:src [resource-name]
                      :preload true})))

(comment
  (def shot (load "sfx/shmup/game/shot.ogg"))
  (.play shot)
  )

(defn set-volume
  "Set global volume level, 0 = mute, 1 = 100%"
  [level]
  (.volume js/Howler level))

(comment
  (set-volume 1)
  )
