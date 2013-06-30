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

(defn parse-block-string
  "Take a string containing a human readable representation of a 3-dimensional
   block and return a vector of coordinates of cubes"
  [block-str]
  (->> block-str
       (#(clojure.string/split % #"\n")) ; split the lines
       (map #(clojure.string/split % #" +")) ; split the parts
       (map (partial filter #(pos? (count %)))) ; drop remnants of initial whitespace
       (map #(map seq %)) ; convert strings to lists of chars
       ; now the outermost list represents y,
       ;     the sublists z,
       ;     and sublists of that represent x
       (map-indexed (fn [y l] (map (partial map #(vector [y] %)) l)))
       (map #(map-indexed (fn [z l] (map (fn [[[y] s]] [[y z] s]) l)) %))
       (map (partial map #(map-indexed (fn [x [[y z] c]] [[x y z] c]) %)))
       (apply concat) (apply concat)
       ; now we have tuples of coordinate and character
       ((fn [l]
          (let [center (first (first (drop-while (fn [[v c]] (and (not= \, c) (not= \X c))) l)))]
          (map (fn [[v c]] [(map #(- %1 %2) v center) c]) l))))
       ; now it's shifted so that the X or , is at the center
       (filter (fn [[v c]] (or (= c \x) (= c \X))))
       (map first)))

(def starting-shapes
  (map parse-block-string ["xXxx"

                           "..x
                            xXx"

                           ".x.
                            xXx"

                           "xx
                            Xx"

                           ".xx
                            xX."

                           "Xx x.
                            x. .."

                           ".. x.
                            Xx x."

                           ".. .x
                            xX .x"]))

(def blocks
  (map (fn [s] {:center [0 0 0] :shape s}) starting-shapes))

(defn random-block [] (rand-nth blocks))

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

(def current-block (atom (random-block) :validator legal?))

(defn next-block
  []
  (swap! old-cubes (partial clojure.set/union (block-cubes @current-block)))
  (reset! current-block (random-block)))

(def cleared-planes (atom 0))

(add-watch
  old-cubes
  :remove
  (fn [_ reference _ new]
    (when-let [full-level (first (full-levels new))]
      (swap! cleared-planes inc)
      (swap! reference #(remove-level full-level %)))))

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
    (try
      (cond
        rotation (swap! current-block #(rotate-block % rotation))
        motion (swap! current-block #(move-block % motion)))
      (catch IllegalStateException e
        (when (= :down motion) (next-block))))))

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
  (qc/frame-rate 24))

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
  (eval `(qc/defsketch main-sketch ~@sketch-options)))
