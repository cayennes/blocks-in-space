(ns blocks-in-space.core
  (:require [quil.core :as qc]))

;; Block manipulation

(def example-block (atom #{[0 0 0] [0 1 0] [0 0 1] [1 0 1]}))

;; Drawing

(def window-size [480 360])

(def grid-scale 20)

(defn draw-cube-at
  [loc]
  (qc/push-matrix)
  (qc/translate (map (partial * grid-scale) loc))
  (qc/box grid-scale)
  (qc/pop-matrix))

(defn draw []
  (qc/translate (map #(/ % 2) window-size))
  (qc/stroke 0 0 0)
  (qc/fill 255 255 255)
  (dorun (map draw-cube-at @example-block)))

(defn setup []
  (qc/frame-rate 24))

;; Startup

(def sketch-options
     [:title "Blocks in Space"
      :setup setup
      :draw draw
      :renderer :p3d
      :size window-size])

(defn run
  "This is for running via a repl connection with (run)"
  []
  (eval `(qc/defsketch main-sketch ~@sketch-options)))
