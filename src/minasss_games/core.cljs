(ns minasss-games.core
  "A small experimetn with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.experiments.harvest-bot :as harvest-bot]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app (pixi/make-app {:width (.-innerWidth js/window)
                             :height (.-innerHeight js/window)}))

(harvest-bot/init app)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
