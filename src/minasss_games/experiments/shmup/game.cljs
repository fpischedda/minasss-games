(ns minasss-games.experiments.shmup.game
  (:require [minasss-games.director
             :as
             director
             :refer
             [scene-cleanup scene-key-down scene-key-up scene-update scene-ready]]
            [minasss-games.collision :refer [make-rect-bounds rects-overlap]]
            [minasss-games.element :as element]
            [minasss-games.gamepad :as gamepad]
            [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]
            [minasss-games.sound :as sound]
            [minasss-games.experiments.shmup.enemy-behaviour :as enemy-behaviour]))

(def scene {:id ::game
            :resources ["images/shmup/game/background.png"
                        "images/shmup/game/player.json"
                        "images/shmup/game/bullet.png"
                        "images/shmup/game/ufo.json"
                        "images/shmup/game/boss.png"]
            :key-mapping {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                          "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                          "ArrowLeft" ::move-left "h" ::move-left "a" ::move-left
                          "ArrowRight" ::move-right "l" ::move-right "d" ::move-right
                          "z" ::fire "m" ::fire}
            :state (atom nil)})

(def main-stage (pixi/make-container))

(def sounds_ {:shot (sound/load "sfx/shmup/game/shot.ogg")
              :explosion (sound/load "sfx/shmup/game/explosion.ogg")
              :impact-metal (sound/load "sfx/shmup/game/impact_metal.ogg")})

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
     :energy 10
     :view view}))

(defn make-boss-element
  [position]
  (element/render
    [:sprite {:texture "images/shmup/game/boss.png"
              :anchor [0.5 0.5]
              :position position}]))

(defn spawn-boss-enemy
  [position]
  (let [view (make-boss-element position)]
    (enemy-behaviour/init-state ::enemy-behaviour/horizontal-ping-pong
      {:position position
       :collision-rect [64 32]
       :direction [0 0]
       :speed 0
       :energy 400
       :view view})))

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
              :anchor [0.5 0.5]
              :position position}]))

(defn spawn-bullet
  [player direction]
  (let [position (:position player)
        view (make-bullet position)]
    (pixi/add-child main-stage view)
    (.play (:shot sounds_))
    {:position position
     :speed 130
     :direction direction
     :collision-rect [8 8]
     :damage 1
     :view view}))

(def FIRE-TIMEOUT-MS 0.3)
(def actions_ (atom {::fire-timeout FIRE-TIMEOUT-MS}))

(defmethod scene-key-down ::game
  [_scene _native action]
  (swap! actions_ assoc action true))

(defmethod scene-key-up ::game
  [_scene _native action]
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
  [state delta-time]
  (let [actions (swap! actions_ update ::fire-timeout - delta-time)
        dir-x (cond
                (::move-left actions) -1
                (::move-right actions) 1
                :else 0)
        dir-y (cond
                (::move-up actions) -1
                (::move-down actions) 1
                :else 0)
        ;; spawn new bullets if fire is pressed (one shot every 0.3 seconds)
        new-bullets (if (and (::fire actions) (>= 0 (::fire-timeout actions)))
                      (let [player (:player state)]
                        (swap! actions_ assoc ::fire-timeout FIRE-TIMEOUT-MS)
                        (-> (:bullets state)
                          (conj (spawn-bullet player [0 -1]))
                          (conj (spawn-bullet player (math/normalize [0.3 -1])))
                          (conj (spawn-bullet player (math/normalize [-0.3 -1])))))
                      (:bullets state))]
    (-> state
      (assoc-in [:player :direction] (math/normalize [dir-x dir-y]))
      (assoc :bullets new-bullets))))

(defn update-position-by-velocity
  [{:keys [direction speed position] :as component} dt]
  (assoc component :position
    (math/translate position (math/scale direction (* speed dt)))))

(def LIMIT-TOP 0)
(def LIMIT-BOTTOM 1200)
(def LIMIT-LEFT 0)
(def LIMIT-RIGHT 640)

