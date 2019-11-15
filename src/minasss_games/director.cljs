(ns minasss-games.director
  "The director should help to coordinate transitions between different
  scenes, providing a way to start a new scene and clean everything
  afterwards.

  A scene is a map containing a dispatching keyword :id used to call:
  - scene-init: used to initialize scene context, it will receive the scene
    and the applications stage
  - scene-ready: called either after resourses have been loaded or right
    away if no resources are requested by the scene
  - scene-cleanup: called as an opportunity to clean current scene like resetting
    scene status, remove graphics objects from the stage and so on

  If the scene defines a :resources key its value it is expected to be a vector
  and used as the `resources` parameter to call `pixi/load-resources` with
  `(scene-ready :scene-id app-stage)` as the `loaded` callback"
  (:require [minasss-games.pixi :as pixi]
            [minasss-games.pixi.settings :as settings]
            [minasss-games.screenplay :as screenplay]
            [minasss-games.tween :as tween]
            [oops.core :refer [oget]]))

(def director_ (atom {}))

(def TargetFPMS (settings/get-by-name "TARGET_FPMS"))

(defn ^:export update-step
  "update view related stuff
  ticker callback will receive a parameter which is referred as
  delta time by PIXI documentation; but this name is missleading because
  instead of time since last update it refers to `frame time` since last frame
  this means that if we target 60 FPS and we effectively have 60 FPS,
  delta-time will have the value 1, if we have 30 FPS delta-time will have
  the value 2; to make things more clear PIXI delta time will be called
  `delta-frame`.
  If we are interested in `real` delta-time we can scale delta-frame with
  TargetFPMS: target frames per millisecond"
  [delta-frame]
  (let [dt-ms (* delta-frame TargetFPMS)]
    (screenplay/update-actions dt-ms)
    (tween/update-tweens dt-ms)))

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

(defmulti scene-init (fn [scene _stage] (:id scene)))

(defmethod scene-init :default
  [scene _]
  (println "no scene-init defined for this scene:" scene))

(defmulti scene-ready (fn [scene _stage] (:id scene)))

(defmethod scene-ready :default
  [scene _]
  (println "no scene-ready defined for this scene:" scene))

(defmulti scene-cleanup :id)

(defmethod scene-cleanup :default
  [scene]
  (println "no scene-cleanup defined for this scene:" scene))

(defn load-scene-resources
  "Load scene defined resources and setup a callback to call
  to scene-ready <:scene-id>"
  [resources scene parent-stage]
  (pixi/load-resources resources (partial scene-ready scene parent-stage)))

(defn start-scene
  "Provided scene is set as the current one, if an old scene is running,
  scene-cleanup will be called on previous scene."
  [scene]
  (swap! director_ (fn [{:keys [current-scene app-stage] :as director}]
                     (when current-scene
                       (scene-cleanup current-scene))
                     (scene-init scene app-stage)
                     ;; eventually load resources defined in the scene.
                     ;; after loading, scene-ready :scene-id will be called.
                     ;; if no resources are defined for the scene,
                     ;; scene-ready :scene-id will be called right away
                     (if-let [resources (:resources scene)]
                       (load-scene-resources resources scene app-stage)
                       (scene-ready scene app-stage))
                     (assoc director :current-scene scene))))
