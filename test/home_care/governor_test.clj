(ns home-care.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [home-care.store :as store]
            [home-care.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :consented-at "2026-01-01"})
    (store/register-care-plan! st {:client-id "client-1" :protocol "daily-living-support"})
    st))

(deftest proceeds-on-clean-routine-visit
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :client-id "client-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-care-plan
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :client-id "no-such-client" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-care-plan (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :client-id "client-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest lifting-transfer-assist-always-escalates
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :lift :client-id "client-1" :safety-class :none
                   :effect :propose :confidence 1.0}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :lifting-transfer-assist (:reason result)))))

(deftest human-approval-on-high-safety-class-even-when-clean
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :client-id "client-1" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :client-id "client-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-visit! st {:visit-id "v1" :client-id "client-1" :tasks [:bathing-assist]})
    (store/record-lift-event! st {:lift-event-id "l1" :client-id "client-1"})
    (is (= 1 (count (store/visits-of st "client-1"))))
    (is (= 1 (count (store/lift-events-of st "client-1"))))
    (is (empty? (store/visits-of st "client-2")))))
