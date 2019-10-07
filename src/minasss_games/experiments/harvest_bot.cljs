(ns minasss-games.experiments.harvest-bot
  "
  * basic mining game experiment

User controls a bot or creature that can extract some kind of minerals which
in turn must be used (directly or after some kind of refinement) to feed the
creature/bot.
User operate in a small area until all resources are found or the bot/creature
have no more energy to explore and mine.

Resources to be managed:
 - bot energy: decay over time
 - minerals: can be collected and then used to feed the robot

The game should show a view of the area where the bot can operate on, the
amount of energy available to the bot and eventually the amount of minerals
collected by the bot. To keep everything simple the area could be a grid where
each cell can have 0 to MAX-UNITS units of mineral available (easily
representable with a number), the bot could be represented as a circle with
the amount of available energy represented as a number inside the circle.

To win the game the bot must clean the whole area; it the bot finishes the
available energy the game is over.

Controls:
 - wasd, arrows, hjkl

GUI:
 - 3x3, 5x5 grid
 - each tile should show the amount on available energy
 - each tile should show the cost to move there
 - player should show available energy and total collected energy
  "
  (:require
   [minasss-games.experiments.harvest-bot.view :as view]
   [minasss-games.experiments.harvest-bot.game :as game]
   [minasss-games.pixi :as pixi]
   [minasss-games.pixi.input :as input]
   [minasss-games.pixi.settings :as settings]))

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

(defn handle-input [event-type _native direction]
  (if (= :key-up event-type)
    (game/update-world :move-bot direction)))

(defn ^:export loaded-callback []
  (view/setup game/world_ main-stage)
  (input/register-keys {"ArrowUp" :up "k" :up
                        "ArrowDown" :down "j" :down
                        "ArrowLeft" :left "h" :left
                        "ArrowRight" :right "l" :right}
    :bot-handler handle-input)
  (.start (pixi/make-ticker game-tick)))

(defn init [app]
  (settings/set! :scale-mode :nearest)
  (pixi/load-resources view/resources loaded-callback)
  (pixi/add-to-app-stage app main-stage))
