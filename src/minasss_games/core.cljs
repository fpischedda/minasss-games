(ns minasss-games.core
  "A small experimetn with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:pixi (pixi/make-app 512 512)}))

(defn ^:export callback []
  (println "load callback")
  (let [background (pixi/make-sprite "images/background.png")]
    (.addChild (.-stage (:pixi @app-state)) background)))

(pixi/load-resources ["images/background.png"] callback)

(pixi/add-app-to-dom (:pixi @app-state))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
