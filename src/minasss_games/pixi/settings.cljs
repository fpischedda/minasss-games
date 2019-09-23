(ns minasss-games.pixi.settings
  "Encapsulate PIXI.settings providing an easiear API
  (compared to using js directly)

  PIXI.settings is an associative array so it is possible to access and set its
  properties using aget/aset")

(defn get-by-name!
  [name value]
  (aget js/PIXI.settings name))

(defn set-by-name!
  [name value]
  (aset js/PIXI.settings name value))

(defmulti set!
  (fn [setting _param] setting))

(defmethod set! :scale-mode
  [_setting scale-mode]
  (let [mode (if (= :nearest scale-mode)
               js/PIXI.SCALE_MODES.NEAREST
               js/PIXI.SCALE_MODES.LINEAR)]
    (set-by-name! "SCALE_MODE" mode)))
