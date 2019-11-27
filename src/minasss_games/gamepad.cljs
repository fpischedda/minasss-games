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
  (let [buttons (oget gamepad "buttons")]
    {:buttons (mapv #(oget % "pressed") buttons)}))

(defn gamepad-connected
  "Register new connected gamepad in the gamepads_ registry"
  [event]
  (let [gamepad (oget event "gamepad")
        status (gamepad-status gamepad)]
    (swap! gamepads_ dissoc (gamepad-id gamepad) {:gamepad gamepad
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

(defn button-pressed
  [gamepad-index button-index]
  (-> @gamepads_
    gamepad-index
    :status
    :buttons
    (nth button-index)))

(defn init
  "Initialize gamepad system, register connected and disconnected event handlers"
  []
  (reset! gamepads_ {})
  (.addEventListener js/window "gamepadconnected" gamepad-connected)
  (.addEventListener js/window "gamepaddisconnected" gamepad-disconnected))

(comment
  (init))
