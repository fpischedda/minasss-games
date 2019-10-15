(ns minasss-games.experiments.awwwliens.game)

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
  plus a player controlled cow and a score"
  [{:keys [width height cow-row cow-col cow-energy]}]
  {:cow (make-cow cow-row cow-col cow-energy)
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
(def world_ (atom (make-world {:width 2 :height 1
                               :cow-row 0 :cow-col 0 :cow-energy 10})))

;; this still depends on the global world_ var,
;; must find a way to get rid of it
(defn move-cow
  [world_ dir]
  (swap! world_ (fn [world]
                  (let [old-pos (get-in world [:cow :position])
                        new-pos (new-position old-pos dir)]
                    (if (valid-position? new-pos world)
                      (assoc-in world [:cow :position] new-pos)
                      world)))))

(defmulti update-world
  (fn [command _world_ & _payload] command))

(defmethod update-world :move-cow
  [_ world_ dir]
  (move-cow world_ dir))

;; calculate cow energy = energy - cell cost
;; sometimes tile cost can be negative meaning that it is a recharging station
;; harvest, meaning points = points + tile energy, set tile enerty = 0
(defmethod update-world :harvest
  [_ world_]
  (swap! world_
    (fn [world]
      (let [{:keys [row col]} (get-in world [:cow :position])
            cell (get-area-tile (:area world) row col)]
        (-> world
          (update-in [:cow :energy] - (:cost cell))
          (update :score + (:energy cell))
          (assoc-in [:area row col :energy] 0))))))

(defn harvest
  "just a small convenience wrapper for update-world :harvest"
  [world_]
  (update-world world_ :harvest))
