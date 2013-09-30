(ns blocks-in-space.blocks-test
  (:require [clojure.test :refer :all]
            [blocks-in-space.blocks :refer :all]))

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
       (map first)
       (set)))

(def manual-starting-shapes; TODO: go through these and select correct
                           ; orientations etc. and assert equality
  (map parse-block-string ["X"

                           "xX"

                           "xXx"

                           " x
                            xX"

                           "xxXx"

                           "xx
                            xX"

                           "..x
                            xXx"

                           ".x.
                            xXx"

                           "xx.
                            .Xx"]))

(def manual-additional-shapes
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

(testing "sanity"
  (deftest unique-normalized
    (testing "normalizing all orientations has the same result"
      (is (apply = (conj (map #(count (set (map normalize-shape (all-orientations %))))
                              (concat manual-starting-shapes manual-additional-shapes))
                         1)))))
  (deftest fits-within-five
    (testing "blocks will be possible to rotate in the given space"
      (is (< (max (for [dim [x y z]
                        shape (take-while #(<= (count %) 7) additional-shapes)]
                    (width-in-dim shape dim))))))))
