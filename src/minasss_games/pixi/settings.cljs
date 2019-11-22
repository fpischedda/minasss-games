(ns minasss-games.pixi.settings
  "Encapsulate PIXI.settings providing an easiear API
  (compared to using js directly)

  PIXI.settings is an associative array so it is possible to access and set its
  properties using aget/aset"
  (:require [oops.core :refer [oget oget+ oset!+]]))

(defn get-by-name
  [name]
  (let [settings (oget js/PIXI "settings")]
    (oget+ settings name)))

(defn set-by-name!
  [name value]
  (let [settings (oget js/PIXI "settings")]
    (oset!+ settings name value)))

(defmulti set!
  (fn [setting _param] setting))

(defmethod set! :scale-mode
  [_setting scale-mode]
  (let [mode (if (= :nearest scale-mode)
               js/PIXI.SCALE_MODES.NEAREST
               js/PIXI.SCALE_MODES.LINEAR)]
    (set-by-name! "SCALE_MODE" mode)))
