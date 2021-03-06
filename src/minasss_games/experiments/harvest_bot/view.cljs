(ns minasss-games.experiments.harvest-bot.view
  "this namespace collects all view related functions"
  (:require [minasss-games.element :as element]
            [minasss-games.experiments.harvest-bot.game :as game]
            [minasss-games.pixi :as pixi]
            [minasss-games.tween :as tween]))

(def resources ["images/background.png" "images/sprite.png" "images/tile.png" "images/gem.png"])

(defonce world-view_ (atom {}))

(def cell-size 128)

(defn update-bot-energy
  "helper function that update the text representing bot energy"
  [energy]
  (pixi/set-text (get-in @world-view_ [:bot :entities :text]) energy))

(defn handle-bot-changed
  [old-bot new-bot]
  (let [old-pos (:position old-bot)
        new-pos (:position new-bot)
        old-energy (:energy old-bot)
        new-energy (:energy new-bot)]

    ;; position changed detection, must find a better way...
    (when (not (= old-pos new-pos))
      (let [old-x (* cell-size (:col old-pos))
            old-y (* cell-size (:row old-pos))
            x (* cell-size (:col new-pos))
            y (* cell-size (:row new-pos))]
        (tween/move-to {:target (get-in @world-view_ [:bot :view])
                        :starting-position {:x old-x :y old-y}
                        :target-position {:x x :y y}
                        :speed 1.5
                        :on-complete game/harvest})))

    ;; eventually update bot energy text
    (when (not (= old-energy new-energy))
      (update-bot-energy new-energy))))

(defn update-score-text
  "helper function to update score text"
  [score]
  (pixi/set-text (get-in @world-view_ [:score :entities :text]) (str "Score " score)))

(defn detect-changed-cell
  "given the old and new states of the area return the cell that has changed"
  [old-area new-area]
  (->> (map vector (flatten new-area) (flatten old-area))
    (filter (fn [[new old]] (not (= new old))))
    first
    first))

(defn update-cell
  "helper function to update a cell view"
  [cell]
  (let [{:keys [row col energy]} cell
        cell-view (get-in @world-view_ [:area :entities :cells row col :view])
        energy-text (pixi/get-child-by-name cell-view "energy")]
    (pixi/remove-child-by-name cell-view "gem")
    (pixi/set-text energy-text energy)))

(defn world-changed-listener
  "listens to changes of bot game entity, updates
  graphical object accordingly"
  [_key _ref old-state new-state]
  (let [old-bot (:bot old-state)
        new-bot (:bot new-state)
        old-score (:score old-state)
        new-score (:score new-state)]
    (when (not (= old-bot new-bot))
      (handle-bot-changed old-bot new-bot))
    (when (not (= old-score new-score))
      (update-score-text new-score))
    (when-let [cell (detect-changed-cell (:area old-state) (:area new-state))]
      (update-cell cell))))

(defn make-cell
  [{:keys [row col energy cost]}]
  (let [container (element/render
                    [:container {:position [(* cell-size col) (* cell-size row)]}
                     [:sprite {:texture "images/tile.png"
                               :scale [4 4]}]
                     [:text {:text energy
                             :position [cell-size 0]
                             :anchor [1 0]
                             :style {"fill" "#62f479" "fontSize" 20}
                             :name "energy"}]
                     [:text {:text cost
                             :position [cell-size cell-size]
                             :anchor [1 1]
                             :style {"fill" "#ce4b17" "fontSize" 20}
                             :name "cost"}]
                     [:sprite {:texture "images/gem.png"
                               :name "gem"
                               :anchor [0.5 0.5]
                               :position [(/ cell-size 2) (/ cell-size 2)]}]])]
    {:view container
     :entities {:energy (pixi/get-child-by-name container "energy")
                :cost (pixi/get-child-by-name container "cost")}}))

(defn make-area-view [area]
  (let [area-container (pixi/make-container)
        cells (vec (map
                     (fn [row]
                       (vec (map (fn [elem]
                                   (let [cell (make-cell elem)]
                                     (pixi/add-child-view area-container cell)
                                     cell)) row)))
                     area))]
    (pixi/set-position area-container 300 200)
    {:view area-container
     :entities {:cells cells}}))

(defn make-score-view [initial-score]
  (let [score-container (pixi/make-container)
        text-style (pixi/make-text-style {:fill  "#cf2323"})
        text (pixi/make-text (str "Score " initial-score) text-style)]
    (pixi/add-child score-container text)
    {:view score-container
     :entities {:text text}}))

(defn make-bot-view [bot]
  (let [container (element/render
                    [:container {:position [(* cell-size (get-in bot [:position :col]))
                                            (* cell-size (get-in bot [:position :row]))]}
                     [:sprite {:texture "images/sprite.png"
                               :scale [4 4]
                               :name "bot"}]
                     [:text {:text (:energy bot)
                             :anchor [0 0]
                             :position [0 0]
                             :style {"fill" "#d751c9" "fontSize" 20}
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

(defn setup
  "setup the view based on the world_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [world_ main-stage]
  (let [background (pixi/make-sprite "images/background.png")
        view (make-world-view @world_)
        {:keys [area bot score]} view]
    (pixi/add-child main-stage background)
    (pixi/add-child-view (:view area) bot)
    (pixi/add-children-view main-stage [area score])
    (reset! world-view_ view)
    (add-watch world_ :world-changed-watch world-changed-listener)))