(defn entity-is-out-of-boundaries
  [{:keys [position boundaries]}]
  (let [[x y] position
        [top left bottom right] boundaries]
    (or (> top y) (< bottom y)
      (> left x) (< right x))))

(defn check-entity-against-boundaries
  "If the entity goes outside of the screen
  its sprite will be released and the entity
  marked as deleted so it can be removed later"
  [entity]
  (if (and (some? (:boundaries entity)) (entity-is-out-of-boundaries entity))
    (do
      (pixi/remove-container (:view entity))
      (assoc entity :deleted true))
    entity))

(defn entities-remove-if-out-of-boundaries
  "Update entities removing the ones that are not visibile anymore"
  [state]
  (update state :entities
    (fn [entities]
      (->> entities
        (map #(check-entity-against-boundaries %))
        (remove :deleted)
        (into [])))))

(defn entities-update-behavior
  "Update enemies, don't do a lot at this point"
  [state]
  (update state :enemies
    (fn [enemies]
      (->> enemies
        (map #((if-some (::ai %) (enemy-behaviour/update-state %) %)))
        (remove :deleted)
        (into [])))))

(comment
  (js/console.log (clj->js (:player @(:state scene))))
  (math/normalize [1 1])
  (math/translate [200 200] (math/scale [1 0] 0.5)))

(defn handle-collisions
  "Detect bullets colliding with enemies and remove both from the state
  and scene graph"
  [{:keys [bullets enemies] :as state}]
  (let [collisions
        (for [{:keys [position collision-rect] :as bullet} bullets
              :let [bullet-rect (make-rect-bounds position collision-rect)]
              {:keys [position collision-rect] :as enemy} enemies
              :let [enemy-rect (make-rect-bounds position collision-rect)]
              :when (rects-overlap bullet-rect enemy-rect)]
          [bullet enemy])]
    (if (empty? collisions)
      state
      (do
        (let [bullets-to-remove (set (map first collisions))
              enemies-to-remove (set (map second collisions))]
          (doseq [bullet bullets-to-remove]
            (pixi/remove-container (:view bullet)))
          (doseq [enemy enemies-to-remove]
            (.play (:explosion sounds_))
            (pixi/remove-container (:view enemy)))
          (assoc state
            :bullets (remove bullets-to-remove (:bullets state))
            :enemies (remove enemies-to-remove (:enemies state))))))))

(defn update-entity-view
  [{:keys [view position]}]
  (pixi/set-position view position))

(defn update-view!
  "Side effecty function that sync sprites accordingly with scene state.
  Now this function returns the state even if it is not supposed to change
  because this way it is easier to put this function in the
  `state management pipeline` and maybe in the future it can take care of
  adding or removing view elements."
  [state]
  (doseq [entity (:entities state)]
    (update-entity-view entity))
  state)

(defn update-positions
  [state delta-time]
  (update-in state [:entities] mapv
    (fn [entity]
      (if-some (:speed entity)
        (update-position-by-velocity entity delta-time)
        entity))))

(defn update-game-state
  "This function updates a bounch of things:
  - player
  - bullets
  - enemies
  - collisions
  - view state of everything
  and returns the next state of the world"
  [state delta-time]
  (handle-gamepad!)
  (-> state
    (handle-actions delta-time)
    (update-positions delta-time)
    entities-remove-if-out-of-boundaries
    handle-collisions
    entities-update-behavior
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
          :entities [(spawn-player [200 400])
                     (spawn-enemy [200 200])
                     (spawn-enemy [260 200])
                     (spawn-boss-enemy [220 120])])))
    (doall [entity (get-in @state [:state :entities])]
      (pixi/add-child main-stage (:view entity)))
    (pixi/add-child app-stage main-stage)))

(defmethod scene-cleanup ::game
  [_]
  (reset! (:state scene) nil)
  (pixi/remove-container main-stage))
