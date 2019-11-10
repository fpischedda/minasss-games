(ns minasss-games.math
  "This namespace collect a bounch of mathematical utilities")

(defn direction
  "Given two vectors a and b, return a new vector that points from a to b"
  [[ax ay] [bx by]]
  [(- ax bx) (- ay by)])

(defn length
  "Given the vector v return its length"
  [[x y]]
  (Math/sqrt (+ (* x x) (* y y))))

(defn normalize
  "Given the vector v return its normalized version"
  [[x y]]
  (let [len (Math/sqrt (+ (* x x) (* y y)))]
    [(/ x len) (/ y len)]))

(defn scale
  "Given the vector v return its scaled version"
  [[x y] scale]
  [(* x scale) (* y scale)])

(defn translate
  "Given the vector v return its translated version"
  [[x y] [tx ty]]
  [(+ x tx) (+ y ty)])
