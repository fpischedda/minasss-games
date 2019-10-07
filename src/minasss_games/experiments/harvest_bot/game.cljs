(ns minasss-games.experiments.harvest-bot.game)

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
  (fn [command & _payload] command))

(defmethod update-world :move-bot
  [_ dir]
  (move-bot dir))

;; calculate bot energy = energy - cell cost
;; sometimes tile cost can be negative meaning that it is a recharging station
;; harvest, meaning points = points + tile energy, set tile enerty = 0
(defmethod update-world :harvest
  [_]
  (swap! world_
    (fn [world]
      (let [[row col] (get-in world [:bot :position])
            cell (get-area-tile (:area world) row col)]
        (-> world
          (update-in [:bot :energy] - (:cost cell))
          (update :score - (:energy cell))
          (assoc-in [:bot :area row col :energy] 0))))))

(defn harvest
  "just a small convenience wrapper for update-world :harvest"
  []
  (update-world :harvest))
