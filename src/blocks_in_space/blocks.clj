(ns blocks-in-space.blocks
  (:use [blocks-in-space.utility :only [neg]]))

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
