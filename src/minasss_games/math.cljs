(ns minasss-games.math
  "This namespace collect a bounch of mathematical utilities")

(defn direction
  "Given two vectors a and b, return a new vector that points from a to b"
  [a b]
  (let [dx (- (:x a) (:x b))
        dy (- (:y a) (:y b))]
    {:x dx :y dy}))

(defn length
  "Given the vector v return its length"
  [v]
  (let [x (:x v)
        y (:y v)]
    (Math/sqrt (+ (* x x) (* y y)))))

(defn normalize
  "Given the vector v return its normalized version"
  [v]
  (let [x (:x v)
        y (:y v)
        len (Math/sqrt (+ (* x x) (* y y)))]
    {:x (/ x len) :y (/ y len)}))
