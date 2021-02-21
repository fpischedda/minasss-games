(ns minasss-games.collision
  "Utility functions for (simple, bounding rect based) collision detection")

(defn make-rect-bounds
  "Make a bounding rect based on provided position and rect size; rect is
  centered around position"
  [[x y] [width height]]
  (let [hw (/ width 2)
        hh (/ height 2)]
    [(- x hw) (- y hh) (+ x hw) (+ y hh)]))

(defn rects-overlap
  "Returns true if the provided rects overlaps"
  [[ax-top ay-top ax-bottom ay-bottom] [bx-top by-top bx-bottom by-bottom]]
  (and (< ax-top bx-bottom) (< ay-top by-bottom) (> ax-bottom bx-top) (> ay-bottom by-top)))

(comment
  (make-rect-bounds [100 100] [10 10])
  (make-rect-bounds [105 105] [10 10])

  (rects-overlap
    (make-rect-bounds [100 100] [10 10])
    (make-rect-bounds [125 125] [10 10])
    )
  )
