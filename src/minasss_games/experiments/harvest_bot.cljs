(ns minasss-games.experiments.harvest-bot
  "
***** basic mining

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
  (:require [minasss-games.pixi :as pixi]))

(def resources ["images/background.png" "images/sprite.png"])

(def main-stage (pixi/make-container))

(defn make-bot
  "a bot have a position (expressed in grid coordinates) and some energy"
  [x y energy]
  {:x x :y y :energy energy})

(defn make-cell [row col traversal-cost energy]
  {:row row
   :col col
   :traversal-cost traversal-cost
   :energy energy})

(defn make-area-row
  "creates a area row"
  [row-index width]
  (vec (map #(make-cell row-index % (rand-int 5) (rand-int 10)) (range width))))

(defn make-area
  "create area cells matrix of dimensions width X height"
  [width height]
  (vec (map #(make-area-row % width) (range height))))

(defn make-world
  "A world is a width X height area, where each item is a cell,
  plus a player and a score"
  [width height bot-x bot-y]
  {:player (make-bot bot-x bot-y 10)
   :score 0
   :area (make-area width height)})

(def world (make-world 5 5 2 2))

(defn ^:export loaded-callback []
  (let [background (pixi/make-sprite "images/background.png")
        sprite (pixi/make-sprite "images/sprite.png")]
    (pixi/add-to-stage main-stage background)
    (pixi/add-to-stage main-stage sprite)))

(defn init [app]
  (pixi/load-resources resources loaded-callback)
  (pixi/add-to-app-stage app main-stage))
