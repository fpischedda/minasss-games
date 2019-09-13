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

(defn add-child
  "Add child to provided parent container"
  [parent child]
  (.addChild parent child))

(defn add-child-view
  "Add child to provided parent container
  this uses a higher level concept of `view`
  a view is a map that contains a main container in the
  :view key and a map of entities in the entities key"
  [parent child]
  (.addChild parent (:view child)))

(defn add-children
  "Add children to provided parent container"
  [parent children]
  (map #(add-child parent %) children))

(defn add-to-app-stage
  "Add container to application main stage"
  [app container]
  (.addChild (.-stage app) container))

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

(defn set-position
  "Set position of any PIXI/Container subclass"
  [container x y]
  (.set (.-position container) x y))

(defn set-anchor
  "Set anchor of any PIXI/Container subclass"
  [container x y]
  (.set (.-anchor container) x y))
