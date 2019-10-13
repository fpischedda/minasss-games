(ns minasss-games.experiments.awwwliens.intro
  "Here I am trying to animate the intro where the alien kidnap the cow
  It should be super fun!")

(def resources ["images/background.png" "images/cow.png" "images/ufo.png" "images/land.png" "images/grass.png" "images/manure.png"])

(defonce world-view_ (atom {}))

(def cell-size 128)

(defn update-step
  "update view related stuff"
  [delta-time]
  (tween/update-tweens delta-time))


(defn make-menu-entry
  [{:keys [text selected position]}]
  (let [color (if selected "#19d708" "#808284")]
    (scene/render
      [:text {:text text
              :position position
              :position [cell-size 0]
              :anchor [0.5 0.5]
              :style {"fill" color "fontSize" 30}}])))

(def menu-items_ (atom [{:text "Play" :position [0 100] :selected true}
                       {:text "Credits" :position [0 200] :selected false}
                       {:text "Vote" :position [0 300] :selected false}]))

(defn make-menu
  [menu-items]
  (scene/render
    [:container {:name "menu"
                 :position [300 100]}
     (into [] (map #(make-menu-entry %) menu-items))]))

(defn menu-changed-listenr
  "Update the menu when selection changes"
  [_key _ref _old new-menu]
  (let [menu (make-menu new-menu)]
    (pixi/remove-child-by-name main-stage "menu")
    (pixi/add-child main-stage menu)))

(defn setup
  "setup the view based on the world_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [world_ main-stage]
  (let [background (pixi/make-sprite "images/background.png")
        menu-container (make-menu @menu-items_)]
    (pixi/add-child main-stage background)
    (pixi/add-child-view main-stage menu-container)
    (add-watch menu-items_ :menu-changed-watch menu-changed-listener)))
