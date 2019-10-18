(ns minasss-games.experiments.awwwliens.core)

(defn make-cow
  "a cow have a position (expressed in grid coordinates) and some energy"
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
  (vec (map #(make-cell row % 1 (rand-int 10)) (range width))))

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
  plus a player controlled cow and a score (days-alive)"
  [{:keys [width height cow-row cow-col cow-energy]}]
  {:cow (make-cow cow-row cow-col cow-energy)
   :days-alive 0
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

(defmulti update-world
  (fn [_world command & _payload] command))

(defmethod update-world :move-cow
  [world _ dir]
  (let [old-pos (get-in world [:cow :position])
        new-pos (new-position old-pos dir)]
    (if (valid-position? new-pos world)
      (assoc-in world [:cow :position] new-pos)
      world)))

;; calculate cow energy = energy - cell cost
;; sometimes tile cost can be negative meaning that it is a recharging station
;; harvest, meaning points = points + tile energy, set tile enerty = 0
(defmethod update-world :eat
  [world _]
  (let [{:keys [row col]} (get-in world [:cow :position])
            cell (get-area-tile (:area world) row col)]
        (-> world
          (update-in [:cow :energy] - (:cost cell))
          (update :days-alive inc)
          (assoc-in [:area row col :energy] 0))))

(defn eat
  "just a small convenience wrapper for update-world :harvest"
  [world]
  (update-world world :eat))
