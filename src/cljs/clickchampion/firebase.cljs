(ns clickchampion.firebase
  (:require [re-frame.core :as re-frame]
            [clickchampion.events :as events]))

(defn add-firebase-auth-listener! []
  (.onAuthStateChanged
  (.auth js/firebase)
  (fn auth-state-changed [user-obj]
    (if (nil? user-obj)
      (do
        (re-frame/dispatch [::events/set-user-uid nil])
        (re-frame/dispatch [::events/set-logged-in false]))
      (do
        (re-frame/dispatch [::events/set-logged-in true])
        (re-frame/dispatch [::events/set-user-uid (.-uid user-obj)]))))
  (fn auth-error [error] (js/console.log error))))