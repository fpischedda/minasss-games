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
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.experiments.awwwliens.core :as core]
            [minasss-games.experiments.awwwliens.view :as view]))

;; main-stage is still in this namespace because in the future
;; the "game" may receive the main stage where to attach its own
;; graphical elements; moving it to the view namespace might mask
;; this possible behavior
(def main-stage (pixi/make-container))

(defn ^:export game-tick
  "Update function called in the game loop;
  parameter delta-time refers to time passed since last update"
  [delta-time]
  (view/update-step delta-time))

(defn handle-input
  [world_ event-type _native direction]
  (if (= :key-up event-type)
    (swap! world_ core/update-world :move-cow direction)))

(defn ^:export loaded-callback []
  (let [world_ (atom (core/make-world {:width 3 :height 1
                                       :cow-row 0 :cow-col 0 :cow-energy 10}))]
    (view/setup world_ main-stage)
    (input/register-keys {"ArrowUp" :up "k" :up "w" :up
                          "ArrowDown" :down "j" :down "s" :down
                          "ArrowLeft" :left "h" :left "a" :left
                          "ArrowRight" :right "l" :right "d" :right}
      ::game-input-handler (partial handle-input world_)))
  (.start (pixi/make-ticker game-tick)))

(defn clean-up
  "Remove input handler and main container"
  []
  (input/unregister-key-handler ::game-input-handler)
  (pixi/remove-container main-stage))

(defn init [parent-stage]
  (pixi/load-resources view/resources loaded-callback)
  (pixi/add-child parent-stage main-stage))
