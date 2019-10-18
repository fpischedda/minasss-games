(ns minasss-games.experiments.awwwliens.view
  (:require [minasss-games.director :as director]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.tween :as tween]
            [minasss-games.experiments.awwwliens.core :as core]))

(def resources ["images/awwwliens/background.png"
                "images/awwwliens/cow.png" "images/tile.png" "images/gem.png"])

(defonce world-view_ (atom {}))

(def cell-size 128)

(defn update-step
  "update view related stuff"
  [delta-time]
  (tween/update-tweens delta-time))

(defn update-cow-energy
  "helper function that update the text representing cow energy"
  [energy]
  (pixi/set-text (get-in @world-view_ [:cow :entities :text]) energy))

(defn handle-cow-changed
  [world_ old-cow new-cow]
  (let [old-pos (:position old-cow)
        new-pos (:position new-cow)
        old-energy (:energy old-cow)
        new-energy (:energy new-cow)]

    ;; position changed detection, must find a better way...
    (when (not (= old-pos new-pos))
      (let [old-x (* cell-size (:col old-pos))
            old-y (* cell-size (:row old-pos))
            x (* cell-size (:col new-pos))
            y (* cell-size (:row new-pos))]
        (tween/move-to {:target (get-in @world-view_ [:cow :view])
                        :starting-position {:x old-x :y old-y}
                        :target-position {:x x :y y}
                        :speed 1.5
                        :on-complete (fn [] (swap! world_ core/eat))})))

    ;; eventually update cow energy text
    (when (not (= old-energy new-energy))
      (if (>= 0 new-energy)
        (director/start-scene minasss-games.experiments.awwwliens.intro/scene)
        (update-cow-energy new-energy)))))

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
  "listens to changes of cow game entity, updates
  graphical object accordingly"
  [_key world_ old-state new-state]
  (let [old-cow (:cow old-state)
        new-cow (:cow new-state)
        old-score (:score old-state)
        new-score (:score new-state)]
    (when (not (= old-cow new-cow))
      (handle-cow-changed world_ old-cow new-cow))
    (when (not (= old-score new-score))
      (update-score-text new-score))
    (when-let [cell (detect-changed-cell (:area old-state) (:area new-state))]
      (update-cell cell))))

(defn make-cell
  [{:keys [row col energy cost]}]
  (let [container (scene/render
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

(defn make-cow-view [cow]
  (let [container
        (scene/render
          [:container {:position [(* cell-size (get-in cow [:position :col]))
                                  (* cell-size (get-in cow [:position :row]))]}
           [:sprite {:texture "images/awwwliens/cow.png"
                     :anchor [0 -0.5]
                     :name "cow"}]
           [:text {:text (:energy cow)
                   :anchor [0 0]
                   :position [0 0]
                   :style {"fill" "#d751c9" "fontSize" 20}
                   :name "energy"}]
           ])]
    {:view container
     :entities {:text (pixi/get-child-by-name container "energy")
                :cow (pixi/get-child-by-name container "cow")}}))

(defn make-world-view
  "A world is a width X height area, where each item is a cell,
  plus a player and a score, the view is composed as follows:
  - the world is represented as a tilemap, in each tile shows the cost and
  available energy;
  - the cow is just a sprite container showing cow sprite and available energy;
  - score is just a text somewhere"
  [world]
  {:cow (make-cow-view (:cow world))
   :score (make-score-view (:score world))
   :area (make-area-view (:area world))})

(defn setup
  "setup the view based on the world_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [world_ main-stage]
  (let [background (pixi/make-sprite "images/awwwliens/background.png")
        view (make-world-view @world_)
        {:keys [area cow score]} view]
    (pixi/add-child main-stage background)
    (pixi/add-child-view (:view area) cow)
    (pixi/add-children-view main-stage [area score])
    (reset! world-view_ view)
    (add-watch world_ ::world-changed-watch world-changed-listener)))
