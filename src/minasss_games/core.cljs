(ns minasss-games.core
  "A small experimetn with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:pixi (pixi/create-app 800 600)}))

(pixi/add-app-to-dom (:pixi @app-state))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
