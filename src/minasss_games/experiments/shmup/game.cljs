(ns minasss-games.experiments.shmup.game
  (:require [minasss-games.director
             :as
             director
             :refer
             [scene-cleanup scene-init scene-key-up scene-update scene-ready]]
            [minasss-games.element :as element]
            [minasss-games.gamepad :as gamepad]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]))

(def scene {:id ::game
            :resources ["images/shmup/game/background.png"
                        "images/shmup/game/baloon.png"
                        "images/shmup/game/cow-still.png"
                        "images/shmup/game/ufo.json"]
            :key-mapping {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                          "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                          "ArrowLeft" ::move-left "h" ::move-left "a" ::move-left
                          "ArrowRight" ::move-right "l" ::move-right "d" ::move-right
                          "z" ::fire "m" ::fire}})

(def main-stage (pixi/make-container))

(defmethod scene-key-down ::game
  [scene _native action]
  )

(defmethod scene-key-up ::game
  [scene _native action]
  )

(def DPAD-UP 0)
(def DPAD-DOWN 1)
(def DPAD-LEFT 2)
(def DPAD-RIGHT 3)
(def BUTTON-A 4)

(defmethod scene-update ::game
  [scene delta-time]
  (cond
    (gamepad/button-pressed 0 BUTTON-A) (set-action! ::fire)
    (gamepad/button-pressed 0 DPAD-UP) (set-action! ::move-up)
    (gamepad/button-pressed 0 DPAD-DOWN) (set-action! ::move-down)
    (gamepad/button-pressed 0 DPAD-LEFT) (set-action! ::move-left)
    (gamepad/button-pressed 0 DPAD-RIGHT) (set-action! ::move-right)))

;; setup the view based on the menu-items_ atom; main-stage refers to the
;; root container, where other graphical elements will be added
(defmethod scene-ready ::game
  [_scene app-stage]
  (let [background (pixi/make-sprite "images/awwwliens/menu/background.png")]
    (pixi/add-child main-stage background)

    (pixi/add-child app-stage main-stage)))

(defmethod scene-cleanup ::game
  [_]
  (screenplay/clean-actions)
  (pixi/remove-container main-stage))
