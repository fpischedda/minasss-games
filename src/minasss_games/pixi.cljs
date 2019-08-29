(ns minasss-games.pixi
  "Simple wrapper around PIXI javascript library"
  (:require [cljsjs.pixi]))


(defn make-app
  "Instantiate a PIXI app"
  [width height]
  (js/PIXI.Application. #js {:width width :height height}))

(defn add-app-to-dom
  "Add the specified app to the DOM"
  [app]
  (js/document.body.appendChild (.-view app)))

(defn get-texture
  "Return a texture from the texture cache, by name"
  [texture-name]
  (get (.. js/PIXI -loader -resources) texture-name))

(defn make-sprite
  "Create a sprite prividing a texture name"
  [texture-name]
  (js/PIXI.Sprite. (get-texture texture-name)))
