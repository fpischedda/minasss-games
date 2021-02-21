(ns minasss-games.gamepad
  "This namespace contains functions to interact with the gamepad
  using the [Gamepad API](https://developer.mozilla.org/en-US/docs/Web/API/Gamepad_API)
  This API exposes two Window event handlers three polling interfaces to query
  gamepad's status:
  - Window.gamepadconnected
  - Window.gamepaddisconnected
  - Gamepad interface
  - GamepadButton interface
  - GamepadEvent interface (event object received by game(dis)connected handlers)
  See linked documentation for details."
  (:require [oops.core :refer [oget]]))

(def gamepads_ (atom {}))

(defn gamepad-id
  [gamepad]
  (oget gamepad "index"))

(defn gamepad-status
  "Return a map with the current status of the provided gamepad; right now
  only buttons are supported and returned in the :buttons key"
  [gamepad]
  (let [buttons (oget gamepad "buttons")
        axes (oget gamepad "axes")]
    {:buttons (mapv #(oget % "pressed") buttons)
     :axes (mapv #(oget % "value") axes)}))

(defn gamepad-connected
  "Register new connected gamepad in the gamepads_ registry"
  [event]
  (let [gamepad (oget event "gamepad")
        status (gamepad-status gamepad)]
    (swap! gamepads_ assoc (gamepad-id gamepad) {:gamepad gamepad
                                                  :old-status status
                                                  :status status})))

(defn gamepad-disconnected
  "Remove disconnected gamepad from gamepads_ registry"
  [event]
  (let [gamepad (oget event "gamepad")]
    (swap! gamepads_ dissoc (gamepad-id gamepad))))

(defn update-gamepads
  "Update the status of all connected gamepads, store previous status in the
  :old-status key"
  []
  (swap! gamepads_ #(reduce-kv (fn [new-gamepads id status]
                                 (let [gamepad (:gamepad status)
                                       old-status (:status status)
                                       status (gamepad-status gamepad)]
                                   (assoc new-gamepads id {:gamepad gamepad
                                                           :old-status old-status
                                                           :status status}))) {} %)))

(defn button-status
  [gamepad-index button-index which-status]
  (if-let [gamepad (get @gamepads_ gamepad-index)]
    (-> gamepad
      which-status
      :buttons
      (nth button-index))
    false))

(defn button-down
  [gamepad-index button-index]
  (button-status gamepad-index button-index :status))

(defn button-up
  [gamepad-index button-index]
  (not (button-status gamepad-index button-index :status)))

(defn button-pressed
  [gamepad-index button-index]
  (let [state (button-status gamepad-index button-index :status)
        old-state (button-status gamepad-index button-index :old-status)]
    (and (not old-state) state)))

(defn button-released
  [gamepad-index button-index]
  (let [state (button-status gamepad-index button-index :status)
        old-state (button-status gamepad-index button-index :old-status)]
    (and old-state (not state))))

(defn axis-status
  ([gamepad-index axis-index]
   (axis-status gamepad-index axis-status :status))
  ([gamepad-index axis-index which-status]
   (if-let [gamepad (get @gamepads_ gamepad-index)]
     (-> gamepad
       which-status
       :axis
       (nth axis-index))
     0)))

(defn init
  "Initialize gamepad system, register connected and disconnected event handlers"
  []
  (reset! gamepads_ {})
  (.addEventListener js/window "gamepadconnected" gamepad-connected)
  (.addEventListener js/window "gamepaddisconnected" gamepad-disconnected))

(comment
  (init))
