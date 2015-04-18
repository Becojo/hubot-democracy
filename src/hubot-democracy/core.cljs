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
        (.send msg "The poll is now ended"))
    (.send msg "There isn't an active poll at the moment")))


(defn handle-show [robot msg]
  (let [question (brain-get robot :question)
        choices (brain-get robot :choices)]
    (if (nil? question)
      (.send msg "There isn't an active poll at the moment")
      (.send msg (str "Question: " question "\n"
                      (display-choices choices))))))


(defn handle-vote [robot msg]
  (.send msg (str "ok " (.. msg -message -user -name))))


(defn hubot-main [robot]
  (.respond robot
            #"poll create (.+) with choices (.+)"
            (partial handle-create robot))

  (.respond robot
            #"poll (end|stop)"
            (partial handle-end robot))

  (.respond robot
            #"poll (show|current)"
            (partial handle-show robot))

  (.respond robot
            #"poll vote"
            (partial handle-vote robot)))


;;; exports

(aset js/module "exports" hubot-main)

(set! *main-cli-fn* (fn [*_]))
