(defproject blocks-in-space "0.3-SNAPSHOT"
  :description "A falling block game in 3D space"
  :url "https://github.com/cayennes/blocks-in-space"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [quil "2.2.0"]
                 [overtone/at-at "1.2.0"]
                 [org.clojure/test.check "0.5.9"]]
  :main blocks-in-space.core)
