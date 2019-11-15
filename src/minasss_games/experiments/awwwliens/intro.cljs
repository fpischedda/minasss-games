(ns minasss-games.experiments.awwwliens.intro
  "Here I am trying to animate the intro where the alien kidnap the cow
  It should be super fun!
  Maybe the intro could go together with the main menu"
  (:require [minasss-games.director :as director :refer [scene-init
                                                         scene-ready
                                                         scene-key-up
                                                         scene-cleanup]]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.screenplay :as screenplay]
            [minasss-games.experiments.awwwliens.game :as game]))

(def clog js/console.log)

(def scene {:id ::menu
            :resources ["images/awwwliens/menu/background.png"
                        "images/awwwliens/menu/baloon.png"
                        "images/awwwliens/menu/cow-still.png"
                        "images/awwwliens/anim/ufo.json"]
            :key-mapping {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                          "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                          "Enter" ::select "Space" ::select
                          "r" ::restart-screenplay}})

(def main-stage (pixi/make-container))

(def intro-screenplay
  [screenplay/action-after :time 1.5
   :then [screenplay/action-move
          :actor :ufo
          :from [0 0] :to [248 200] :time 5.0
          :then [screenplay/action-after
                 :time 3.0
                 :then [[screenplay/action-scale
                         :actor :cow
                         :from 0.1 :to 1.0 :time 4.0]
                        [screenplay/action-move
                         :actor :cow
                         :from [248 200] :to [248 400] :time 4.0]]]]])

(comment
  (let [container (pixi/get-child-by-name main-stage "cow")]
    (pixi/set-attributes container {:scale 0.5}))
  )

(defn screenplay-listener
  [_event-type action payload]
  (when-let [actor (:actor payload)]
    (let [container (pixi/get-child-by-name main-stage (name actor))]
      (pixi/set-attributes container (:state payload)))))

(defn make-animated-ufo
  "Create animated ufo element"
  []
  (let [ufo (scene/render
              [:animated-sprite {:spritesheet "images/awwwliens/anim/ufo.json"
                                 :animation-name "ufo"
                                 :animation-speed 0.05
                                 :position [-200 -200]
                                 :name "ufo"}])]
    (.play ufo)
    ufo))

(defn make-cow-still
  "Create cow still element"
  []
  (scene/render
    [:sprite {:texture "images/awwwliens/menu/cow-still.png"
              :position [-200 -400]
              :pivot [0.5 0.5]
              :name "cow"}]))

(def menu-items_ (atom {:selected-index 0
                        :items [{:text "Press Enter\nTo Play" :position [-90 100]}
                                {:text "Arrows\nWASD\nHJKL\nTo Move" :position [-90 200]}
                                {:text "By Carmilla\nAnd Minasss" :position [-90 300]}]}))

(defn make-menu-entry
  [{:keys [text position]} selected]
  (let [color (if selected "#19d708" "#808284")
        [x y] position]
    [:text {:text text
            :anchor [0.5 0.5]
            :position [(if selected (- x 20) x) y]
            :style {"fill" color "fontSize" 25}}]))

(defn make-menu
  [{:keys [selected-index items]}]
  (scene/render
    [:sprite {:name "menu"
              :anchor [1.0 0.0]
              :position [590 50]
              :texture "images/awwwliens/menu/baloon.png"}
     (into [] (map-indexed #(make-menu-entry %2 (= %1 selected-index)) items))]))

(defn menu-changed-listener
  "Update the menu when selection changes"
  [_key _ref _old new-menu]
  (let [menu (make-menu new-menu)]
    (pixi/remove-child-by-name main-stage "menu")
    (pixi/add-child main-stage menu)))

(defmulti update-menu!
  (fn [action] action))

(defmethod update-menu! ::move-up
  [_]
  (swap! menu-items_ (fn [menu]
                       (let [selected-index (:selected-index menu)]
                         (if (< 0 selected-index)
                           (assoc menu :selected-index (dec selected-index))
                           menu)))))

(defmethod update-menu! ::move-down
  [_]
  (swap! menu-items_ (fn [menu]
                       (let [selected-index (:selected-index menu)]
                         (if (> (dec (count (:items menu))) selected-index)
                           (assoc menu :selected-index (inc selected-index))
                           menu)))))

(defmethod update-menu! ::select
  [_]
  (let [app-stage (.-parent main-stage)]
    (director/start-scene game/scene)))

(defmethod scene-key-up ::menu
  [scene _native action]
  (if (= ::restart-screenplay action)
    (screenplay/start intro-screenplay screenplay-listener)
    (update-menu! action)))

;; setup the view based on the menu-items_ atom; main-stage refers to the
;; root container, where other graphical elements will be added
(defmethod scene-ready ::menu
  [_scene app-stage]
  (let [background (pixi/make-sprite "images/awwwliens/menu/background.png")]
    (pixi/add-child main-stage background)
    (pixi/add-child main-stage (make-cow-still))
    (pixi/add-child main-stage (make-animated-ufo))
    (pixi/add-child main-stage (make-menu @menu-items_))

    (pixi/add-child app-stage main-stage)

    (screenplay/start intro-screenplay screenplay-listener)
    (add-watch menu-items_ ::menu-changed-watch menu-changed-listener)))

(defmethod scene-cleanup ::menu
  [_]
  (screenplay/clean-actions)
  (remove-watch menu-items_ ::menu-changed-watch)
  (pixi/remove-container main-stage))
