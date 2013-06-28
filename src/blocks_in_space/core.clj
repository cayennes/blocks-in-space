(ns blocks-in-space.core
  (:require [quil.core :as qc]))

;; Utility

(defn neg [x] (* -1 x))

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

;; Block manipulation

(def blocks
  (let [shapes [#{[0 0 0]}
                #{[0 0 0] [1 0 0]}
                #{[0 0 0] [1 0 0] [-1 0 0]}
                #{[0 0 0] [1 0 0] [0 1 0]}]]
    (map (fn [s] {:center [0 0 0] :shape s}) shapes)))

(defn random-block [] (rand-nth blocks))

(defn block-cubes
  "Return the set of locations of cubes in the block"
  [block]
  (set (map #(map + (:center block) %) (:shape block))))

(defn legal?
  [block]
  (not-any? wall-cubes (block-cubes block)))

(def current-block (atom (random-block) :validator legal?))

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

(defn next-block
  []
  (reset! current-block (random-block)))

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

(defn handle-key-press [] ; TODO: this has too much logic
  (let [key-char (qc/raw-key)
        rotation (rotation-keybindings key-char)
        motion (motion-keybindings key-char)]
    (try
      (cond
        rotation (swap! current-block #(rotate-block % rotation))
        motion (swap! current-block #(move-block % motion)))
      (catch IllegalStateException e
        (when (= :down motion) (next-block))))))

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
  (qc/stroke 255 255 255)
  (qc/fill 63 63 63)
  (dorun (map draw-cube-at wall-cubes)))

(defn draw-blocks
  []
  (qc/stroke 0 0 0)
  (qc/fill 255 255 255 153)
  (dorun (map draw-cube-at (block-cubes @current-block))))

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
