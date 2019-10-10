(ns minasss-games.tween
  "this namespace will collect all tweening and easing functions
  useful for simple animations"
  (:require [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]))

(def view-updater-functions_ (atom (list)))

(defn update-tweens
  [delta-time]
  (swap! view-updater-functions_
    (fn [functions]
      (into () (filter (fn [f] (f delta-time)) functions)))))

(defn register-tween
  [f]
  (swap! view-updater-functions_ conj f))

(defn move-to
  [{:keys [target starting-position target-position speed on-complete]}]
  (let [dir (math/direction target-position starting-position)
        normalized-dir (math/normalize dir)
        calculated-pos (volatile! starting-position)
        prev-distance (volatile! (math/length dir))]
    (register-tween
      (fn [delta-time]
        (vswap! calculated-pos (fn [pos]
                                 {:x (+ (:x pos) (* delta-time speed (:x normalized-dir)))
                                  :y (+ (:y pos) (* delta-time speed (:y normalized-dir)))}))
        (let [distance (math/length (math/direction target-position @calculated-pos))]
          (if (> distance @prev-distance)
            (do
              (pixi/set-position target (:x target-position) (:y target-position))
              (when (some? on-complete)
                (on-complete))
              false)
            (do
              (vreset! prev-distance distance)
              (pixi/set-position target (:x @calculated-pos) (:y @calculated-pos))
              true)))))))
