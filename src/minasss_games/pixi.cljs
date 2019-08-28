(ns minasss-games.pixi
  "Simple wrapper around PIXI javascript library"
  (:require [cljsjs.pixi]))


(defn create-app
  "Instantiate a PIXI app"
  [width height]
  (js/PIXI.Application. #js {:width width :height height}))

(defn add-app-to-dom
  "Add the specified app to the DOM"
  [app]
  (js/document.body.appendChild (.-view app)))
