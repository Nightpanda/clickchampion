(ns clickchampion.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [clickchampion.subs :as subs]
            [clickchampion.events :as events]
            [reagent.core :as r]
            ))

(def db (.database js/firebase))

(defn db-ref [path] (.ref db (str "/" path)))

(defn save-clicks [uid clicks] (.set (db-ref (str "clicks/" uid)) clicks))

(defn save-username-uid [uid username] (.set (db-ref (str "users/" uid)) username))

(defn on [path f]
 (let [ref (db-ref path)
       a (r/atom nil)]
   (.on ref "value" (fn [x]
                      (reset! a (.val x))))
   (r/create-class
     {:display-name "listener"
      :component-will-unmount
      (fn will-unmount-listener [this]
        (.off ref))
      :reagent-render
      (fn render-listener [args]
        (into [f a] args))})))

(defn get-username []
  (let [uid (re-frame/subscribe [::subs/user-uid])]
    (.once (db-ref (str "users/" @uid))
      "value"
      (fn received-db [snapshot]
        (re-frame/dispatch [::events/set-username (.val snapshot)])))))

(defn get-clicks []
  (let [uid (re-frame/subscribe [::subs/user-uid])]
    (.once (db-ref (str "clicks/" @uid))
      "value"
      (fn received-db [snapshot]  
        (re-frame/dispatch [::events/set-clicks (.val snapshot)])))))

(.onAuthStateChanged
  (.auth js/firebase)
  (fn auth-state-changed [user-obj]
    (if (nil? user-obj)
      (do
        (re-frame/dispatch [::events/set-user-uid nil])
        (re-frame/dispatch [::events/set-logged-in false]))
      (do
        (re-frame/dispatch [::events/set-logged-in true])
        (re-frame/dispatch [::events/set-user-uid (.-uid user-obj)])
        (get-username)
        (get-clicks)
        (let [username (re-frame/subscribe [::subs/username])]
          (.updateProfile user-obj (js-obj "displayName" @username))))))
  (fn auth-error [error] (js/console.log error)))

(defn provider []  (new js/firebase.auth.GoogleAuthProvider))

(defn google-sign-in []
  (.signInWithPopup (.auth js/firebase) (provider)))

(defn logout-auth []
  (.signOut (.auth js/firebase)))

(def logout-button
  [:button {:on-click (fn [] (logout-auth))}
   "Logout"])

(defn logged-in-view []
  (let [username (re-frame/subscribe [::subs/username])
        user-uid (re-frame/subscribe [::subs/user-uid])
        clicks (re-frame/subscribe [::subs/clicks])]
    (fn []
      [:div
        [:p "Welcome back! " @username]
        (if (nil? @clicks)
          [:div "Your clicks: " 0]
          [on (str "clicks/" @user-uid) 
            (fn [a]
              [:div "Your clicks: " @a]
              )])
        [:button {:on-click 
                    (fn []
                      (do (re-frame/dispatch [::events/set-clicks (inc @clicks)])
                      (save-clicks @user-uid (inc @clicks))))}
        "Click me"]
       logout-button])))

(defn logged-out-view []
  (fn []
    [:div
     [:button {:on-click (fn []
                           (google-sign-in))}
      "Sign in with Google!"]
     [:button {:on-click (fn []
                           (save-clicks "rUpQthyR4yVqnpGcCOpmyOrJpyx2" 1337))} "Unauthorized click"]]))

(defn title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :label (str "Hello from " @name)
     :level :level1]))

(defn main-panel []
  (let [username (re-frame/subscribe [::subs/username])
        logged-in (re-frame/subscribe [::subs/logged-in])]
    (fn []
      (if @logged-in
        [logged-in-view]
        [logged-out-view]))))
