(ns minasss-games.pixi
  "Simple wrapper around PIXI javascript library"
  (:require [cljsjs.pixi]
            [oops.core :refer [oget oget+ oset!]]))


(def Loader (oget js/PIXI.Loader "shared"))
(def Resources (oget Loader "resources"))
(def Ticker (oget js/PIXI.Ticker "shared"))

(defn add-app-to-dom
  "Add the specified app to the DOM"
  [app]
  (js/document.body.appendChild (oget app "view")))

(defn make-ticker
  "Create a ticker registering an handler"
  [handler-fn]
  (let [ticker (js/PIXI.Ticker.)]
    (.add ticker handler-fn)
    ticker))

(defn add-to-shared-ticker
  "Add a listerner to the shared ticker"
  [handler-fn]
  (.add Ticker handler-fn))

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
  (println "loading " (oget resource "url") " , total " (oget loader "progress")))

(defn load-resources
  "Load resourse specified by `resources` array, when finished
  call loaded-fn callback
  do not load resources already in the Resource pool"
  [resources loaded-fn]
  (let [to-load (filter #(nil? (aget Resources %)) resources)]
    (->
      (.add Loader (clj->js to-load))
      (.on "progress" load-resources-progress-callback)
      (.load loaded-fn))))

(defn get-texture
  "Return a texture from the texture cache, looking up by name"
  [texture-name]
  (let [tex (aget Resources texture-name)]
    (if (nil? tex)
      (println "could not find texture " texture-name)
      (oget tex "texture"))))

(defn get-spritesheet
  "Return a spritesheet from the texture cache, by name"
  [resource-name]
  (let [res (aget Resources resource-name)]
    (if (nil? res)
      (println "could not find spritesheet " resource-name)
      (oget res "spritesheet"))))

(defn get-spritesheet-texture
  "Return a texture from a spritesheet
  spritesheet and texture are specified by name"
  [resource-name texture-name]
  (aget (oget+ (get-spritesheet resource-name) "textures") texture-name))

(defn make-container
  "Create a PIXI container"
  []
  (js/PIXI.Container.))

(defn make-sprite
  "Create a sprite prividing a texture name"
  [texture-name]
  (js/PIXI.Sprite. (get-texture texture-name)))

(defn make-sprite-from-spritesheet
  "Create a sprite prividing a spritesheet name and texture name"
  [spritesheet-name texture-name]
  (js/PIXI.Sprite. (get-spritesheet-texture spritesheet-name texture-name)))

(defn make-animated-sprite
  "Create a animated sprite prividing a spritesheet name"
  [spritesheet-name animation-name]
  (js/PIXI.AnimatedSprite. (oget+ (oget (get-spritesheet spritesheet-name) "animations") animation-name)))

(defn add-child
  "Add child to provided parent container"
  [parent child]
  (.addChild parent child))

(defn add-children
  "Add children to provided parent container"
  [parent children]
  (dorun (map #(add-child parent %) children))
  parent)

(defn get-child-by-name
  "return, if any, the container's child identified by name"
  [container child-name]
  (.getChildByName container child-name))

(defn remove-container
  "Remove a container from the scene"
  [container]
  (.removeChild (oget container "parent") container))

(defn remove-child-by-name
  "Remove the child specified by name"
  [parent child-name]
  (if-let [child (get-child-by-name parent child-name)]
    (.removeChild parent child)))

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
  (dorun (map #(add-child-view parent %) children))
  parent)

(defn add-to-app-stage
  "Add container to application main stage"
  [app container]
  (.addChild (oget app "stage") container)
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
  ([container [x y]]
   (set-position container x y))
  ([container x y]
   (.set (oget container "position") x y)
   container))

(defn set-anchor
  "Set anchor of any PIXI/Container subclass"
  [container x y]
  (.set (oget container "anchor") x y)
  container)

(defn set-pivot
  "Set pivot of any PIXI/Container subclass"
  [container x y]
  (.set (oget container "pivot") x y)
  container)

(defn set-scale
  "Set scale of any PIXI/Container subclass"
  ([container scale]
   (set-scale scale scale))
  ([container x y]
   (.set (oget container "scale") x y)
   container))

(defn set-name
  "Set name of any PIXI/DisplayObject subclass"
  [container name]
  (oset! container "name" name)
  container)

(defn set-text
  "Set text content of PIXI/Text instance"
  [container text]
  (oset! container "text" text)
  container)

(defn set-alpha
  "Set alpha of any PIXI/DisplayObject subclass"
  [container value]
  (oset! container "alpha" value)
  container)

(defn set-animation-speed
  "Set animation-speed of a PIXI/AnimatedSprite instance"
  [container value]
  (oset! container "animationSpeed" value)
  container)

(defn set-visible
  "Set visibility of any PIXI/DisplayObject subclass"
  [container visible?]
  (oset! container "visible" visible?)
  container)

(defn set-texture
  "Set texture of any PIXI/Sprite subclass"
  [container texture]
  (oset! container "texture" (if (string? texture) (get-texture texture) texture))
  container)

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

(defmethod set-attribute :scale
  ([container _attribute scale]
   (set-scale container scale scale))
  ([container _attribute x y]
   (set-scale container x y)))

(defmethod set-attribute :name
  [container _attribute name]
  (set-name container name))

(defmethod set-attribute :alpha
  [container _attribute value]
  (set-alpha container value))

(defmethod set-attribute :animation-speed
  [container _attribute value]
  (set-animation-speed container value))

(defmethod set-attribute :visible
  [container _attribute visible?]
  (set-visible container visible?))

(defmethod set-attribute :texture
  [container _attribute texture]
  (set-texture container texture))

(defmethod set-attribute :text
  [container _attribute text]
  (set-text container text))

(defn set-attributes
  "Given a container subclass set its attributes by attributes map,
  there is no type checking so trying to set properties not available
  to some classes may or may not break everything"
  [container attributes]
  (dorun (map (fn [[attr value]] (set-attribute container attr value)) attributes))
  container)
