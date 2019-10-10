(ns minasss-games.tween
  "this namespace will collect all tweening and easing functions
  useful for simple animations"
  (:require [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]))

(def tweens-registry_ (atom (list)))

(defn register-tween
  [tween]
  (swap! tweens-registry_ conj tween))

(defmulti updater
  (fn [tween _delta-time] (:tween tween)))

(defn update-tweens
  [delta-time]
  (swap! tweens-registry_
    (fn [tweens]
      (->> tweens
        (map #(updater % delta-time))
        (filter some?)
        (into ())))))

(defmethod updater ::move-to
  [tween delta-time]
  (let [{:keys [target position target-position direction prev-distance speed]} (:params tween)
        on-complete (:on-complete tween)
        new-position {:x (+ (:x position) (* delta-time speed (:x direction)))
                      :y (+ (:y position) (* delta-time speed (:y direction)))}
        distance (math/length (math/direction target-position new-position))]
    (if (> distance prev-distance)
      ;; lifecycle of this tween finishes here; if on-complete returns
      ;; something that seems a tween return it so it can be registered
      (do
        (pixi/set-position target (:x target-position) (:y target-position))
        (when (some? on-complete)
          (let [next-tween (on-complete)]
            (when (and (map? next-tween) (some? (:tween next-tween)))
              next-tween))))
      ;; this tween has not finished yet, in updates the target and
      ;; prepares the state for next iteration
      (do
        (pixi/set-position target (:x new-position) (:y new-position))
        (update-in tween [:params] assoc :position new-position :prev-distance distance)))))

(defn move-to
  [{:keys [target starting-position target-position speed on-complete]}]
  (let [dir (math/direction target-position starting-position)
        normalized-dir (math/normalize dir)
        prev-distance (math/length dir)]
    (register-tween
      {:tween ::move-to
       :on-complete on-complete
       :params {:target target
                :position starting-position
                :target-position target-position
                :direction normalized-dir
                :prev-distance prev-distance
                :speed speed}})))
