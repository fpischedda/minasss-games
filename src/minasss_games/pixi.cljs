(ns minasss-games.pixi
  "Simple wrapper around PIXI javascript library"
  (:require [cljsjs.pixi]))


(def Loader js/PIXI.Loader.shared)
(def Resources js/PIXI.Loader.shared.resources)

(defn add-app-to-dom
  "Add the specified app to the DOM"
  [app]
  (js/document.body.appendChild (.-view app)))

(defn make-app
  "Instantiate a PIXI app"
  ([options-map]
   (let [app (js/PIXI.Application. (clj->js options-map))]
     (add-app-to-dom app)
     app))
  ([width height]
   (let [app (js/PIXI.Application. #js {:width width :height height})]
     (add-app-to-dom app)
     app)))

(defn ^:export load-resources-progress-callback
  [loader resource]
  (println "loading " (.-url resource) " , total " (.-progress loader)))

(defn load-resources
  "Load resourse specified by `resources` array, when finished
  call loaded-fn callback"
  [resources loaded-fn]
  (->
    (.add Loader (clj->js resources))
    (.on "progress" load-resources-progress-callback)
    (.load loaded-fn)))

(defn get-texture
  "Return a texture from the texture cache, by name"
  [texture-name]
  (let [tex (aget Resources texture-name)]
    (if (nil? tex)
      (println "could not find texture " texture-name)
      (.-texture tex))))

(defn make-container
  "Create a PIXI container"
  []
  (js/PIXI.Container.))

(defn make-sprite
  "Create a sprite prividing a texture name"
  [texture-name]
  (js/PIXI.Sprite. (get-texture texture-name)))

(defn add-to-stage
  "Add container to provided stage"
  [stage container]
  (.addChild stage container))

(defn add-to-app-stage
  "Add container to application main stage"
  [app container]
  (add-to-stage (.-stage app) container))

(defn make-text-style
  "Create a text style to be used to create Text objects"
  [options-map]
  (js/PIXI.TextStyle. (clj->js options-map)))

(defn make-text
  "Create a text style to be used to create Text objects"
  [text style]
  (js/PIXI.Text. text style))

(defn make-graphics
  "Create a Graphics object, used to draw shapes"
  []
  (js/PIXI.Graphics.))
