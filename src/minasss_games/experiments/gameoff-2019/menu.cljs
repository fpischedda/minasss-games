(ns minasss-games.experiments.gameoff-2019.menu
  (:require [minasss-games.director :as director :refer [scene-ready scene-cleanup]]
            [minasss-games.pixi :as pixi]
            [minasss-games.pixi.scene :as scene]
            [minasss-games.screenplay :as screenplay]
            [minasss-games.experiments.awwwliens.game :as game]))

(def scene {:id ::menu-scene
            :register-keys {"ArrowUp" ::move-up "k" ::move-up "w" ::move-up
                            "ArrowDown" ::move-down "j" ::move-down "s" ::move-down
                            "Enter" ::select "Space" ::select}
            :resources ["images/awwwliens/menu/background.png"
                        "images/awwwliens/menu/baloon.png"
                        "images/awwwliens/menu/cow-still.png"
                        "images/awwwliens/anim/ufo.json"]})

(def main-stage (pixi/make-container))

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

#_(defn scene-key-up ::menu-scene
  [_native action]
  (update-menu! action))

;; NOTE: init should be used to initialize scene context
;; after init the director will load resources, if specified,
;; and we everything will be ready, the ready function will be called
;; init could also start a "Loading" animation or this may be provided
;; by the director directly
;; for now this function is defined only to show the new protocol/contract
;; (defmethod scene-init ::menu-scene
;;   [_scene app-stage]
;;   nil)

(defmethod scene-ready ::menu-scene
  [_scene app-stage]
  (let [background (pixi/make-sprite "images/awwwliens/menu/background.png")]
    (pixi/add-child main-stage background)
    (pixi/add-child main-stage (make-menu @menu-items_))

    (add-watch menu-items_ ::menu-changed-watch menu-changed-listener))
  (pixi/add-child app-stage main-stage))

(defmethod scene-cleanup ::menu-scene
  [_]
  (remove-watch menu-items_ ::menu-changed-watch)
  (pixi/remove-container main-stage))
