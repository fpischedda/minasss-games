(ns minasss-games.experiments.awwwliens.intro
  "Here I am trying to animate the intro where the alien kidnap the cow
  It should be super fun!
  Maybe the intro could go together with the main menu"
  (:require [minasss-games.director :as director :refer [scene-init scene-cleanup]]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.input :as input]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.tween :as tween]
            [minasss-games.experiments.awwwliens.game :as game]))

(def scene {:init-scene ::menu-scene
            :cleanup-scene ::menu-scene})

(def resources ["images/awwwliens/intro-bg.png"
                "images/awwwliens/menu-container.png"])

(def main-stage (pixi/make-container))

(def menu-items_ (atom {:selected-index 0
                        :items [{:text "Play" :position [150 200]}
                                {:text "Credits" :position [150 300]}
                                {:text "Vote" :position [150 400]}]}))

(defn make-menu-entry
  [{:keys [text position]} selected]
  (let [color (if selected "#19d708" "#808284")]
    [:text {:text text
            :position position
            :style {"fill" color "fontSize" 30}}]))

(defn make-menu
  [{:keys [selected-index items]}]
  (scene/render
    [:sprite {:name "menu"
              :position [300 100]
              :texture "images/awwwliens/menu-container.png"}
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
                         (if (> (count (:items menu)) selected-index)
                           (assoc menu :selected-index (inc selected-index))
                           menu)))))
(defn start-game
  []
  (let [app-stage (.-parent main-stage)]
    (director/start-scene game/scene)))

(defmethod update-menu! ::select
  [_]
  (start-game))

(defn handle-input
  [event-type _native action]
  (if (= :key-up event-type)
    (update-menu! action)))

(defn update-step
  "update view related stuff"
  [delta-time]
  (tween/update-tweens delta-time))

(defn setup
  "setup the view based on the menu-items_ atom; main-stage refers to the
  root container, where other graphical elements will be added"
  [main-stage]
  (let [background (pixi/make-sprite "images/awwwliens/intro-bg.png")
        menu-container (make-menu @menu-items_)]
    (pixi/add-child main-stage background)
    (pixi/add-child main-stage menu-container)
    (add-watch menu-items_ :menu-changed-watch menu-changed-listener)))

(defn ^:export loaded-callback []
  (setup main-stage)
  (input/register-keys {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                        "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                        "Enter" ::select "Space" ::select}
    ::menu-handler handle-input)
  (.start (pixi/make-ticker update-step)))

(defmethod scene-cleanup ::menu-scene
  [_]
  (input/unregister-key-handler ::menu-handler)
  (pixi/remove-container main-stage))


(defmethod scene-init ::menu-scene
  [_scene parent-stage]
  (pixi/load-resources resources loaded-callback)
  (pixi/add-child parent-stage main-stage))
