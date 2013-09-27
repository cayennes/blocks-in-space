(ns blocks-in-space.blocks
  (:use [blocks-in-space.utility :only [neg]]))

(def x first)
(def y second)
(def z last)

(defn block-cubes
  "Return the set of locations of cubes in the block"
  [block]
  (set (map #(map + (:center block) %) (:shape block))))

(defn make-block
  [center shape]
  {:center center
   :shape shape})

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

(defn shift-within-bounds
  "Given some bounds and a block, return a similar block within those bounds.
  Returns nil if not possible."
  [block x-min y-min x-max y-max]
  (letfn [(has-cubes-outside?
            [b coord bound min-or-max]
            (->> (block-cubes b)
                 (map coord)
                 (apply min-or-max)
                 (min-or-max bound)
                 (not= bound)))
          (needed-shifts
            [b]
            (->> [[:north (has-cubes-outside? b y y-max max)]
                  [:east (has-cubes-outside? b x x-min min)]
                  [:south (has-cubes-outside? b y y-min min)]
                  [:west (has-cubes-outside? b x x-max max)]]
                 (filter second); only the things that have shifts outside
                 (map first); just the shift
                 (set)))
          (legal? [b] (empty? (needed-shifts b)))
          (one-shift [b]
                     (let [shifts (needed-shifts b)]
                       ; If it needs to shift in opposite directions, it just
                       ; doesn't fit.  This is not an actual possibility in
                       ; current game design.
                       (if (or (and (:east shifts) (:west shifts))
                               (and (:north shifts) (:south shifts)))
                           nil
                           (move-block b (first shifts)))))]
    (->> (iterate one-shift block)
         (filter #(or (= nil %) (legal? %)))
         (first))))

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
          (let [center (->> l
                            (filter (fn [[v c]] (#{\, \X} c)))
                            (first); the tuple with the desired character
                            (first))]; its coordinate
          (map (fn [[v c]] [(map #(- %1 %2) v center) c]) l))))
       ; now it's shifted so that the X or , is at the center
       (filter (fn [[v c]] (or (= c \x) (= c \X))))
       (map first)))

(def starting-shapes
  (mapv parse-block-string ["X"

                            "Xx"

                            "xXx"

                            "Xx
                             x "

                            "xXxx"

                            "xx
                             Xx"

                            "x..
                             xXx"

                            ".x.
                             xXx"

                            ".xx
                             xX."]))


(def additional-shapes
  (map parse-block-string ["Xx x.
                            x. .."

                           ".. x.
                            Xx x."

                           ".. .x
                            xX .x"

                           "xxXxx"

                           "xXxx
                            x   "

                           "xxx
                            x,
                            x  "

                           "..x
                            xXx
                            x.."

                           ".x.
                            xXx
                            x.."

                           "x
                            xXx
                            x  "

                           "x.x
                            xXx"

                           "xx
                            xXx"

                           ".x..
                            xXxx"

                           ".x.
                            xXx
                            .x."

                           "x..
                            xX.
                            .xx"

                           "xx..
                            .Xxx"

                           "xx ..
                            Xx x."

                           ".x. ...
                            xXx .x."

                           ".x. .x.
                            xXx ..."

                           ".x. ...
                            xXx ..x"

                           "x.. ...
                            xXx x.."

                           "..x ...
                            xXx ..x"

                           "x.. ...
                            xXx .x."

                           "..x ...
                            xXx .x."

                           "x.. ...
                            xXx ..x"

                           "..x ...
                            xXx x.."

                           ".xx ...
                            xX. .x."

                           "xx. ...
                            .Xx .x."

                           ".xx ...
                            xX. x.."

                           "xx. ...
                            .Xx ..x"

                           ".X. .xx
                            xx. ..."

                           ".X. xx.
                            .xx ..."

                           "x. x.
                            Xx .x"

                           ".x .x
                            xX x."]))
