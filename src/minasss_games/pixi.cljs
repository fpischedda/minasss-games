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

(defn ^:export load-resources-progress-callback
  [loader resources]
  (println "loading callllllback"))

(defn load-resources
  "Load resourse specified by `resources` array, when finished
  call loaded-fn callback"
  [resources loaded-fn]
  (->
    (.add js/PIXI.Loader.shared (clj->js resources))
    (.on "progress" load-resources-progress-callback)
    (.load loaded-fn)))

(defn get-texture
  "Return a texture from the texture cache, by name"
  [texture-name]
  (get (.. js/PIXI -Loader -shared -resources) texture-name))

(defn make-sprite
  "Create a sprite prividing a texture name"
  [texture-name]
  (js/PIXI.Sprite. (get-texture texture-name)))
