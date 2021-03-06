(ns minasss-games.screenplay
  "Here is a way to define complex screenplays like the one described below
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
  To make it possible to use different backends the screenplay should work
  independently of the pixi code (i.e. updating containers, sprite etc),
  instead it should provide a callback mechanism with at least three events:
  - start: subscribers will receive the initial context (whatever it means)
  - step: subscribers will receive the step update (for example current pos)
  - finish: subscribers will receive the last state and maybe the next action

  Event listeners will receive event-type, action and a payload which may be
  different for each kind of actions, as a convention the ::start payload
  will contain the initial state and the actor (if it makes sense), ::step and
  ::finish will contain the actor, old-state and (current) state.

  This protocol is not well defined yet and will probably change in the
  future."
  (:require [minasss-games.math :as math]))

(def clog js/console.log)

;; Replicating the action namespace for now, to not break existing
;; functionality

(defmulti make-action (fn [[action &_options] _listener] action))

(defn start
  "A screenplay is a tree like structure defined as a vector;
  first item represent the action; the rest of the vector represents
  options specified as pairs like :time 500, :actor :bob and so on"
  [screenplay listener]
  (if (keyword? (first screenplay))
    (make-action screenplay listener)
    (dorun (map #(make-action % listener) screenplay))))

(def actions-registry_ (atom (list)))
(def active-actions-registry_ (atom (list)))

(defn clean-actions
  "remove all actions from the registry"
  []
  (reset! active-actions-registry_ (list))
  (reset! actions-registry_ (list)))

(defn register-action
  ([action-name listener then params]
   (register-action
     {:action action-name
      :listener listener
      :then (when (some? then) (partial start then listener))
      :params params}))

  ([action]
   (swap! actions-registry_ conj action)))

(defmulti updater
  (fn [action _delta-time] (:action action)))

(defn update-actions
  [delta-time]
  (swap! active-actions-registry_
    (fn [action]
      (->> action
        (map #(updater % delta-time))
        (filter some?)
        doall)))
  (swap! active-actions-registry_ concat @actions-registry_)
  (reset! actions-registry_ (list)))

(defmethod updater ::move
  [action delta-time]
  (let [{:keys [actor from to position direction elapsed time]} (:params action)
        new-elapsed (+ elapsed delta-time)
        listener (:listener action)]
    (if (>= new-elapsed time)
      ;; lifecycle of this action finishes here;
      ;; if :listener is set, call it with the latest state and ::finish
      ;; event type
      (do
        (listener ::finish (:action action) {:actor actor
                                             :old-state {:position position}
                                             :state {:position to}})
        ;; return the next action if any
        ;; :then should be associated to a function that registers a new action
        (when-let [next-action (:then action)]
          (next-action)
          nil))
      ;; this action has not finished yet, in updates the target and
      ;; prepares the state for next iteration
      (do
        (let [new-position (->> (/ new-elapsed time)
                             (math/scale direction )
                             (math/translate from ))]
          (listener ::step (:action action) {:actor actor
                                             :old-state {:position position}
                                             :state {:position new-position}})
          ;; return the current state of the action for the next iteration
          (update-in action [:params] assoc :position new-position :elapsed new-elapsed))))))

(defmethod updater ::after
  [action delta-time]
  (let [time (get-in action [:params :time])
        new-time (- time delta-time)
        listener (:listener action)
        listener-payload {:old-state {:time time}
                          :state {:time new-time}}]
    (if (>= 0 new-time)
      (do
        (listener ::finish (:action action) listener-payload)
        (when-let [next-action (:then action)]
          (next-action)
          nil))
      (do
        (listener ::step (:action action) listener-payload)
        ;; return the current state of the action for the next iteration
        (update-in action [:params] assoc :time new-time)))))

(defmethod updater ::scale
  [{:keys [action params then listener] :as action-state} delta-time]
  (let [{:keys [actor from to scale diff elapsed time]} params
        new-elapsed (+ elapsed delta-time)]
    (if (>= new-elapsed time)
      (do
        (listener ::finish action {:actor actor
                                   :old-state {:scale scale}
                                   :state {:scale to}})
        (when (some? then)
          (then)
          nil))
      (do
        (let [new-scale (+ from (* diff (/ new-elapsed time)))]
          (listener ::step action {:actor actor
                                   :old-state {:scale scale}
                                   :state {:scale new-scale}})
          ;; return the current state of the action for the next iteration
          (update-in action-state [:params] assoc
            :elapsed new-elapsed :scale new-scale))))))

;; after action just waits until the specified amount of time passes
;; and eventually continue with the actions specified in the :then
;; option
(defmethod make-action ::after
  [[action & options] listener]
  (let [{:keys [time then]} (apply hash-map options)
        initial-state {:time time}]
    (listener ::start action {:state initial-state})
    (register-action action listener then initial-state)))

(defmethod make-action ::move
  [[action & options] listener]
  (let [{:keys [actor from to time then]} (apply hash-map options) ;; options are provided as a vector but it is pratical to access them as a map
        dir (math/direction to from)]
    (listener ::start action {:actor actor
                              :state {:position from}})
    (register-action action listener then {:actor actor
                                           :from from
                                           :to to
                                           :position from
                                           :direction dir
                                           :elapsed 0.0
                                           :time time})))

(defmethod make-action ::scale
  [[action & options] listener]
  (let [{:keys [actor from to time then]} (apply hash-map options)]
    (listener ::start action {:actor actor :state {:scale from}})
    (register-action action listener then {:actor actor
                                           :from from
                                           :to to
                                           :diff (- to from)
                                           :scale from
                                           :elapsed 0.0
                                           :time time})))

;; this action is a bit particular, it is meant to be used to set
;; actor properties once so the only event triggered by this
;; action is ::start, so there is also no need to register itself
(defmethod make-action ::set-attributes
  [[action & options] listener]
  (let [options-map (apply hash-map options)
        actor (:actor options-map)
        attributes (dissoc options-map :actor)]
    (listener ::start action {:actor actor :state attributes})))
