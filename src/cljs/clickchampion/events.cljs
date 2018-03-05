(ns clickchampion.events
  (:require [re-frame.core :as re-frame]
            [clickchampion.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-user-uid
 (fn [db [_ uid]]
   (assoc db :user-uid uid)))

(re-frame/reg-event-db
 ::set-logged-in
 (fn [db [_ logged-in]]
   (assoc db :logged-in logged-in)))

(re-frame/reg-event-db
 ::set-username
 (fn [db [_ username]]
   (assoc db :username username)))
   
(re-frame/reg-event-db
 ::set-clicks
 (fn [db [_ clicks]]
   (assoc db :clicks clicks)))
