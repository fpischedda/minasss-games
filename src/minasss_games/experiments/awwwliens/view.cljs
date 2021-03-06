(ns minasss-games.experiments.awwwliens.view
  (:require [minasss-games.director :as director]
            [minasss-games.element :as element]
            [minasss-games.experiments.awwwliens.core :as core]
            [minasss-games.pixi :as pixi]
            [minasss-games.tween :as tween]))

(def resources ["images/awwwliens/game/background.png"
                "images/awwwliens/cow.png"
                "images/awwwliens/game/tile.png"
                "images/awwwliens/plants/plants.json"])

(defonce world-view_ (atom {}))

(def cell-height 32)
(def cell-width 128)

(def plant-textures ["plant-1.png"
                     "plant-2.png"
                     "plant-3.png"
                     "plant-4.png"
                     "plant-5.png"])

(def plant-poison-textures ["plant-poison-1.png"
                            "plant-poison-2.png"
                            "plant-poison-3.png"
                            "plant-poison-4.png"
                            "plant-poison-5.png"])

(defn get-plant-texture-name
  "Return the texture name base on plant energy"
  [energy poison?]
  (if (<= energy 0)
    "plant-1.png"
    (if poison?
            (nth plant-poison-textures energy)
            (nth plant-textures energy))))

(defn update-cow-energy
  "helper function that update the text representing cow energy"
  [energy]
  (pixi/set-text (get-in @world-view_ [:cow :entities :text]) energy))

(defn to-world-position
  [{:keys [col row]}]
  [(* cell-width col) (* cell-height row)])

(defn move-cow
  [from-pos to-pos on-complete-fn]
  (tween/move-to {:target (get-in @world-view_ [:cow :view])
                  :starting-position (to-world-position from-pos)
                  :target-position (to-world-position to-pos)
                  :speed 100
                  :on-complete on-complete-fn}))

(defn update-score-text
  "helper function to update score text"
  [score]
  (pixi/set-text (get-in @world-view_ [:score :entities :text]) (str "Days Alive " score)))

(defn get-food-text
  "return the text content for the food quantity of the cell
  it forces the string +N for N > 0"
  [cell]
  (let [food (core/cell-food cell)]
    (if (> food 0) (str "+" food) food)))

(defn update-cell
  "helper function to update a cell view"
  [{:keys [row col energy poison] :as cell}]
  (let [cell-view (get-in @world-view_ [:area :entities :cells row col :view])
        food-text (pixi/get-child-by-name cell-view "food")
        plant (pixi/get-child-by-name cell-view "plant")
        plant-texture (pixi/get-spritesheet-texture
                        "images/awwwliens/plants/plants.json"
                        (get-plant-texture-name energy poison))]
    (pixi/set-texture plant plant-texture)
    (pixi/set-attributes food-text {:visible (> energy 1)
                                    :text (get-food-text cell)})))

(defn make-cell
  [{:keys [row col energy poison] :as cell}]
  (let [container (element/render
                    [:container {:position [(* cell-width col) (* cell-height row)]}
                     [:sprite {:texture "images/awwwliens/game/tile.png"
                               :anchor [0 1]
                               :position [0 cell-height]}]
                     [:text {:text (get-food-text cell)
                             :position [cell-width 0]
                             :anchor [1 0]
                             :visible (> energy 1)
                             :style {"fill" "#62f479" "fontSize" 20}
                             :name "food"}]
                     [:sprite {:texture {:spritesheet "images/awwwliens/plants/plants.json"
                                         :texture (get-plant-texture-name energy poison)}
                               :name "plant"
                               :anchor [0.5 1.0]
                               :position [16 cell-height]}]])]
    {:view container
     :entities {:energy (pixi/get-child-by-name container "food")
                :plant (pixi/get-child-by-name container "plant")}}))

(defn make-area-view [area]
  (let [area-container (pixi/make-container)
        cells (vec (map
                     (fn [row]
                       (vec (map (fn [elem]
                                   (let [cell (make-cell elem)]
                                     (pixi/add-child-view area-container cell)
                                     cell)) row)))
                     area))]
    (pixi/set-position area-container 150 290)
    {:view area-container
     :entities {:cells cells}}))

(defn make-score-view [initial-score]
  (let [score-container (pixi/make-container)
        text-style (pixi/make-text-style {:fill  "#cf2323"})
        text (pixi/make-text (str "Days Alive " initial-score) text-style)]
    (pixi/add-child score-container text)
    {:view score-container
     :entities {:text text}}))

(defn make-cow-view [cow]
  (let [container
        (element/render
          [:container {:position [(* cell-width (get-in cow [:position :col]))
                                  (* cell-height (get-in cow [:position :row]))]}
           [:sprite {:texture "images/awwwliens/cow.png"
                     :name "cow"
                     :position [0 cell-height]
                     :anchor [0 1.0]}]
           [:text {:text (:energy cow)
                   :position [62 -36]
                   :style {"fill" "#d751c9" "fontSize" 19}
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
   :score (make-score-view (:days-alive world))
   :area (make-area-view (:area world))})

(defn setup
  "setup the view based on the provided world; main-stage refers to the
  root container, where other graphical elements will be added"
  [world main-stage]
  (let [background (pixi/make-sprite "images/awwwliens/game/background.png")
        view (make-world-view world)
        {:keys [area cow score]} view]
    (pixi/add-child main-stage background)
    (pixi/add-child-view (:view area) cow)
    (pixi/add-children-view main-stage [area score])
    (reset! world-view_ view)))
