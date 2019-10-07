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
   [minasss-games.pixi :as pixi]
   [minasss-games.pixi.input :as input]
   [minasss-games.pixi.settings :as settings]))

;; main-stage is still in this namespace because in the future
;; the "game" may receive the main stage where to attach its own
;; graphical elements; moving it to the view namespace might mask
;; this possible behavior
(def main-stage (pixi/make-container))

(defn make-bot
  "a bot have a position (expressed in grid coordinates) and some energy"
  [row col energy]
  {:position {:row row :col col} :energy energy})

(defn make-cell [row col cost energy]
  {:row row
   :col col
   :cost cost
   :energy energy})

(defn make-area-row
  "creates a area row"
  [row width]
  (vec (map #(make-cell row % (rand-int 5) (rand-int 10)) (range width))))

(defn make-area
  "create area cells matrix of dimensions width X height"
  [width height]
  (vec (map #(make-area-row % width) (range height))))

(defn get-area-tile
  "return the tile at the specified coordinates"
  [area row col]
  (get-in area [row col]))

(defn make-world
  "A world is a width X height area, where each item is a cell,
  plus a player controlled bot and a score"
  [{:keys [width height bot-row bot-col bot-energy]}]
  {:bot (make-bot bot-row bot-col bot-energy)
   :score 0
   :width width
   :height height
   :area (make-area width height)})

(defn new-position
  "calculate new position based on old one and provided direction,
  acceptable values for dir are :left, :right, :up and :down,
  with any other value for dir the old position will be returned."
  [position dir]
  (let [row (:row position)
        col (:col position)]
    (condp = dir
      :left {:row row :col (dec col)}
      :right {:row row :col (inc col)}
      :up {:row (dec row) :col col}
      :down {:row (inc row) :col col}
      {:row row :col col})))

(defn valid-position?
  "return true if the provided position is valid, meaning it is inside
  the area grid"
  [{:keys [row col]} {:keys [width height]}]
  (and (>= row 0) (< row height) (>= col 0) (< col width)))

;; make-world parameters are sooo meaningless for the reader...
;; must find a better way!
;; ok, a map might be a bit better
(def world_ (atom (make-world {:width 5 :height 5
                               :bot-row 0 :bot-col 0 :bot-energy 10})))

;; this still depends on the global world_ var,
;; must find a way to get rid of it
(defn move-bot
  [dir]
  (swap! world_ (fn [world]
                  (let [new-pos (new-position (get-in world [:bot :position]) dir)]
                    (if (valid-position? new-pos world)
                      (assoc-in world [:bot :position] new-pos)
                      world)))))

(defmulti update-world
  [command & _payload] command)

(defmethod update-world :move-bot
  [_ [dir]]
  (move-bot dir))

;; calculate bot energy = energy - tile cost
;; sometimes tile cost can be negative meaning that it is a recharging station
;; harvest, meaning points = points + tile energy, set tile enerty = 0
(defmethod update-world :harvest
  [_ _]
  (swap! world_
    (fn [world]
      (let [[row col] (get-in world [:bot :position])
            tile (get-area-tile (:area world) row col)])
      (-> world
        (update-in [:bot :energy] - (:cost tile))
        (update :score - (:energy tile))
        (assoc-in [:bot :area row col] 0)))))

(defn ^:export game-tick
  "Update function called in the game loop;
  parameter delta-time refers to time passed since last update"
  [delta-time]
  (view/update-step delta-time))

(defn handle-input [event-type _ direction]
  (if (= :key-up event-type)
    (update-world :move-bot direction)))

(defn ^:export loaded-callback []
  (view/setup world_ main-stage)
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
