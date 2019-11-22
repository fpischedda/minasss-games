(ns minasss-games.core
  "A small experiment with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.director :as director]
            [minasss-games.experiments.awwwliens.intro :as awwwliens]))
            ;; [minasss-games.experiments.harvest-bot :as harvest-bot]))

(enable-console-print!)

(defn init
  []
  (let [app (pixi/make-app {:width (.-innerWidth js/window)
                            :height (.-innerHeight js/window)})]
    (director/init app)
    (director/start-scene awwwliens/scene)
    app))

(defonce ^:export app (init))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
