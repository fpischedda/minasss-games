(ns minasss-games.plot
  "Here is a way to define complex plots like the one described below
  ```
  [[:fade-in :time 1000 :actor :scene]
  [:after :time 500
  :then [
    [:wait-all :actions
      [:move :actor :bob :from [0 100] :to [100 100] :time 1000]
      [:move :actor :alice :from [300 100] :to [110 100] :time 900]
      :then [:show :target :menu]
    ]]]]
  ```
  To make it possible to use different backends the plot should work
  independently of the pixi code (i.e. updating containers, sprite etc),
  instead it should provide a callback mechanism with at least three events:
  - start: subscribers will receive the initial context (whatever it means)
  - step: subscribers will receive the step update (for example current pos)
  - finish: subscribers will receive the last state and maybe the next action

  Event listeners will receive event-type, action and a payload which may be
  different for each kind of actions, as a convention the ::start payload
  will contain the initial state and the actor (if it makes sense), ::step and
  ::finish will contain the actor, old-state and state.

  This protocol is not well defined yet and will probably change in the
  future."
  (:require [minasss-games.math :as math]))

;; Replicating the action namespace for now, to not break existing
;; existing functionality

(def actions-registry_ (atom (list)))

(defn register-action
  [action]
  (swap! actions-registry_ conj action))

(defmulti updater
  (fn [action _delta-time] (:action action)))

(defn update-actions
  [delta-time]
  (swap! actions-registry_
    (fn [action]
      (->> action
        (map #(updater % delta-time))
        (filter some?)
        doall))))

(defmethod updater ::move
  [action delta-time]
  (let [{:keys [actor position target-position direction prev-distance speed]} (:params action)
        new-position {:x (+ (:x position) (* delta-time speed (:x direction)))
                      :y (+ (:y position) (* delta-time speed (:y direction)))}
        distance (math/length (math/direction target-position new-position))
        listener (:listener action)
        listener-payload {:actor actor
                          :old-state {:position position}
                          :state {:position new-position}}]
    (if (> distance prev-distance)
      ;; lifecycle of this action finishes here;
      ;; if :dispatcher is set call dispatch function with the latest state
      ;; of the action
      (do
        (listener ::finish (:action action) listener-payload)
        ;; return the next action if any
        ;; :then should be associated to a function that returns a new action
        (when-let [next-action (:then action)]
          (next-action)))
      ;; this action has not finished yet, in updates the target and
      ;; prepares the state for next iteration
      (do
        (listener ::step (:action action) listener-payload)
        ;; return the current state of the action for the next iteration
        (update-in action [:params] assoc :position new-position :prev-distance distance)))))

(defn move
  []
  )

(defmulti make-action (fn [action _dispatcher _options]) action)

(defmethod make-action ::move
  [_action listener options]
  (let [{:keys [actor from to speed]} (apply hash-map options) ;; options are provided as a vector but it is pratical to access them as a map
        dir (math/direction to from)
        normalized-dir (math/normalize dir)
        prev-distance (math/length dir)
        then (when-let options)]
    (listener ::start action {:actor actor
                              :state {:position from}})
    (register-action
      {:action ::move
       :listener listener
       :params {:actor actor
                :position from
                :target-position to
                :direction normalized-dir
                :prev-distance prev-distance
                :speed speed}})))

(defn make-plot
  "A plot is a tree like structure defined as a vector;
  first item represent the action; the rest of the vector represents
  options specified as pairs like :time 500, :actor :bob and so on"
  [plot-spec listener])
