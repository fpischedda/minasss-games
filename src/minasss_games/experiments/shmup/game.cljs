(ns minasss-games.experiments.shmup.game
  (:require [minasss-games.director
             :as
             director
             :refer
             [scene-cleanup scene-init scene-key-down scene-key-up scene-update scene-ready]]
            [minasss-games.element :as element]
            [minasss-games.gamepad :as gamepad]
            [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]))

(def scene {:id ::game
            :resources ["images/shmup/game/background.png"
                        "images/shmup/game/player.png"
                        "images/shmup/game/ufo.json"]
            :key-mapping {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                          "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                          "ArrowLeft" ::move-left "h" ::move-left "a" ::move-left
                          "ArrowRight" ::move-right "l" ::move-right "d" ::move-right
                          "z" ::fire "m" ::fire}
            :state (atom nil)})

(defn make-animated-ufo
  "Create animated ufo element"
  [ufo]
  (element/render
    [:animated-sprite {:spritesheet "images/shmup/game/ufo.json"
                       :animation-name "ufo"
                       :animation-speed 0.05
                       :autostart true
                       :position (:position ufo)
                       :name "ufo"}]))

(defn make-player
  "Create player element"
  [player]
  (element/render
    [:sprite {:texture "images/shmup/game/player.png"
              :position (:position player)
              :name "player"}]))

(def main-stage (pixi/make-container))

(def actions_ (atom {}))

(defmethod scene-key-down ::game
  [scene _native action]
  (swap! actions_ assoc action true))

(defmethod scene-key-up ::game
  [scene _native action]
  (swap! actions_ dissoc action))

(def DPAD-UP 0)
(def DPAD-DOWN 1)
(def DPAD-LEFT 2)
(def DPAD-RIGHT 3)
(def BUTTON-A 4)

(defn handle-input!
  [scene]
  (swap! (:state scene)
    (fn [state]
      (let [actions @actions_
            dir-x (cond
                    (::move-left actions) -1
                    (::move-right actions) 1
                    :else 0)
            dir-y (cond
                    (::move-up actions) -1
                    (::move-down actions) 1
                    :else 0)
            direction (math/normalize [dir-x dir-y])]
        (assoc-in state [:player :direction] direction))))
  )

(defn move-player!
  [scene delta-time]
  (swap! (:state scene)
    (fn [state]
      (let [{:keys [position direction speed]} (:player state)
            new-pos (math/translate position (math/scale direction (* speed delta-time)))]
        (assoc-in state [:player :position] new-pos)))))

(comment
  (js/console.log (clj->js (:player @(:state scene))))
  (math/normalize [1 1])
  (math/translate [200 200] (math/scale [1 0] 0.5)))

(defn update-view! [scene]
  (let [state @(:state scene)]
    (pixi/set-position (get-in state [:view :player]) (get-in state [:player :position]))))

(defmethod scene-update ::game
  [scene delta-time]
  (when (:view @(:state scene))
    (handle-input! scene)
    (move-player! scene delta-time)
    (update-view! scene)))

;; put scene to initial state
(defmethod scene-init ::game
  [scene]
  (reset! (:state scene) {:player {:position [200 400]
                                   :energy 100
                                   :speed 100
                                   :direction [0 0]}
                          :ufo {:position [200 200]
                                :energy 100}}))

;; setup the view adding the ufo and the player
(defmethod scene-ready ::game
  [scene app-stage]
  (let [background (pixi/make-sprite "images/shmup/game/background.png")]
    (pixi/add-child main-stage background)
    (swap! (:state scene)
      (fn [state]
        (let [ufo (make-animated-ufo (:ufo state))
              player (make-player (:player state))]
          (pixi/add-child main-stage ufo)
          (pixi/add-child main-stage player)
          (assoc state :view {:ufo ufo :player player}))))
    (pixi/add-child app-stage main-stage)))

(defmethod scene-cleanup ::game
  [_]
  (pixi/remove-container main-stage))
