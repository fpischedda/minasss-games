(ns minasss-games.core
  "A small experimetn with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.pixi.experiments.harvest-bot :as harvest-bot]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:pixi (pixi/make-app 512 512)}))

(pixi/add-app-to-dom (:pixi @app-state))
(harvest-bot/init (:pixi @app-state))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
