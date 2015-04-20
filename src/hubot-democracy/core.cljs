(ns hubot-democracy.core
  (:require [clojure.string :as str]))


;;; helpers

(def brain (atom {:question nil
                  :choices []
                  :votes {}}))


(defn brain-get [robot key]
  (comment (.get (.-brain robot) key))
  (get @brain key))


(defn brain-set [robot key val]
  (comment (.set (.-brain robot) key val))
  (swap! brain assoc key val))


(defn poll-active? [robot]
  (not (nil? (brain-get robot :question))))


(defn display-choices [choices]
  (str/join "\n"
            (map-indexed (fn [i choice]
                           (str (inc i) ". " choice))
                         choices)))

(defn compile-votes [votes choices]
  (zipmap choices
          (reduce
           #(update-in %1 [(second %2)] inc)
           (into [] (repeat (count choices) 0))
           votes)))


(defn display-compiled-votes [votes]
  (str/join "\n"
            (map (fn [[choice number]]
                   (str choice ": " number " vote" (if (not= number 1) "s")))
                 votes)))


;; commands


(defn handle-create [robot msg]
  (if (poll-active? robot)
    (.send msg "The current poll must be ended before creating another one")
    (let [[_ question schoices] (.-match msg)]
      (brain-set robot :question question)
      (brain-set robot :choices (str/split schoices #","))
      (.send msg "The poll is now active"))))


(defn handle-end [robot msg]
  (if (poll-active? robot)
    (do (brain-set robot :question nil)
        (brain-set robot :votes {})
        (.send msg "The poll is now ended"))
    (.send msg "There isn't an active poll at the moment")))


(defn handle-show [robot msg]
  (let [question (brain-get robot :question)
        choices (brain-get robot :choices)]
    (.send msg
           (if (nil? question)
             "No active poll"
             (str "Question: " question "\n" (display-choices choices))))))


(defn handle-vote [robot msg]
  (let [user-key (.toLowerCase (.. msg -message -user -name))
        votes (brain-get robot :votes)
        choices (brain-get robot :choices)
        [_ schoice] (.-match msg)
        choice (dec (js/Number schoice))]

    (if (contains? votes user-key)
      (.reply msg "You already voted for this poll")
      (if (or (< choice 0) (> choice (count choices)))
        (.reply msg "No such answer is available")
        (do (brain-set robot :votes (assoc votes user-key choice))
            (.reply msg "Your vote was registered"))))))


(defn handle-results [robot msg]
  (if (poll-active? robot)
    (let [votes (brain-get robot :votes)
          choices (brain-get robot :choices)]
      (.send msg (display-compiled-votes (compile-votes votes choices))))
    (.send msg "There isn't an active poll at the moment")))


(defn hubot-main [robot]
  (.respond robot
            #"poll create (.+) with choices (.+)"
            (partial handle-create robot))

  (.respond robot
            #"poll (end|stop)"
            (partial handle-end robot))

  (.respond robot
            #"poll(\s(show|current))?"
            (partial handle-show robot))

  (.respond robot
            #"poll vote ([0-9]+)"
            (partial handle-vote robot))

  (.respond robot
            #"poll results"
            (partial handle-results robot)))


;;; exports

(aset js/module "exports" hubot-main)

(set! *main-cli-fn* (fn [*_]))
