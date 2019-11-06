(ns minasss-games.experiments.awwwliens.intro
  "Here I am trying to animate the intro where the alien kidnap the cow
  It should be super fun!
  Maybe the intro could go together with the main menu"
  (:require [minasss-games.director :as director :refer [scene-init scene-cleanup]]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.screenplay :as screenplay]
            [minasss-games.tween :as tween]
            [minasss-games.experiments.awwwliens.game :as game]))

(def scene {:init-scene ::menu-scene
            :cleanup-scene ::menu-scene})

(def resources ["images/awwwliens/menu/background.png"
                "images/awwwliens/menu/baloon.png"
                "images/awwwliens/menu/cow-still.png"
                "images/awwwliens/anim/ufo.json"])

(def main-stage (pixi/make-container))

(def intro-plot [[:screenplay/move
                  :actor :cow
                  :from [0 0] :to [100 100] :speed 30
                  :then [:screenplay/after
                         :time 500
                         :then [:screenplay/move
                         :actor :ufo
                         :from [0 0] :to [100 100] :speed 50]]]])

(defn make-animated-ufo
  "Create animated ufo element"
  []
  (let [ufo (scene/render
          [:animated-sprite {:spritesheet "images/awwwliens/anim/ufo.json"
                             :animation-name "ufo"
                             :animation-speed 0.05
                             :position [200 200]
                             :name "ufo"}])]
    (.play ufo)
    ufo))

(defn make-cow-still
  "Create cow still element"
  []
  (scene/render
    [:sprite {:texture "images/awwwliens/menu/cow-still.png"
              :position [248 400]
              :name "cow-stil"}]))

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

(defn handle-input
  [event-type _native action]
  (if (= :key-up event-type)
    (update-menu! action)))

(defn setup
  "setup the view based on the menu-items_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [main-stage]
  (let [background (pixi/make-sprite "images/awwwliens/menu/background.png")]
    (pixi/add-child main-stage background)
    (pixi/add-child main-stage (make-cow-still))
    (pixi/add-child main-stage (make-animated-ufo))
    (pixi/add-child main-stage (make-menu @menu-items_))
    (add-watch menu-items_ ::menu-changed-watch menu-changed-listener)))

(defn ^:export loaded-callback []
  (setup main-stage)
  (input/register-keys {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                        "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                        "Enter" ::select "Space" ::select}
    ::menu-handler handle-input)
  )

(defmethod scene-cleanup ::menu-scene
  [_]
  (input/unregister-key-handler ::menu-handler)
  (remove-watch menu-items_ ::menu-changed-watch)
  (pixi/remove-container main-stage))


(defmethod scene-init ::menu-scene
  [_scene parent-stage]
  (pixi/load-resources resources loaded-callback)
  (pixi/add-child parent-stage main-stage))
