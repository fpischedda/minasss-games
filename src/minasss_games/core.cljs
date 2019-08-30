(ns minasss-games.core
  "A small experimetn with PIXI javascript library"
  (:require [minasss-games.pixi :as pixi]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:pixi (pixi/make-app 512 512)}))

(defn ^:export loaded-callback []
  (println "load callback")
  (let [background (pixi/make-sprite "images/background.png")
        sprite (pixi/make-sprite "images/sprite.png")
        stage (.-stage (:pixi @app-state))]
    (.addChild stage background)
    (.addChild stage sprite)))

(pixi/add-app-to-dom (:pixi @app-state))

(pixi/load-resources ["images/background.png" "images/sprite.png"] loaded-callback)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
