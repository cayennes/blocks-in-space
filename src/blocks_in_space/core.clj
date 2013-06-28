(ns blocks-in-space.core
  (:require [quil.core :as qc]))

;; Utility

(defn neg [x] (* -1 x))

;; Block manipulation

(def example-block (atom {:center [0 0 0] :shape #{[0 0 0] [1 0 0] [0 1 0] [1 -1 0]}}))

(defn block-cubes
  "Return the set of locations of cubes in the block"
  [block]
  (set (map #(map + (:center block) %) (:shape block))))

(defn rotate-block
  "Rotate the given block one of: :clockwise :counterclockwise :north :east :south :west"
  [block direction]
  (let [rotation-fns {:clockwise (fn [[x y z]] [(neg y) x z])
                      :counterclockwise (fn [[x y z]] [y (neg x) z])
                      :north (fn [[x y z]] [x (neg z) y])
                      :east (fn [[x y z]] [z y (neg x)])
                      :south (fn [[x y z]] [x z (neg y)])
                      :west (fn [[x y z]] [(neg z) y x])}
        transform-cube (rotation-fns direction)
        transform-shape #(map transform-cube %)]
    (assoc block :shape (set (transform-shape (:shape block))))))

(defn move-block
  "Move the given block one of: :north :east :south :west :down"
  [block direction]
  (let [movement-fns {:north (fn [[x y z]] [x (dec y) z])
                      :east (fn [[x y z]] [(inc x) y z])
                      :south (fn [[x y z]] [x (inc y) z])
                      :west (fn [[x y z]] [(dec x) y z])
                      :down (fn [[x y z]] [x y (dec z)])}]
    (assoc block :center ((movement-fns direction) (:center block)))))

;; Boundaries

(def x-size 5)
(def y-size x-size)
(def z-size 10)

(def wall-cubes
  (letfn [(outer-coord [size] (/ (inc size) 2))
          (wall-edges [size] [(outer-coord size) (neg (outer-coord size))])
          (full-wall [size] (range (neg (outer-coord size)) (inc (outer-coord size))))]
    (set
      (concat
        (for [x (wall-edges x-size) y (full-wall y-size) z (range (neg z-size) (inc 0))]
          [x y z])
        (for [x (full-wall x-size) y (wall-edges y-size) z (range (neg z-size) (inc 0))]
          [x y z])
        (for [x (full-wall x-size) y (full-wall y-size)]
          [x y (neg z-size)])))))

;; Input

(def rotation-keybindings
  {\e :north
   \f :east
   \d :south
   \s :west
   \w :counterclockwise
   \r :clockwise})

(def motion-keybindings
  {\i :north
   \l :east
   \k :south
   \j :west
   \space :down})

(defn handle-key-press []
  (let [key-char (qc/raw-key)
        rotation (rotation-keybindings key-char)
        motion (motion-keybindings key-char)]
    (cond
      rotation (swap! example-block #(rotate-block % rotation))
      motion (swap! example-block #(move-block % motion)))))

;; Drawing

(def window-size [480 360])

(def grid-scale 50)

(defn draw-cube-at
  [loc]
  (qc/push-matrix)
  (qc/translate (map (partial * grid-scale) loc))
  (qc/box grid-scale)
  (qc/pop-matrix))

(defn draw-walls
  []
  (qc/stroke 0 0 255)
  (qc/fill 63 63 63)
  (dorun (map draw-cube-at wall-cubes)))

(defn draw-blocks
  []
  (qc/stroke 0 0 0)
  (qc/fill 255 255 255 153)
  (dorun (map draw-cube-at (block-cubes @example-block))))

(defn draw []
  (qc/background 127 127 127)
  (qc/translate (map #(/ % 2) window-size))
  (draw-walls)
  (draw-blocks))

(defn setup []
  (qc/smooth)
  (qc/frame-rate 24))

;; Startup

(def sketch-options
     [:title "Blocks in Space"
      :setup setup
      :draw draw
      :key-pressed handle-key-press
      :renderer :p3d
      :size window-size])

(defn run
  "This is for running via a repl connection with (run)"
  []
  (eval `(qc/defsketch main-sketch ~@sketch-options)))
