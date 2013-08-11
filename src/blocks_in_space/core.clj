(ns blocks-in-space.core
  (:use [blocks-in-space.blocks :only [block-cubes rotate-block move-block
                                       starting-shapes additional-shapes]])
  (:use [blocks-in-space.utility :only [neg]])
  (:require [quil.core :as qc])
  (:require [overtone.at-at :as at])
  (:gen-class))

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

;; Old cubes at the bottom

(defn full-levels
  [cubes]
  (let [level-size (* x-size y-size)]
   (->> (group-by last cubes) ; group cubes by level (z-coordinate)
        (filter (fn [[_ v]] (= level-size (count v)))) ; only the full ones
        keys))) ; the level

(defn remove-level
  [level cubes]
  (let [above (filter (fn [[_ _ z]] (> z level)) cubes)
        below (filter (fn [[_ _ z]] (< z level)) cubes)]
    (clojure.set/union below (map (fn [[x y z]] [x y (dec z)]) above))))

;; State

(def old-cubes (atom #{}))

(defn legal?
  [block]
  (not-any? (clojure.set/union wall-cubes @old-cubes) (block-cubes block)))

(def current-possible-shapes (atom starting-shapes))

(def future-shapes (atom additional-shapes))

(defn new-random-block
  []
  {:center [0 0 0] :shape (rand-nth @current-possible-shapes)})

(def current-block (atom (new-random-block) :validator legal?))

(defn next-block
  []
  (swap! old-cubes (partial clojure.set/union (block-cubes @current-block)))
  (reset! current-block (new-random-block)))

(defn move-current-block
  [move-type direction]
  (try
    (case move-type
      :rotate (swap! current-block #(rotate-block % direction))
      :translate (swap! current-block #(move-block % direction)))
    (catch IllegalStateException e
      ; ignore most impossible movements but progress to next block when going
      ; down is impossible
      (when (= :down direction) (next-block)))))

(def cleared-planes (atom 0))

(add-watch
  old-cubes
  :remove
  (fn [_ reference _ new-value]
    (when-let [full-level (first (full-levels new-value))]
      (swap! cleared-planes inc)
      (swap! reference #(remove-level full-level %)))))

(defn another-possible-shape!
  []
  (when-let [next-shape (first @future-shapes)]
    (swap! current-possible-shapes #(conj % next-shape))
    (swap! future-shapes rest)))

(add-watch
  cleared-planes
  :add-shape-every-planes
  (fn [_ reference old-value new-value]
    (when (> (quot new-value 3) (quot old-value 3))
          (another-possible-shape!))))

;; Secheduling

(def timer-pool (at/mk-pool))

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
      rotation (move-current-block :rotate rotation)
      motion (move-current-block :translate motion))))

;; Drawing

(def grid-scale 50)

(def window-size
  (vec (->> [x-size y-size]
            (map (partial + 4))
            (map (partial * grid-scale)))))

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
  (gradient (neg z)))

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
    (dorun (map #(draw-cube-at % stroke :level) @old-cubes))
    (dorun (map #(draw-cube-at % stroke fill) (block-cubes @current-block)))))

(defn draw-score
  []
  (qc/fill 255 255 255)
  (qc/text (str @cleared-planes)
           10 (* 0.5 (second window-size))))

(defn draw []
  (qc/background 0 0 0)
  (qc/stroke-weight 2)
  (draw-score)
  (qc/translate (map #(/ % 2) window-size))
  (draw-walls)
  (draw-blocks))

(defn setup []
  (qc/smooth)
  (qc/frame-rate 24)
  ; drop current block every 2 seconds
  (at/every 2000 #(move-current-block :translate :down) timer-pool))

;; Startup

(def sketch-options
     [:title "Blocks in Space"
      :setup setup
      :draw draw
      :key-pressed handle-key-press
      :renderer :opengl
      :size window-size])

(defn run
  "This is for running via a repl connection with (run)"
  []
  (at/stop-and-reset-pool! timer-pool :strategy :kill)
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
