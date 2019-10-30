(ns minasss-games.pixi.scene
  "It would be AWESOME to use a language like hiccup to define a scene or a
  container, it could look something like this
  `[:container {:anchor [0 0] :position [100 100]}
    [:sprite 'sprite/image.png' {:position [50 50]}]
    [:text 'Hello world' {:style {:fill '#d73637' :fontSize 16}}]]`

  It would be even more handy with graphics
  [:graphic
     [:fill-mode 0xff00ff
        [rect 0 0 100 100]]]

  This namespace contains functions to provide such a declarative API"
  (:require [minasss-games.pixi :as pixi]))

(defn valid-tag?
  "Return true if the provided parameter is a valid tag"
  [tag]
  (or (keyword? tag)
    (symbol? tag)
    (string? tag)))

(defn un-nest-children
  "Try to find the innermost sequence that make sense in the context
  of scene creation. at most two levels of nesting are supported
  meaning that sub elements of a scene can be provided at the same level of
  the parent container or inside another sequence type of object"
  [children]
  (let [first-level (first children)]
    (cond
      (valid-tag? first-level) children
      (valid-tag? (first first-level)) children
      :else (un-nest-children first-level))))

(defn fix-children
  [x]
  (cond
    (nil? x) []
    (or (seq? x) (vector? x)) (un-nest-children x)
    :else x))

(defn normalize
  "An element is composed by a tag, attributes and a children collection.
  content vector can hold:
  - an optional attribute map which will contain element attributes/properties (such as position, anchor etc), this must be in the first place
  - a optional collection of children's definitions
  This function ensures that an element is in the correct format"
  [[tag & content]]
  (when-not (valid-tag? tag)
    (throw (ex-info (str tag " is not a valid element name.") {:tag tag :content content})))

  (let [map-attrs (first content)]
    (if (map? map-attrs)
      [tag
       map-attrs
       (fix-children (next content))]
      [tag
       {}
       (fix-children content)])))

(defmulti make-element
  (fn [tag _attributes] tag))

(defmethod make-element :container
  [tag attrs]
  (let [container (pixi/make-container)]
    (pixi/set-attributes container attrs)))

(defmethod make-element :sprite
  [tag attrs]
  (let [texture (:texture attrs)
        container (if (map? texture)
                    (pixi/make-sprite-from-spritesheet (:spritesheet texture) (:texture texture))
                    (pixi/make-sprite texture))
        cleaned-attrs (dissoc attrs :texture)]
    (pixi/set-attributes container cleaned-attrs)))

(defmethod make-element :animated-sprite
  [tag attrs]
  (let [container (pixi/make-animated-sprite (:spritesheet attrs) (:animation-name attrs))
        cleaned-attrs (dissoc attrs :spritesheet :animation-name)]
    (pixi/set-attributes container cleaned-attrs)))

(defmethod make-element :text
  [tag attrs]
  (let [style (when-let [style-attrs (:style attrs)] (pixi/make-text-style style-attrs))
        text (:text attrs)
        cleaned-attrs (dissoc attrs :text)
        container (if (some? style) (pixi/make-text text style) (pixi/make-text text))]
    (pixi/set-attributes container cleaned-attrs)))

(defn render
  [element]
  (let [[tag attributes children] (normalize element)
        rendered (make-element tag attributes)]
    (pixi/add-children rendered (map render children))))
