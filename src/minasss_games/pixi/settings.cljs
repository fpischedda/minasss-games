(ns minasss-games.pixi.settings
  "Encapsulate PIXI.settings providing an easiear API
  (compared to using js directly)

  PIXI.settings is an associative array so it is possible to access and set its
  properties using aget/aset"
  (:require
   [pixi.js :as pixi]
   [oops.core :refer [oget oget+ oset!+]]))

(def Settings (oget pixi "settings"))

(def ScaleModeNearest pixi.SCALE_MODES.NEAREST)
(def ScaleModeLinear pixi.SCALE_MODES.LINEAR)
(def scale-modes {:nearest ScaleModeNearest
                  :linear  ScaleModeLinear})
(defn get-by-name
  [name]
  (oget+ Settings name))

(defn set-by-name!
  [name value]
  (oset!+ Settings name value))

(defmulti set!
  (fn [setting _param] setting))

(defmethod set! :scale-mode
  [_setting scale-mode]
  (set-by-name! "SCALE_MODE" (get scale-modes scale-mode ScaleModeNearest)))
