(ns minasss-games.experiments.harvest-bot
  (:require [minasss-games.pixi :as pixi]))

(def main-stage (pixi/make-container))

(defn ^:export loaded-callback []
  (let [background (pixi/make-sprite "images/background.png")
        sprite (pixi/make-sprite "images/sprite.png")]
    (.addChild main-stage background)
    (.addChild main-stage sprite)))

(defn init [app]
  (pixi/load-resources ["images/background.png" "images/sprite.png"] loaded-callback)
  (pixi/add-to-app-stage app main-stage))
