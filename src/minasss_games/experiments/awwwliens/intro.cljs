(ns minasss-games.experiments.awwwliens.intro
  "Here I am trying to animate the intro where the alien kidnap the cow
  It should be super fun!"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.pixi.settings :as settings]
            [minasss-games.tween :as tween]))

(def resources ["images/background.png"])

(def main-stage (pixi/make-container))

(defn make-menu-entry
  [{:keys [text selected position]}]
  (let [color (if selected "#19d708" "#808284")]
    (scene/render
      [:text {:text text
              :position position
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

(defn menu-changed-listener
  "Update the menu when selection changes"
  [_key _ref _old new-menu]
  (let [menu (make-menu new-menu)]
    (pixi/remove-child-by-name main-stage "menu")
    (pixi/add-child main-stage menu)))

(defmulti update-menu!
  (fn [verb _param] verb))

(defmethod update-menu! ::move-selection
  [_ direction]
  (condp = direction
    ::up (swap! menu-items_ (fn [menu]
                              (let [selected-index (:selected-index menu)]
                                (if (> 0 selected-index)
                                  (assoc menu :selected-index (dec selected-index))
                                  menu))))))

(defn handle-input
  [event-type _native direction]
  (if (= :key-up event-type)
    (update-menu! :move-selection direction)))

(defn update-step
  "update view related stuff"
  [delta-time]
  (tween/update-tweens delta-time))

(defn setup
  "setup the view based on the menu-items_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [main-stage]
  (let [background (pixi/make-sprite "images/background.png")
        menu-container (make-menu @menu-items_)]
    (pixi/add-child main-stage background)
    (pixi/add-child-view main-stage menu-container)
    (add-watch menu-items_ :menu-changed-watch menu-changed-listener)))

(defn ^:export loaded-callback []
  (setup main-stage)
  (input/register-keys {"ArrowUp" ::up "k" ::up "w" ::up
                        "ArrowDown" ::down "j" ::down "s" ::down
                        "Space" ::select}
    :menu-handler handle-input)
  (.start (pixi/make-ticker update-step)))

(defn init [app]
  (settings/set! :scale-mode :nearest)
  (pixi/load-resources resources loaded-callback)
  (pixi/add-to-app-stage app main-stage))
