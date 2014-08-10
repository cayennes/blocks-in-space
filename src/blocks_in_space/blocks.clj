(ns blocks-in-space.blocks
  (:use [blocks-in-space.utility :only [neg]]))

(def x first)
(def y second)
(def z last)

(def make-shape sorted-set)

(defn make-block
  [center shape]
  {:center center
   :shape shape})

(defn block-cubes
  "Return the set of locations of cubes in the block"
  [block]
  (set (map #(mapv + (:center block) %) (:shape block))))

(defn rotate-shape
  [shape direction]
  (let [rotation-fns {:clockwise (fn [[x y z]] [(neg y) x z])
                      :counterclockwise (fn [[x y z]] [y (neg x) z])
                      :north (fn [[x y z]] [x (neg z) y])
                      :east (fn [[x y z]] [z y (neg x)])
                      :south (fn [[x y z]] [x z (neg y)])
                      :west (fn [[x y z]] [(neg z) y x])}
        transform-cube (rotation-fns direction)
        transform-shape #(mapv transform-cube %)]
      (apply make-shape (transform-shape shape))))

(defn rotate-block
  "Rotate the given block one of: :clockwise :counterclockwise :north :east :south :west"
  [block direction]
  (assoc block :shape (rotate-shape (:shape block) direction)))

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

;;;; create complete sets of shapes

(defn width-in-dim
  [shape coord]
  (let [vals (map coord shape)]
    (-> (- (apply max vals) (apply min vals))
        (inc))))

(defn make-centered
  "Shifts the block so that the center is in the middle of the width of each
  dimension; if there are an even number of cubes it will choose the side
  towards the center of mass"
  [shape]
  (let [center-points (mapv #(/ (+ (apply max (map % shape))
                                   (apply min (map % shape)))
                                2)
                            [x y z])
        centerish-cubes (mapv #(vec [(int (Math/floor %)) (int (Math/ceil %))])
                              center-points)
        center (mapv (fn [dim] (if (> (count
                                       (filter #(<= (dim %)
                                                    (first (dim centerish-cubes)))
                                                     shape))
                                      (count
                                       (filter #(>= (dim %)
                                                    (second (dim centerish-cubes)))
                                                     shape)))
                                   (first (dim centerish-cubes))
                                   (second (dim centerish-cubes))))
                     [x y z])]
    (set (map (fn [cube] (mapv #(- %1 %2) cube center)) shape))))

(defn shape-score
  "Gives a score (in the form of a vector) to shapes based on in this order:

  * number of cubes
  * flatness in z, y, and then x dimension
  * lower center of mass in z, y, and then x dimension
  * simple comparison of the shapes as ordered lists of blocks

  When comparing non-equivalent shapes, this has the desired quality of
  putting smaller and then flatter shapes first.

  When comparing equivalent shapes, this has the desired quality of selecting
  an orientation relatively likely to show as much as possible of the
  features, since it will firstly be as flat as possible in the z direction
  and secondly have more cubes lower than higher.  Also, it will return 0 iff
  one shape is merely a translation of the other."
  [shape]
  (let [s (make-centered shape)]
    [(count s); number of cubes
     (mapv #(width-in-dim s %) [z y x]); flatness
     (mapv #(* -1 (apply + (% s))) [z y x]); proportional to center of mass
     (vec (sort s))])); tie-breaker

(defn all-orientations
  [shape]
  (->>
    (for [twist (map #(repeat % :clockwise) (range 4))
          turn [[] [:north] [:east] [:south] [:west] [:north :north]]]
      (let [rotations (concat twist turn)]
        (first
          (filter #(empty? (second %))
                  (iterate (fn [[current-shape remaining-rotations]]
                               [(rotate-shape current-shape (first remaining-rotations))
                                (rest remaining-rotations)])
                           [shape rotations])))))
      (map first)
      (set)))

(defn normalize-shape
  "given a shape, pick the best orientation for it and then center it on the origin"
  [shape]
  (->> shape
       (all-orientations)
       (map make-centered)
       (reduce #(if (< (compare (shape-score %1) (shape-score %2)) 0)
                    %1
                    %2))))

(defn all-additions
  "The set of normalized shapes fitting within size 5 cube that can be created
  by adding one cube"
  [shape]
  (set
    (filter
      #(and (> (count %) (count shape))
            (<= (width-in-dim % x) 5)); TODO: 5 shouldn't really be set in this file at all
      (for [cube shape
            dim-count (range 3)
            offset [-1 1]]
        (let [new-cube (mapv + cube (concat (repeat dim-count 0) [offset 0 0]))]
          (normalize-shape (conj shape new-cube)))))))

(defn shapes-of-next-size
  [shapes]
  (->> shapes
       (map all-additions)
       (apply concat)
       (set)
       (sort-by shape-score)))

(def all-shapes
  (seque
    (apply concat (iterate shapes-of-next-size [#{[0 0 0]}]))))

(def starting-shapes (take-while #(= (width-in-dim % z) 1) all-shapes))

(def additional-shapes (drop-while #(= (width-in-dim % z) 1) all-shapes))
