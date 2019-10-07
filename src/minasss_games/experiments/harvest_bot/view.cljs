(ns minasss-games.experiments.harvest-bot.view
  "this namespace collects all view related functions"
  (:require [minasss-games.math :as math]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.experiments.harvest-bot.game :as game]))

(def resources ["images/background.png" "images/sprite.png" "images/tile.png"])

(defonce world-view_ (atom {}))

(def view-updater-functions_ (atom (list)))

(defn update-step
  "update view related stuff"
  [delta-time]
  (swap! view-updater-functions_
    (fn [functions]
      (into () (filter (fn [f] (f delta-time)) functions)))))

(defn add-view-updater-function
  [f]
  (swap! view-updater-functions_ conj f))

(defn make-move-to
  [container old-pos target-pos completed-fn]
  (let [dir (math/direction target-pos old-pos)
        normalized-dir (math/normalize dir)
        calculated-pos (volatile! old-pos)
        prev-distance (volatile! (math/length dir))]
    (fn [delta-time]
      (vswap! calculated-pos (fn [pos]
                              {:x (+ (:x pos) (* delta-time (:x normalized-dir)))
                               :y (+ (:y pos) (* delta-time (:y normalized-dir)))}))
      (let [distance (math/length (math/direction target-pos @calculated-pos))]
        (if (> distance @prev-distance)
          (do
            (pixi/set-position container (:x target-pos) (:y target-pos))
            (when (some? completed-fn)
              (completed-fn))
            false)
          (do
            (vreset! prev-distance distance)
            (pixi/set-position container (:x @calculated-pos) (:y @calculated-pos))
            true))))))

(defn handle-bot-changed
  [old-bot new-bot]
  (let [old-pos (:position old-bot)
        new-pos (:position new-bot)
        old-energy (:energy old-bot)
        new-energy (:energy new-bot)]
    ;; position changed detection, must find a better way...
    (cond
      (not (= old-pos new-pos))
      (let [old-x (* 64 (:col old-pos))
            old-y (* 64 (:row old-pos))
            x (* 64 (:col new-pos))
            y (* 64 (:row new-pos))]
        (add-view-updater-function
          (make-move-to (get-in @world-view_ [:bot :view]) {:x old-x :y old-y} {:x x :y y} game/harvest)))
      (not (= old-energy new-energy))
      (pixi/set-text (get-in @world-view_ [:bot :entities :text]) new-energy))))

(defn bot-changed-listener
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
      (pixi/set-text (get-in @world-view_ [:score :entities :text]) (str "Score " new-score)))))

(defn make-tile
  [{:keys [row col energy cost]}]
  (let [container (scene/render
                    [:container {:position [(* 64 col) (* 64 row)]}
                     [:sprite {:texture "images/tile.png"
                               :scale [2 2]}]
                     [:text {:text energy
                             :position [64 0]
                             :anchor [1 0]
                             :style {"fill" "#62f479" "fontSize" 16}
                             :name "energy"}]
                     [:text {:text cost
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
    (add-watch world_ :bot-handler bot-changed-listener)))
