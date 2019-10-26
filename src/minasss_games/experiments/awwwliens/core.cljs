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
   :energy (rand-int 5)
   :fertilizer 0})

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

(def poison-probability 0.2)

(defn update-cell
  "Update cell with the following logic:
  - dec fertilizer, capping it to 0
  - increase energy by 1 if no fertilizer is present, by 2 if it is
  - if energy > 4 it turns to compost increasing fertilizer by one and turning its energy to -2
  - if energy > 3 starts rotting meaning it counts as 1 when computing food"
  [{:keys [energy fertilizer] :as cell}]
  (let [temp-fertilizer (max 0 (dec fertilizer))
        temp-energy (+ energy (inc (min 1 temp-fertilizer))) ;; energy += if fertilizer > 0 then 2 else 1 :D
        new-energy (if (> temp-energy 4) -2 temp-energy)
        new-fertilizer (if (> temp-energy 4) (inc temp-fertilizer) temp-fertilizer)
        poison (> (* 2 poison-probability) (rand))]
    (assoc cell :fertilizer new-fertilizer :energy new-energy :poison poison)))

(defn update-area
  "Update cells with the following logic:
  - dec fertilizer, capping it to 0
  - increase energy by 1 if no fertilizer is present, by 2 if it is
  - if energy > 4 it turns to compost increasing fertilizer by one and turning its energy to -1
  - if energy > 3 starts rotting meaning it counts as 1 when computing food"
  [area]
  (mapv #(mapv update-cell %) area))

(defn cell-food
  "Return the amount of food that the cell can give to the cow
  there different cases:
  - energy 3 then 2, top of the form of the plant
  - energy = 1 or energy > 2 then 1
  - else 0
  if poison is true return - cell-food"
  [{:keys [energy poison]}]
  (let [poison-multiplier (if poison -1 1)]
    (* poison-multiplier (cond
                           (>= 1 energy) 0
                           (= 3 energy) 2
                           :else 1))))

(def cow-max-energy 10)

(defn eat
  "Calculate cow energy = energy + cell-food - cell cost
  BUT not bigger than cow-max-energy
  remove plant from cell (setting energy to 0)
  increase days alive
  calculate poison flag"
  [world]
  (let [{:keys [row col]} (get-in world [:cow :position])
        cell (get-area-tile (:area world) row col)
        food (cell-food cell)
        hungry (if (and (= 0 food) (= 0 (get-in world [:cow :prev-food]))) -1 0)]
        (-> world
          (update-in [:cow :energy] #(min cow-max-energy (+ % food hungry)))
          (assoc-in [:cow :prev-food] food)
          (update-in [:days-alive] inc)
          (assoc-in [:area row col :energy] -1) ;; it will increase in the grow step anyway
          (update-in [:area row col] assoc :poison (> poison-probability (rand))))))

(defn grow
  "Update cells meaning: grow plants"
  [world]
  (update-in world [:area] update-area))
