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
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.pixi.settings :as settings]
            [minasss-games.pixi.scene :as scene]))

(def resources ["images/background.png" "images/sprite.png" "images/tile.png"])

(def main-stage (pixi/make-container))

(defn make-bot
  "a bot have a position (expressed in grid coordinates) and some energy"
  [row col energy]
  {:row row :col col :energy energy})

(defn make-cell [row col traversal-cost energy]
  {:row row
   :col col
   :traversal-cost traversal-cost
   :energy energy})

(defn make-area-row
  "creates a area row"
  [row width]
  (vec (map #(make-cell row % (rand-int 5) (rand-int 10)) (range width))))

(defn make-area
  "create area cells matrix of dimensions width X height"
  [width height]
  (vec (map #(make-area-row % width) (range height))))

(defn make-world
  "A world is a width X height area, where each item is a cell,
  plus a player controlled bot and a score"
  [width height bot-row bot-col bot-energy]
  {:bot (make-bot bot-row bot-col bot-energy)
   :score 0
   :area (make-area width height)})

;; make-world parameters are sooo meaningless for the reader...
;; must find a better way!
(def world_ (atom (make-world 5 5 0 0 10)))

(defn make-tile
  [{:keys [row col energy traversal-cost]}]
  (let [container (scene/render
             [:container {:position [(* 64 col) (* 64 row)]}
              [:sprite {:texture "images/tile.png"
                        :scale [2 2]}]
              [:text {:text (str energy)
                      :position [64 0]
                      :anchor [1 0]
                      :style {"fill" "#62f479" "fontSize" 16}
                      :name "energy"}]
              [:text {:text (str traversal-cost)
                      :position [64 64]
                      :anchor [1 1]
                      :style {"fill" "#ce4b17" "fontSize" 16}
                      :name "cost"}]])]
    {:view container
     :entities {:energy (pixi/get-child-by-name container "energy")
                :cost (pixi/get-child-by-name container "cost")}}))

(defn make-area-view [area]
  (let [area-container (pixi/make-container)
        tiles (vec (map (fn [row] (vec (map #(pixi/add-child-view area-container (make-tile %)) row))) area))]
    (pixi/set-position area-container 200 200)
    {:view area-container
     :entities {:tiles tiles}}))

(defn make-score-view [initial-score]
  (let [score-container (pixi/make-container)
        text-style (pixi/make-text-style {:fill  "#cf2323"})
        text (pixi/make-text (str "Score " initial-score) text-style)]
    (pixi/add-child score-container text)
    {:view score-container
     :entities {:text text}}))

(defn make-bot-view [bot]
  (let [container (scene/render
                    [:container {:position [64 64]}
                     [:sprite {:texture "images/sprite.png"
                               :scale [2 2]
                               :name "bot"}]
                     [:text {:text (:energy bot)
                             :anchor [0 0]
                             :position [0 0]
                             :style {"fill" "#d751c9" "fontSize" 16}
                             :name "energy"}]
                     ])]
    {:view container
     :entities {:text (pixi/get-child-by-name container "energy")
                :bot (pixi/get-child-by-name container "bot")}}))

(defn make-world-view
  "A world is a width X height area, where each item is a cell,
  plus a player and a score, the view is composed as follows:
  - the world is represented as a tilemap, in each tile shows the cost and
  available energy;
  - the bot is just a sprite container showing bot sprite and available energy;
  - score is just a text somewhere"
  [world]
  {:bot (make-bot-view (:bot world))
   :score (make-score-view (:score world))
   :area (make-area-view (:area world))})

(defonce ^:private world-view_ (atom {}))

(defonce ^:private time_ (atom 0.0))

(defn ^:export game-tick
  "Update function called in the game loop
  parameter delta-time refers to time passed from last update"
  [delta-time]
  (let [next-pos (get-in @world_ [:bot :next-pos])
        bot-view (get-in @world-view_ [:bot :view])]
    (when next-pos
      (pixi/set-position bot-view (* 64 (:x next-pos)) (* 64 (:y next-pos)))
      (swap! world_ update :bot dissoc :next-pos))))

(defn ^:export loaded-callback []
  (let [background (pixi/make-sprite "images/background.png")
        view (make-world-view @world_)
        {:keys [area bot score]} view]
    (pixi/add-child main-stage background)
    (pixi/add-child-view (:view area) bot)
    (pixi/add-children-view main-stage [area score])
    (reset! world-view_ view)
    (.start (pixi/make-ticker game-tick))))

(defn init [app]
  (settings/set! :scale-mode :nearest)
  (pixi/load-resources resources loaded-callback)
  (pixi/add-to-app-stage app main-stage))
