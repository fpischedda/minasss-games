(ns minasss-games.pixi.settings)

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
