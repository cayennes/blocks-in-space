(ns blocks-in-space.core
  (:require [quil.core :as qc]))

;; Utility

(defn neg [x] (* -1 x))

;; Block manipulation

(def example-block (atom #{[0 0 0] [1 0 0] [0 1 0] [1 -1 0]}))

(defn rotate-block
  "Rotate the given block one of: :clockwise :counterclockwise :north :east :south :west"
  [block direction]
  (let [rotation-fns {:clockwise (fn [[x y z]] [(neg y) x z])
                      :counterclockwise (fn [[x y z]] [y (neg x) z])
                      :north (fn [[x y z]] [x (neg z) y])
                      :east (fn [[x y z]] [z y (neg x)])
                      :south (fn [[x y z]] [x z (neg y)])
                      :west (fn [[x y z]] [(neg z) y x])}]
    (set (map (rotation-fns direction) block))))

;; Input

(def rotation-keybindings
  {\e :north
   \f :east
   \d :south
   \s :west
   \w :counterclockwise
   \r :clockwise})

(defn handle-key-press []
  (let [key-char (qc/raw-key)
        rotation (rotation-keybindings key-char)]
    (when rotation
      (swap! example-block #(rotate-block % rotation)))))

;; Drawing

(def window-size [480 360])

(def grid-scale 50)

(defn draw-cube-at
  [loc]
  (qc/push-matrix)
  (qc/translate (map (partial * grid-scale) loc))
  (qc/box grid-scale)
  (qc/pop-matrix))

(defn draw []
  (qc/background 127 127 127)
  (qc/translate (map #(/ % 2) window-size))
  (qc/stroke 0 0 0)
  (qc/fill 255 255 255 153)
  (dorun (map draw-cube-at @example-block)))

(defn setup []
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
