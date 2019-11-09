(ns minasss-games.director
  "The director should help to coordinate transitions between different
  scenes, providing a way to start a new scene and clean everything
  afterwards."
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.screenplay :as screenplay]
            [minasss-games.tween :as tween]
            [oops.core :refer [oget]]))

(def director_ (atom {}))

(defn ^:export update-step
  "update view related stuff"
  [delta-time]
  (screenplay/update-actions delta-time)
  (tween/update-tweens delta-time))

(defn init
  "Initialize the director which is basicly a map that holds
  - :app the main app object
  - :app-stage the main stage where to attach scenes
  - :current-scene optional, a map that contains information about current scene
    like init function, cleanup function and maybe something else in the future"
  [app]
  (pixi/add-to-shared-ticker update-step)
  (reset! director_ {:app app
                     :app-stage (oget app "stage")}))

(defmulti scene-cleanup :cleanup-scene)

(defmethod scene-cleanup :default
  [scene]
  (println "no cleanup-scene defined for this scene:" scene))

(defmulti scene-init (fn [scene _stage] (:init-scene scene)))

(defmethod scene-init :default
  [scene _]
  (println "no init-scene defined for this scene:" scene))

(defn start-scene
  "Set provided scene as the current one, if an old scene is running
  clean it up before starting the new one.
  A scene is a map containing dispatching keywords used to call setup and
  teardown multimethods:
  - :init-scene setup a scene, the callee will receive the parent stage
  - :cleaup-scene the callee should clean everything like input handlers,
  containers etc"
  [scene]
  (swap! director_ (fn [director]
                     (when-let [old-scene (:current-scene director)]
                       (scene-cleanup old-scene))
                     (scene-init scene (:app-stage director))
                     (assoc director :current-scene scene))))
