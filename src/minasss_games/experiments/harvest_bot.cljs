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
  (:require [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.pixi.settings :as settings]
            [minasss-games.pixi.scene :as scene]))

(def resources ["images/background.png" "images/sprite.png" "images/tile.png"])

(def main-stage (pixi/make-container))

(defn make-bot
  "a bot have a position (expressed in grid coordinates) and some energy"
  [row col energy]
  {:position {:row row :col col} :energy energy})

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
                    [:container {:position [(* 64 (get-in bot [:position :col]))
                                            (* 64 (get-in bot [:position :row]))]}
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

(defn make-move-to
  [container old-pos target-pos]
  (let [dir (math/direction target-pos old-pos)
        normalized-dir (math/normalize dir)
        calculated-pos (atom old-pos)
        prev-distance (atom (math/length dir))]
    (fn [delta-time]
      (swap! calculated-pos (fn [pos]
                              {:x (+ (:x pos) (* delta-time (:x normalized-dir)))
                               :y (+ (:y pos) (* delta-time (:y normalized-dir)))}))
      (let [distance (math/length (math/direction target-pos @calculated-pos))]
        (if (> distance @prev-distance)
          (do
            (pixi/set-position container (:x target-pos) (:y target-pos))
            false)
          (do
            (reset! prev-distance distance)
            (pixi/set-position container (:x @calculated-pos) (:y @calculated-pos))
            true))))))

(def view-updater-functions_ (atom (list)))

(defn add-view-updater-function
  [f]
  (swap! view-updater-functions_ conj f))

(defn bot-changed-listener
  "listens to changes of bot game entity, updates
  graphical object accordingly"
  [_key _ref old-value new-value]
  (let [old-pos (get-in old-value [:bot :position])
        new-pos (get-in new-value [:bot :position])]
    ;; position changed detection, must find a better way...
    (when (not (= old-pos new-pos))
      (let [old-x (* 64 (:col old-pos))
            old-y (* 64 (:row old-pos))
            x (* 64 (:col new-pos))
            y (* 64 (:row new-pos))]
        (add-view-updater-function (make-move-to (get-in @world-view_ [:bot :view]) {:x old-x :y old-y} {:x x :y y}))))))

(defn ^:export game-tick
  "Update function called in the game loop;
  parameter delta-time refers to time passed since last update"
  [delta-time]
  (let [next-pos (get-in @world_ [:bot :next-pos])
        bot-view (get-in @world-view_ [:bot :view])]
    (swap! view-updater-functions_ (fn [functions]
                                     (into () (filter (fn [f] (f delta-time)) functions))))))

(defn setup []
  (let [background (pixi/make-sprite "images/background.png")
        view (make-world-view @world_)
        {:keys [area bot score]} view]
    (pixi/add-child main-stage background)
    (pixi/add-child-view (:view area) bot)
    (pixi/add-children-view main-stage [area score])
    (reset! world-view_ view)
    (add-watch world_ :bot-handler bot-changed-listener)
    (input/register-keys {"ArrowUp" :up "k" :up
                          "ArrowDown" :down "j" :down
                          "ArrowLeft" :left "h" :left
                          "ArrowRight" :right "l" :right}
      :bot-handler
      (fn [event-type _native translated]
        (if (= :key-up event-type)
          (move-bot translated))))
    (.start (pixi/make-ticker game-tick))))

(defn ^:export loaded-callback []
  (setup))

(defn init [app]
  (settings/set! :scale-mode :nearest)
  (pixi/load-resources resources loaded-callback)
  (pixi/add-to-app-stage app main-stage))
