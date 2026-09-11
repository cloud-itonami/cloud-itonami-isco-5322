(ns home-care.store
  "SSoT for the ISCO-08 5322 independent home-based-personal-care
  sole-proprietor actor, behind a `Store` protocol so the backend is a swap
  (MemStore default ‖ a real Datomic/kotoba-server backend, per the itonami
  actor pattern).

  Domain = independent home-based personal care practice:

    client     — a client under care (clientId, consentedAt)
    care-plan  — the daily-living support plan for a client (planId,
                 clientId, protocol)
    visit      — a scheduled/completed home visit (visitId, clientId, tasks)
    lift-event — a lifting/transfer-assist event (liftEventId, clientId,
                 performedBy #{:robot :caregiver})

  The append-only records are the operating ledger: a visit or lift event
  must reference a registered client with a registered care plan, and
  visits/lift-events are never mutated in place, only appended.")

(defprotocol Store
  (client [st client-id])
  (care-plan-of [st client-id])
  (visits-of [st client-id])
  (lift-events-of [st client-id])
  (register-client! [st client])
  (register-care-plan! [st care-plan])
  (record-visit! [st visit])
  (record-lift-event! [st lift-event]))

(defrecord MemStore [state]
  Store
  (client [_ client-id]
    (get-in @state [:clients client-id]))
  (care-plan-of [_ client-id]
    (get-in @state [:care-plans client-id]))
  (visits-of [_ client-id]
    (filter #(= client-id (:client-id %)) (:visits @state)))
  (lift-events-of [_ client-id]
    (filter #(= client-id (:client-id %)) (:lift-events @state)))
  (register-client! [_ client]
    (swap! state assoc-in [:clients (:client-id client)] client))
  (register-care-plan! [_ care-plan]
    (swap! state assoc-in [:care-plans (:client-id care-plan)] care-plan))
  (record-visit! [_ visit]
    (swap! state update :visits (fnil conj []) visit))
  (record-lift-event! [_ lift-event]
    (swap! state update :lift-events (fnil conj []) lift-event)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:clients {} :care-plans {} :visits [] :lift-events []} seed)))))
