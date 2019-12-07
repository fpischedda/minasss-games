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
                        "images/shmup/game/player.json"
                        "images/shmup/game/bullet.png"
                        "images/shmup/game/ufo.json"]
            :key-mapping {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                          "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                          "ArrowLeft" ::move-left "h" ::move-left "a" ::move-left
                          "ArrowRight" ::move-right "l" ::move-right "d" ::move-right
                          "z" ::fire "m" ::fire}
            :state (atom nil)})

(def main-stage (pixi/make-container))

(defn make-animated-ufo
  "Create animated ufo element"
  [ufo]
  (element/render
    [:animated-sprite {:spritesheet "images/shmup/game/ufo.json"
                       :animation-name "ufo"
                       :animation-speed 0.1
                       :autostart true
                       :position (:position ufo)
                       :name "ufo"}]))

(defn make-player
  "Create player element"
  [player]
  (element/render
    [:animated-sprite {:spritesheet "images/shmup/game/player.json"
                       :animation-name "player"
                       :animation-speed 0.1
                       :autostart true
                       :position (:position player)
                       :name "player"}]))

(defn make-bullet
  [position]
  (element/render
    [:sprite {:texture "images/shmup/game/bullet.png"
              :position position}]))

(defn spawn-bullet
  [player]
  (let [position (:position player)
        view (make-bullet position)]
    (pixi/add-child main-stage view)
    {:position position
     :speed 100
     :direction [0 -1]
     :view view}))

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

(defn handle-gamepad!
  []
  (swap! actions_
    (fn [actions]
      (assoc actions
        ::move-left (gamepad/button-down 0 DPAD-LEFT)
        ::move-right (gamepad/button-down 0 DPAD-RIGHT)
        ::move-up (gamepad/button-down 0 DPAD-UP)
        ::move-down (gamepad/button-down 0 DPAD-DOWN)))))

(defn handle-actions!
  [state]
  (let [actions @actions_
        dir-x (cond
                (::move-left actions) -1
                (::move-right actions) 1
                :else 0)
        dir-y (cond
                (::move-up actions) -1
                (::move-down actions) 1
                :else 0)
        bullets (:bullets state)
        new-bullets (if (::fire actions) (conj bullets (spawn-bullet (:player state))) bullets)]
    (-> state
      (assoc-in [:player :direction] (math/normalize [dir-x dir-y]))
      (assoc :bullets new-bullets))))

(defn update-bullets!
  [state delta-time]
  (update state :bullets
    (fn [bullets]
      (mapv
        (fn [{:keys [position direction speed] :as bullet}]
          (let [new-pos (math/translate position (math/scale direction (* speed delta-time)))]
            (assoc bullet :position new-pos))) bullets))))

(defn move-player!
  [state delta-time]
  (let [{:keys [position direction speed]} (:player state)
        new-pos (math/translate position (math/scale direction (* speed delta-time)))]
    (assoc-in state [:player :position] new-pos)))

(comment
  (js/console.log (clj->js (:player @(:state scene))))
  (math/normalize [1 1])
  (math/translate [200 200] (math/scale [1 0] 0.5)))

(defn update-view!
  [state]
  (pixi/set-position (get-in state [:view :player]) (get-in state [:player :position]))
  (mapv (fn [bullet] (pixi/set-position (:view bullet) (:position bullet))) (:bullets state)))

(defmethod scene-update ::game
  [scene delta-time]
  (when (:view @(:state scene))
    ;; (handle-gamepad!)
    (swap! (:state scene)
      (fn [state]
        (-> state
          handle-actions!
          (move-player! delta-time)
          (update-bullets! delta-time)
          )))
    (update-view! @(:state scene))))

;; put scene to initial state
(defmethod scene-init ::game
  [scene]
  (reset! (:state scene) {:player {:position [200 400]
                                   :energy 100
                                   :speed 100
                                   :direction [0 0]}
                          :bullets []
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
