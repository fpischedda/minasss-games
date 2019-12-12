(ns minasss-games.experiments.shmup.game
  (:require [minasss-games.director
             :as
             director
             :refer
             [scene-cleanup scene-key-down scene-key-up scene-update scene-ready]]
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

(defn make-rect-bounds
  "Make a bounding rect based on provided position and rect size; rect is
  centered around position"
  [[x y] [width height]]
  (let [hw (/ width 2)
        hh (/ height 2)]
    [(- x hw) (- y hh) (+ x hw) (+ y hh)]))

(defn rects-overlap
  "Returns true if the provided rects overlaps"
  [[ax-top ay-top ax-bottom ay-bottom] [bx-top by-top bx-bottom by-bottom]]
  (and (< ax-top bx-bottom) (< ay-top by-bottom) (> ax-bottom bx-top) (> ay-bottom by-top)))

(comment
  (make-rect-bounds [100 100] [10 10])
  (make-rect-bounds [105 105] [10 10])

  (rects-overlap
    (make-rect-bounds [100 100] [10 10])
    (make-rect-bounds [125 125] [10 10])
    )
  )

(defn make-animated-ufo
  "Create animated ufo element at position"
  [position]
  (element/render
    [:animated-sprite {:spritesheet "images/shmup/game/ufo.json"
                       :animation-name "ufo"
                       :animation-speed 0.1
                       :autostart true
                       :position position
                       :anchor [0.5 0.5]
                       :name "ufo"}]))

(defn spawn-enemy
  [position]
  (let [view (make-animated-ufo position)]
    (pixi/add-child main-stage view)
    {:position position
     :collision-rect [32 32]
     :direction [0 0]
     :speed 0
     :energy 100
     :view view}))

(defn make-player
  "Create player element"
  [position]
  (element/render
    [:animated-sprite {:spritesheet "images/shmup/game/player.json"
                       :animation-name "player"
                       :animation-speed 0.1
                       :autostart true
                       :position position
                       :anchor [0.5 0.5]
                       :name "player"}]))

(defn spawn-player
  [position]
  (let [view (make-player position)]
    (pixi/add-child main-stage view)
    {:position position
     :energy 100
     :speed 100
     :collision-rect [8 8]
     :direction [0 0]
     :view view}))

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
     :anchor [0.5 0.5]
     :speed 130
     :direction [0 -1]
     :collision-rect [8 8]
     :view view}))

(def actions_ (atom {}))

(defmethod scene-key-down ::game
  [scene _native action]
  (swap! actions_ assoc action true))

(defmethod scene-key-up ::game
  [scene _native action]
  (swap! actions_ dissoc action))

(def LAXE-UP 0)
(def LAXE-DOWN 1)
(def LAXE-LEFT 2)
(def LAXE-RIGHT 3)
(def DPAD-UP 12)
(def DPAD-DOWN 13)
(def DPAD-LEFT 14)
(def DPAD-RIGHT 15)
(def BUTTON-A 0)

(defn handle-gamepad!
  []
  (swap! actions_
    (fn [actions]
      (assoc actions
        ::fire (or (::fire actions) (gamepad/button-down 0 BUTTON-A))
        ::move-left (or (::move-left actions)
                      (> 0 (gamepad/axis-status 0 DPAD-LEFT))
                      (> 0 (gamepad/axis-status 0 LAXE-LEFT)))
        ::move-right (or (::move-right actions)
                       (> 0 (gamepad/axis-status 0 DPAD-RIGHT))
                       (> 0 (gamepad/axis-status 0 LAXE-RIGHT)))
        ::move-up (or (::move-up actions)
                    (> 0 (gamepad/axis-status 0 DPAD-UP))
                    (> 0 (gamepad/axis-status 0 LAXE-UP)))
        ::move-down (or (::move-down actions)
                      (> 0 (gamepad/axis-status 0 DPAD-DOWN))
                      (> 0 (gamepad/axis-status 0 LAXE-DOWN)))))))

(defn handle-actions
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

(defn update-bullet
  "Update bullet position, if the bullet goes outside of the screen
  its sprite will be released and the bullet marked as deleted so it
  can be removed later"
  [{:keys [position direction speed collision-rect view] :as bullet} delta-time]
  (let [new-pos (math/translate position (math/scale direction (* speed delta-time)))
        delete (> 0 (nth new-pos 1))]
    (when delete
      (pixi/remove-container view))
    (assoc bullet
      :position new-pos
      :deleted delete)))

(defn update-bullets
  "Update bullets removing the ones that are not visibile anymore"
  [state delta-time]
  (update state :bullets
    (fn [bullets]
      (->> bullets
        (map #(update-bullet % delta-time))
        (remove :deleted)
        (into [])))))

(defn update-enemy
  [{:keys [position direction speed]} delta-time]
  (let [new-pos (math/translate position (math/scale direction (* speed delta-time)))]
    (assoc enemy :position new-pos)))

(defn update-enemies
  "Update enemies, don't do a lot at this point"
  [state delta-time]
  (update state :enemies
    (fn [enemies]
      (->> enemies
        (map #(update-enemy % delta-time))
        (remove :deleted)
        (into [])))))

(defn move-player
  [state delta-time]
  (let [{:keys [position direction speed]} (:player state)
        new-pos (math/translate position (math/scale direction (* speed delta-time)))]
    (assoc-in state [:player :position] new-pos)))

(comment
  (js/console.log (clj->js (:player @(:state scene))))
  (math/normalize [1 1])
  (math/translate [200 200] (math/scale [1 0] 0.5)))

(defn update-view!
  "Side effecty function that sync sprites accordingly with scene state.
  Now this function returns the state even if it is not supposed to change
  because this way it is easier to put this function in the
  `state management pipeline` and maybe in the future it can take care of
  adding or removing view elements."
  [state]
  (let [{:keys [position view]} (:player state)]
       (pixi/set-position view position))
  ;; enemies do nothing at this time but lets prepare for the future
  (doseq [{:keys [view position]} (:enemies state)]
    (pixi/set-position view position))
  ;; update active bullets
  (doseq [{:keys [view position]} (:bullets state)]
    (pixi/set-position view position))
  state)

(defn update-game-state
  "This function updates a bounch of things:
  - player
  - bullets
  - enemies
  - view state of everything
  and returns the next state of the world"
  [state delta-time]
  (handle-gamepad!)
  (-> state
    handle-actions
    (move-player delta-time)
    (update-bullets delta-time)
    (update-enemies delta-time)
    update-view!))

(defmethod scene-update ::game
  [scene delta-time]
  (swap! (:state scene)
    (fn [state]
      (when (some? state)
        (update-game-state state delta-time)))))

;; setup the view adding the enemies and the player
(defmethod scene-ready ::game
  [scene app-stage]
  (let [background (pixi/make-sprite "images/shmup/game/background.png")]
    (pixi/add-child main-stage background)
    (swap! (:state scene)
      (fn [state]
        (assoc state
          :player (spawn-player [200 400])
          :bullets []
          :enemies [(spawn-enemy [200 200]) (spawn-enemy [260 200])])))
    (pixi/add-child app-stage main-stage)))

(defmethod scene-cleanup ::game
  [_]
  (reset! (:state scene) nil)
  (pixi/remove-container main-stage))
