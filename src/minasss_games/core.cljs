(ns minasss-games.core
  "A small experiment with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.director :as director]
            [minasss-games.experiments.shmup.game :as shmup]))

(enable-console-print!)

(defn init
  []
  (let [app (pixi/make-app {:width (.-innerWidth js/window)
                            :height (.-innerHeight js/window)})]
    (director/init app)
    (director/start-scene shmup/scene)
    app))

(defonce ^:export app (init))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
