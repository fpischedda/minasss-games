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

(defn add-children
  "Add children to provided parent container"
  [parent children]
  (mapv #(add-child parent %) children)
  parent)

(defn add-child-view
  "Add child to provided parent container
  this uses a higher level concept of `view`
  a view is a map that contains a main container in the
  :view key and a map of entities in the entities key"
  [parent child]
  (.addChild parent (:view child))
  parent)

(defn add-children-view
  "Add children `view` to provided parent container"
  [parent children]
  (mapv #(add-child-view parent %) children)
  parent)

(defn add-to-app-stage
  "Add container to application main stage"
  [app container]
  (.addChild (.-stage app) container)
  container)

(defn make-text-style
  "Create a text style to be used to create Text objects"
  [options-map]
  (js/PIXI.TextStyle. (clj->js options-map)))

(defn make-text
  "Create a text style to be used to create Text objects"
  ([text]
   (js/PIXI.Text. text))
  ([text style]
   (js/PIXI.Text. text style)))

(defn make-graphics
  "Create a Graphics object, used to draw shapes"
  []
  (js/PIXI.Graphics.))

(defn set-position
  "Set position of any PIXI/Container subclass"
  [container x y]
  (.set (.-position container) x y)
  container)

(defn set-anchor
  "Set anchor of any PIXI/Container subclass"
  [container x y]
  (.set (.-anchor container) x y)
  container)

(defn set-pivot
  "Set pivot of any PIXI/Container subclass"
  [container x y]
  (.set (.-pivot container) x y)
  container)

(defn set-scale
  "Set scale of any PIXI/Container subclass"
  [container x y]
  (.set (.-scale container) x y)
  container)

(defn set-name
  "Set name of any PIXI/DisplayObject subclass"
  [container name]
  (aset container "name" name)
  container)

(defn make-ticker
  "Create a ticker registering an handler"
  [handler-fn]
  (let [ticker (js/PIXI.Ticker.)]
    (.add ticker handler-fn)
    ticker))

(defmulti set-attribute
  (fn [container attribute value] attribute))

(defmethod set-attribute :default
  [container _attribute _value]
  container)

(defmethod set-attribute :position
  [container _attribute [x y]]
  (set-position container x y))

(defmethod set-attribute :scale
  [container _attribute [x y]]
  (set-scale container x y))

(defmethod set-attribute :anchor
  [container _attribute [x y]]
  (set-anchor container x y))

(defmethod set-attribute :pivot
  [container _attribute [x y]]
  (set-pivot container x y))

(defmethod set-attribute :name
  [container _attribute name]
  (set-name container name))

(defn set-attributes
  "Given a container subclass set its attributes by attributes map,
  there is no type checking so trying to set properties not available
  to some classes may or may not break everything"
  [container attributes]
  (mapv (fn [[attr value]] (set-attribute container attr value)) attributes)
  container)

(defn get-child-by-name
  "return, if any, the container's child identified by name"
  [container child-name]
  (.getChildByName container child-name))
