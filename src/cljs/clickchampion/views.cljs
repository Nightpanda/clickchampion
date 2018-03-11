(ns clickchampion.views
  (:require [re-frame.core :as re-frame]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.icons :as ic]
            [cljs-react-material-ui.reagent :as ui]
            [clickchampion.subs :as subs]
            [clickchampion.events :as events]
            [reagent.core :as r]
            [re-com.core :as re-com]
            [clickchampion.firebase :refer [add-firebase-auth-listener!]]))

(def db (.database js/firebase))

(defn db-ref [path] (.ref db (str "/" path)))

(defn save-clicks [uid clicks] (.set (db-ref (str "users/" uid "/clicks")) clicks))

(defn save-username-uid [uid username] (.set (db-ref (str "users/" uid "/username")) username))

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

(defn firebase-get-value [path]
  )

(defn get-username! []
  (let [uid (re-frame/subscribe [::subs/user-uid])]
    (.once (db-ref (str "users/" @uid "/username"))
      "value"
      (fn received-db [snapshot]
        (re-frame/dispatch [::events/set-username (.val snapshot)])))))

(defn get-clicks! [uid]
  (.once (db-ref (str "users/" uid "/clicks"))
    "value"
    (fn received-db [snapshot]  
      (re-frame/dispatch [::events/set-clicks (.val snapshot)]))))

(defn provider []  (new js/firebase.auth.GoogleAuthProvider))

(defn google-sign-in []
  (.signInWithPopup (.auth js/firebase) (provider)))

(defn logout-auth []
  (.signOut (.auth js/firebase)))

(def logout-button
  [re-com/button :on-click (fn [] (logout-auth))
   :label "Logout"])

(defn logged-in-view []
  (let [username (re-frame/subscribe [::subs/username])
        user-uid (re-frame/subscribe [::subs/user-uid])
        clicks (re-frame/subscribe [::subs/clicks])]
    (.updateProfile (.-currentUser (.auth js/firebase)) (js-obj "displayName" @username))
    (fn []
        [:div.row
          [ui/card [ui/card-title "Welcome back! " ]
          [ui/card-text @username]
          [on (str "users/" @user-uid "/clicks") 
            (fn [a]
              (let [current-clicks (if (nil? @a) 0 @a)]
                (re-frame/dispatch [::events/set-clicks current-clicks])
                [ui/card-text (str "Your clicks: " current-clicks)]))]
          
        [ui/card-actions [re-com/md-circle-icon-button :on-click 
                    (fn []
                      (save-clicks @user-uid (inc @clicks)))
        :md-icon-name "zmdi-plus"]
        ]
        logout-button]
       ])))

(defn logged-out-view []
  (fn []
    [ui/card
      [ui/card-actions
     [:button.btn.btn-primary {:on-click (fn [] (google-sign-in))}
       "Sign in with Google!"]]]))

(def no-username-view
  (let [new-username (r/atom "")
        user-uid (re-frame/subscribe [::subs/user-uid])]
    (fn []
        [ui/card 
          [ui/card-header "Welcome newbie. Please choose a username."]
          [ui/card-text 
            [:input.form-control
              {:on-change #(reset! new-username (-> % .-target .-value))
              }]
          ]
          [ui/card-actions
            [re-com/button :on-click (fn [] 
                                (.updateProfile (.-currentUser (.auth js/firebase)) (js-obj "displayName" @new-username))
                                (re-frame/dispatch [::events/set-username @new-username])
                                (save-username-uid @user-uid @new-username)
                                (save-clicks @user-uid 0)) 
                          :label "Save"]
                          logout-button]
          
          ]
        )))

(defn leaderboard []
    (fn []
          [ui/card 
            [ui/card-header "Leaderboard"]
            [ui/card-text 
              [on (str "users/")
                (fn [a]
                  (let [users (js->clj @a :keywordize-keys true)]
                    (if (some? users)
                      [ui/list 
                        (for [user (reverse (sort-by :clicks (map #(val %) users)))]
                          (let [clicks (:clicks user)
                                username (:username user)]
                            ^{:key (random-uuid)} [ui/list-item username " " clicks]))])))]]]))

(defn main-panel []
  (let [username (re-frame/subscribe [::subs/username])
        logged-in (re-frame/subscribe [::subs/logged-in])
        uid (re-frame/subscribe [::subs/user-uid])]
    (add-firebase-auth-listener!)
    (fn []
    [ui/mui-theme-provider
      {:mui-theme (get-mui-theme {:palette {:text-color (color :blue200)}})}
      [:div.container-fluid
        [:div.row
          [:div.col-md-8
            (if @logged-in
              (do 
                (get-username!)
                (get-clicks! @uid)
                (if (nil? @username)
                  [no-username-view]
                  [logged-in-view]))
              [logged-out-view])]
          [:div.col-md-4 
            [leaderboard]]]]])))
        
