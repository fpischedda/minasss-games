(ns minasss-games.experiments.awwwliens.game
  "Entry for Autumn Lisp Game Jam Oct 2019
  https://itch.io/jam/autumn-lisp-game-jam-2019

  The concept of the game is a super simplified tamagotchi based
  a bit on the experience of the harvest_bot experiment, extending the
  exploration of the resource management topic.

  This game tells a story about an extra terrestrial race which goes crazy for
  Earth's cows, like humans with cats, dogs, guinea pigs and...ok you get the
  point. So from time to time one cow, probably the cutest one, get kidnapped
  and moved to an artificial land where she live more or less happily.
  Turns out that kinapped cows loose their mind!
  First of all it seems they are too scared to move and without external
  intervention they will stay in the same place until death. Fortunately alien
  technology can help in the form of a remote cow controller but there is a
  side effect, every time a cow moves she poops. Again this is not the end of
  the universe if you love cows as much this race does; cow manures will help
  to grow cow food!
  How long will you be able to keep the cow alive?"
  (:require [minasss-games.director :as director :refer [scene-init scene-cleanup]]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.experiments.awwwliens.core :as core]
            [minasss-games.experiments.awwwliens.view :as view]))

(def scene {:init-scene ::game-scene
            :cleanup-scene ::game-scene})

(def world_ (atom nil))

;; main-stage is still in this namespace because in the future
;; the "game" may receive the main stage where to attach its own
;; graphical elements; moving it to the view namespace might mask
;; this possible behavior
(def main-stage (pixi/make-container))

(defn handle-input
  [event-type _native direction]
  (if (= :key-up event-type)
    (condp = direction
      :exit (director/start-scene minasss-games.experiments.awwwliens.intro/scene)
      (swap! world_ core/move-cow direction))
    ))

(defn handle-cow-changed
  [world_ old-cow new-cow]
  (let [old-pos (:position old-cow)
        new-pos (:position new-cow)
        old-energy (:energy old-cow)
        new-energy (:energy new-cow)]

    ;; position changed detection, must find a better way...
    (when (not (= old-pos new-pos))
      (view/move-cow old-pos new-pos (fn [] (swap! world_ #(-> % core/eat core/grow)))))

    ;; eventually update cow energy text
    ;; if energy <= 0 then it is game over
    (when (not (= old-energy new-energy))
      (if (>= 0 new-energy)
        (director/start-scene minasss-games.experiments.awwwliens.intro/scene)
        (view/update-cow-energy new-energy)))))

(defn world-changed-listener
  "listens to changes of cow game entity, updates
  graphical object accordingly"
  [_key world_ old-state new-state]
  (let [old-cow (:cow old-state)
        new-cow (:cow new-state)]
    ;; the score is the amount of days the cow has been alive
    (view/update-score-text (:days-alive new-state))

    (when (not (= old-cow new-cow))
      (handle-cow-changed world_ old-cow new-cow))

    (doseq [rows (:area new-state)
            cell rows]
      (view/update-cell cell))))

(defn ^:export loaded-callback []
  (let [world (core/make-world {:width 3 :height 3
                                :cow-row 0 :cow-col 0 :cow-energy 2})]
    (view/setup world main-stage)
    (input/register-keys {"ArrowUp" :up "k" :up "w" :up
                          "ArrowDown" :down "j" :down "s" :down
                          "ArrowLeft" :left "h" :left "a" :left
                          "ArrowRight" :right "l" :right "d" :right
                          "Escape" :exit}
      ::game-input-handler handle-input)
    (reset! world_ world)
    (add-watch world_ ::world-changed-watch world-changed-listener)))

(defmethod scene-cleanup ::game-scene
  [_]
  (remove-watch world_ ::world-changed-watch)
  (reset! world_ nil)
  (input/unregister-key-handler ::game-input-handler)
  (pixi/remove-container main-stage))

(defmethod scene-init ::game-scene
  [_scene parent-stage]
  (pixi/load-resources view/resources loaded-callback)
  (pixi/add-child parent-stage main-stage))
