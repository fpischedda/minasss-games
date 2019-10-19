(ns minasss-games.experiments.awwwliens.core)

(defn make-cow
  "A cow have a position (expressed in grid coordinates) and some energy"
  [row col energy]
  {:position {:row row :col col} :energy energy})

(defn make-random-cell
  "Create a cell randomizing cost, energy and fertilizer"
  [row col]
  {:row row
   :col col
   :cost 1
   :energy (inc (rand-int 2))
   :fertilizer (rand-int 2)})

(defn make-area-row
  "Create a area row"
  [row width]
  (vec (map #(make-random-cell row %) (range width))))

(defn make-area
  "Create area cells matrix of dimensions width X height"
  [width height]
  (vec (map #(make-area-row % width) (range height))))

(defn get-area-tile
  "Return the tile at the specified coordinates"
  [area row col]
  (get-in area [row col]))

(defn make-world
  "A world is a width X height area, where each item is a cell,
  plus a player controlled cow and a score (days-alive)"
  [{:keys [width height cow-row cow-col cow-energy]}]
  {:cow (make-cow cow-row cow-col cow-energy)
   :days-alive 0
   :width width
   :height height
   :area (make-area width height)})

(defn new-position
  "Calculate new position based on old one and provided direction,
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

(defn move-cow
  [world dir]
  (let [old-pos (get-in world [:cow :position])
        new-pos (new-position old-pos dir)]
    (if (valid-position? new-pos world)
      (assoc-in world [:cow :position] new-pos)
      world)))

(defn update-cell
  "Update cell with the following logic:
  - dec fertilizer, capping it to 0
  - increase energy by 1 if no fertilizer is present, by 2 if it is
  - if energy > 5 starts rotting
  - if energy > 6 it turns to compost increasing fertilizer by one and turning its energy to -1"
  [{:keys [energy fertilizer] :as cell}]
  (let [temp-fertilizer (max 0 (dec fertilizer))
        temp-energy (+ energy (inc (min 1 temp-fertilizer))) ;; energy += if fertilizer > 0 then 2 else 1 :D
        new-energy (if (> temp-energy 6) -1 temp-energy)
        new-fertilizer (if (> temp-energy 6) (inc temp-fertilizer) temp-fertilizer)]
    (assoc cell :manure new-fertilizer :energy new-energy)))

(defn update-area
  "Update cell with the following logic:
  - dec fertilizer, capping it to 0
  - increase energy by 1 if no fertilizer is present, by 2 if it is
  - if energy > 5 starts rotting
  - if energy > 6 it turns to compost increasing fertilizer by one and turning its energy to -1"
  [area]
  (into []
    (map #(into [] (map update-cell %)) area)))

(defn cell-food
  "Return the amount of food that the cell can give to the cow
  there different cases:
  - poison? then -1
  - energy <= 0 then 0
  - energy = 1 or energy > 3 then 1
  - else 2"
  [{:keys [energy poison]}]
  (cond
    poison -1
    (or (= 1 energy) (> energy 3)) 1
    :else 2))

(defn eat
  "Calculate cow energy = cell-food - cell cost
  remove plant from cell (setting energy to 0)
  increase days alive
  eventually remove poison flag"
  [world]
  (let [{:keys [row col]} (get-in world [:cow :position])
            cell (get-area-tile (:area world) row col)]
        (-> world
          (update-in [:cow :energy] + (- (cell-food cell) (:cost cell)))
          (update :days-alive inc)
          (assoc-in [:area row col :energy] 0)
          (update-in [:area row col] dissoc :poison))))

(defn grow
  "Update cells meaning: grow plants"
  [world]
  (update :area update-area))
