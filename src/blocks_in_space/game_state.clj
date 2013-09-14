(ns blocks-in-space.game-state
  (:use [blocks-in-space.blocks :only [block-cubes rotate-block move-block
                                       starting-shapes additional-shapes]])
  (:use [blocks-in-space.utility :only [neg]])
  (:require [clojure.set :as set])
  (:require [overtone.at-at :as at]))

;; Dimensions etc.

(def x-size 5)
(def y-size x-size)
(def z-size 10)

(def center (mapv #(quot % 2) [x-size y-size]))

;; Secheduling

(def timer-pool (at/mk-pool))

;; Blocks

(defn- full-levels
  [cubes]
  (let [level-size (* x-size y-size)]
   (->> (group-by last cubes) ; group cubes by level (z-coordinate)
        (filter (fn [[_ v]] (= level-size (count v)))) ; only the full ones
        keys))) ; the level

(defn- remove-level
  [level cubes]
  (let [above (filter (fn [[_ _ z]] (> z level)) cubes)
        below (filter (fn [[_ _ z]] (< z level)) cubes)]
    (set
      (clojure.set/union below (map (fn [[x y z]] [x y (dec z)]) above)))))

(def old-cubes (atom #{}))

(defn- legal?
  [block]
  (let [cubes (block-cubes block)]
    (and
      (not-any? (fn [[x y z]] (or (< x 0) ; within bounds
                                  (< y 0)
                                  (>= x x-size)
                                  (>= y y-size)
                                  (<= z (neg z-size))))
                cubes)
      (not-any? @old-cubes cubes)))) ; not in an already fallen block

(def current-possible-shapes (atom starting-shapes))

(def future-shapes (atom additional-shapes))

(def status-message (atom "press p to play"))

(defn get-status-message
  []
  @status-message)

(defn- new-random-block
  []
  {:center (conj center 0) :shape (rand-nth @current-possible-shapes)})

(def current-block (atom (new-random-block) :validator legal?))

(defn- next-block
  []
  (swap! old-cubes (partial clojure.set/union (block-cubes @current-block)))
  (try (reset! current-block (new-random-block))
       (catch IllegalStateException e
         (reset! status-message "game over"))))

(def cleared-planes (atom 0))

(add-watch
  old-cubes
  :remove
  (fn [_ reference _ new-value]
    (when-let [full-level (first (full-levels new-value))]
      (swap! cleared-planes inc)
      (swap! reference #(remove-level full-level %)))))

(defn- another-possible-shape!
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

(defn get-cleared-planes
  []
  @cleared-planes)

(defn move-current-block!
  [move-type direction]
  (try
    (case move-type
      :rotate (swap! current-block #(rotate-block % direction))
      :translate (swap! current-block #(move-block % direction)))
    (catch IllegalStateException e
      ; ignore most impossible movements but progress to next block when going
      ; down is impossible
      (when (= :down direction) (next-block)))))

(defn get-frozen-cubes
  []
  @old-cubes)

(defn get-falling-cubes
  []
  (block-cubes @current-block))

;; Modes

(def modes
  {:pause {:enter (fn []
                    (at/stop-and-reset-pool! timer-pool :strategy :kill)
                    (reset! status-message "paused"))}
   :play {:enter (fn []
                   (at/every 2000 #(move-current-block! :translate :down) timer-pool)
                   (reset! status-message ""))}})

(def mode (atom :pause))

(add-watch
  mode
  :change
  (fn [_ _ old-value new-value]
    (if (not= old-value new-value)
        ((get-in modes [new-value :enter])))))

(defn set-mode!
  [new-mode]
  (reset! mode new-mode))

(defn get-mode
  []
  @mode)
