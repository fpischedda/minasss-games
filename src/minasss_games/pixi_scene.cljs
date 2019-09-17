(ns minasss-games.pixi-scene
  "It would be AWESOME to use a language like hiccup to define a scene or a
  container, it could look something like this
  `[:container {:anchor [0 0] :position [100 100]}
    [:sprite 'sprite/image.png' {:position [50 50]}]
    [:text 'Hello world' {:style {:fill '#d73637' :fontSize 16}}]]`

  It would be even more handy with graphics
  [:graphic
     [:fill-mode 0xff00ff
        [rect 0 0 100 100]]]"
  (:require [minasss-games.pixi :as pixi]))

(def tile
  "an example container definition"
  [:container {:position [64 64]}
   [:sprite {:texture "images/tile.pn" :scale [2 2]}]
   [:text {:id "energy-text" :style {:fill "#12ae2a" :font-size 16} :anchor [1 0] :position [64 0]} "Hello"]
   [:text {:id "cost-text" :style {:fill "#d73637" :font-size 16} :anchor [1 1] :position [64 64]}] "World!"])

(defn element?
  "Return true if `x` is an valid element. True when `x` is a vector
  and the first element is a keyword, e.g. `[:sprite]` or `[:container [:text \"x\"]`."
  [x]
  (and (vector? x)
       (keyword? (first x))))

(defn children
  "Normalize the children of a HTML element."
  [x]
  (->> (cond
         (nil? x) '()

         (element? x) (list x)
         (and (list? x) (symbol? (first x))) (list x)

         (list? x) x

         (and (sequential? x)
              (= (count x) 1)
              (sequential? (first x))
              (not (string? (first x)))
              (not (element? (first x))))
         (children (first x))

         (sequential? x) x

         :else (list x))
       (remove nil?)))

(defn element
  "An element is composed by a tag and a content vector
  content vector can hold:
  - an attribute map which will hold element attributes/properties (such as position, anchor etc)
  - a collection holding this container's children
  This function ensures that an element is in the correct format"
  [[tag & content]]
  (when-not (or (keyword? tag)
                (symbol? tag)
                (string? tag))
    (throw (ex-info (str tag " is not a valid element name.") {:tag tag :content content})))

  (let [map-attrs (first content)]
    (if (map? map-attrs)
      [tag
       map-attrs
       (children (next content))]
      [tag
       {}
       (children content)])))

(defmulti make-element
  (fn [element] first))

(defn make-element-children
  [element children]
  (map #(pixi/add-child element (make-element %)) children))

(defmethod make-element :container
  [[tag attrs children]]
  (let [container (pixi/make-container)]
    (pixi/set-attributes container attrs)
    (make-element-children container children)))

(defmethod make-element :sprite
  [[tag attrs children]]
  (let [texture (:texture attrs)
        container (pixi/make-sprite texture)]
    (pixi/set-attributes container attrs)
    (make-element-children container children)))

(defmethod make-element :text
  [[tag attrs children]]
  (let [style (when-let [style-attrs (:style attrs)] (pixi/make-text-style style-attrs))
        text (:text attrs)
        container (if (some? style) (pixi/make-text text style) (pixi/make-text text))]
    (pixi/set-attributes container attrs)
    (make-element-children container children)))
