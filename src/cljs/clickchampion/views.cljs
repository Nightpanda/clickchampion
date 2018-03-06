(ns clickchampion.views
  (:require [re-frame.core :as re-frame]
            [clickchampion.subs :as subs]
            [clickchampion.events :as events]
            [reagent.core :as r]))

(def db (.database js/firebase))

(defn db-ref [path] (.ref db (str "/" path)))

(defn save-clicks [uid clicks] (.set (db-ref (str "clicks/" uid)) clicks))

(defn save-username-uid [uid username] (.set (db-ref (str "users/" uid)) username))


(defn child-added [path f]
 (let [ref (db-ref path)
       a (r/atom nil)]
   (.on ref "child_added" (fn [x]
                      (reset! a (.val x))))
   (r/create-class
     {:display-name "listener"
      :component-will-unmount
      (fn will-unmount-listener [this]
        (.off ref))
      :reagent-render
      (fn render-listener [args]
        (into [f a] args))})))

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

(defn get-username! []
  (let [uid (re-frame/subscribe [::subs/user-uid])]
    (.once (db-ref (str "users/" @uid))
      "value"
      (fn received-db [snapshot]
        (re-frame/dispatch [::events/set-username (.val snapshot)])))))

(defn get-clicks! [uid]
  (.once (db-ref (str "clicks/" uid))
    "value"
    (fn received-db [snapshot]  
      (re-frame/dispatch [::events/set-clicks (.val snapshot)]))))

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
    (.updateProfile (.-currentUser (.auth js/firebase)) (js-obj "displayName" @username))
    (fn []
      [:div
        [:p "Welcome back! " @username]
          [on (str "clicks/" @user-uid) 
            (fn [a]
              (let [current-clicks (if (nil? @a) 0 @a)]
                [:div "Your clicks: " current-clicks]))]
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
      "Sign in with Google!"]]))

(def no-username-view
  (let [new-username (r/atom "")
        user-uid (re-frame/subscribe [::subs/user-uid])]
    (fn []
      [:div 
        [:div
          "Welcome newbie. Please choose a username."
          [:input {:type "text" :id "username-input" :on-change #(reset! new-username (-> % .-target .-value))}]
          [:button {:on-click (fn [] 
                                (.updateProfile (.-currentUser (.auth js/firebase)) (js-obj "displayName" @new-username))
                                (re-frame/dispatch [::events/set-username @new-username])
                                (save-username-uid @user-uid @new-username))} "Save"]]
        logout-button])))

(defn leaderboard []
    (fn []
      [:div
        [on (str "users/") 
              (fn [a]
                (let [users (js->clj @a)
                      ]
                  (if (some? users)
                    [:ul
                      (for [user users]
                        (let [uid (key user)]
                          ^{:key user} [:li "Name " user 
                                            [on (str "clicks/" uid) 
                                              (fn [a]
                                                (let [current-clicks (if (nil? @a) 0 @a)]
                                                  [:div " Score "  current-clicks]))]]))])))]]))

(defn main-panel []
  (let [username (re-frame/subscribe [::subs/username])
        logged-in (re-frame/subscribe [::subs/logged-in])
        uid (re-frame/subscribe [::subs/user-uid])]
    (fn []
      [:div 
        (if @logged-in
          (do 
            (get-username!)
            (get-clicks! @uid)
            (if (nil? @username)
              [no-username-view]
              [logged-in-view]))
          [logged-out-view])
        [leaderboard]])))
        
