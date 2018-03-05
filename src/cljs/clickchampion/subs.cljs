(ns clickchampion.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::username
 (fn [db]
   (:username db)))
   
(re-frame/reg-sub
 ::user-uid
 (fn [db]
   (:user-uid db)))

(re-frame/reg-sub
 ::logged-in
 (fn [db]
   (:logged-in db)))

(re-frame/reg-sub
 ::clicks
 (fn [db]
   (:clicks db)))
