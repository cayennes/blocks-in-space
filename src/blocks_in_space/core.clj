(ns blocks-in-space.core
  (:use [blocks-in-space.utility :only [neg]])
  (:use [blocks-in-space.game-state :only [x-size y-size z-size
                                           set-mode! get-mode
                                           get-cleared-planes
                                           move-current-block!
                                           get-frozen-cubes get-falling-cubes]])
  (:require [clojure.set :as set])
  (:require [quil.core :as qc])
  (:gen-class))

;; Boundaries

(def wall-cubes
  (letfn [(wall-edges [size] [-1 size])
          (full-wall [size] (range -1 (inc size)))]
    (set
      (concat
        (for [x (wall-edges x-size) y (full-wall y-size) z (range (neg z-size) (inc 0))]
          [x y z])
        (for [x (full-wall x-size) y (wall-edges y-size) z (range (neg z-size) (inc 0))]
          [x y z])
        (for [x (full-wall x-size) y (full-wall y-size)]
          [x y (neg z-size)])))))

;; Info

(defn get-status-message
  []
  (let [messages {:new "press p to play"
                  :game-over "game over! press n for a new game"
                  :pause "paused"
                  :play ""}]
    ((get-mode) messages)))

;; Drawing

(def grid-scale 50)

(def window-size
  (->> [x-size y-size]
       (map (partial + 2))
       (mapv (partial * grid-scale))))

(def gradient [[0x30 0x14 0x0F]
               [0x4C 0x22 0x2A]
               [0x60 0x37 0x4D]
               [0x65 0x52 0x74]
               [0x57 0x72 0x96]
               [0x34 0x93 0xAA]
               [0x07 0xB3 0xAC]
               [0x44 0xCF 0x9C]
               [0x8D 0xE8 0x82]
               [0xDE 0xF9 0x68]])

(defn level-color
  [z]
  (try (gradient (neg z))
       (catch IndexOutOfBoundsException e
         (first gradient))))

(defn draw-cube-at
  [[x y z] stroke fill]
  (qc/push-matrix)
  (qc/translate (map (partial * grid-scale) [x y z]))
  (apply qc/stroke (if (= stroke :level) (level-color z) stroke))
  (apply qc/fill (if (= fill :level) (level-color z) fill))
  (qc/box (dec grid-scale))
  (qc/pop-matrix))

(defn draw-walls
  []
  (let [fill [63 63 63]]
    (dorun (map #(draw-cube-at % [255 255 255] fill) wall-cubes))))

(defn draw-blocks
  []
  (let [stroke [0 0 0]
        fill [255 255 255 127]]
    (dorun (map #(draw-cube-at % stroke :level) (get-frozen-cubes)))
    (dorun (map #(draw-cube-at % stroke fill) (get-falling-cubes)))))

(defn draw-text
  []
  (qc/fill 255 255 255)
  (qc/text (str (get-cleared-planes))
           10 (* 0.5 (second window-size)))
  (qc/text (get-status-message) (* 1.5 grid-scale) (* 0.5 grid-scale)))

(defn draw []
  (qc/background 0 0 0)
  (qc/stroke-weight 2)
  (draw-text)
  (qc/translate ; everything offset by half a cube because they draw from the center
    [(* grid-scale 1.5) ; also allow space for tops of walls
     (* grid-scale 1.5)
     (* grid-scale -0.5)]) ; so that the top of the walls are in the plane of the window
  (draw-walls)
  (draw-blocks))

(defn setup []
  (qc/smooth)
  (qc/frame-rate 24))

;; Keybindings

(def keybindings
  {:new {\p #(set-mode! :play)}
   :pause {\p #(set-mode! :play)}
   :play {\e #(move-current-block! :rotate :north)
          \f #(move-current-block! :rotate :east)
          \d #(move-current-block! :rotate :south)
          \s #(move-current-block! :rotate :west)
          \w #(move-current-block! :rotate :counterclockwise)
          \r #(move-current-block! :rotate :clockwise)
          \i #(move-current-block! :translate :north)
          \l #(move-current-block! :translate :east)
          \k #(move-current-block! :translate :south)
          \j #(move-current-block! :translate :west)
          \space #(move-current-block! :translate :down)
          \p #(set-mode! :pause)}
    :game-over {\n #(set-mode! :new)}})

;; Startup

(def sketch-options
     [:title "Blocks in Space"
      :setup setup
      :draw draw
      :key-pressed #((get-in keybindings
                             [(get-mode) (qc/raw-key)]
                             (fn [])))
      :renderer :opengl
      :size window-size])

(defn run
  "This is for running via a repl connection with (run)
  
  For some reason - I think a quil bug - it doesn't always work when it has
  been previously run in the same repl.  Trying again often succeeds."
  []
  (eval `(qc/defsketch main-sketch ~@sketch-options)))

; exit from https://groups.google.com/forum/?fromgroups=#!topic/clj-processing/eY6FpVYX-XU
;           https://www.refheap.com/9034

(defn exit-on-close [sketch]
  (let [frame (-> sketch .getParent .getParent .getParent .getParent)]
    (.setDefaultCloseOperation frame javax.swing.JFrame/EXIT_ON_CLOSE)))

(defn -main
  "This is for running as a stand-alone app ($lein run or the uberjar)"
  [& args]
  (exit-on-close (apply qc/sketch sketch-options)))
