(ns home-care.governor
  "HomeCareGovernor — the independent safety/traceability layer for the
  ISCO-08 5322 independent home-based-personal-care actor. The Care Advisor
  proposes actions (visit, lifting/transfer assist); it has no notion of
  care-plan provenance or lifting risk, so this MUST be a separate system
  able to *reject* a proposal and fall back to HOLD — the itonami-actor
  pattern (independent Governor gates a proposing actor) applied to this
  occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. Lifting/transfer-assist actions
  ALWAYS require human sign-off, even when every hard invariant passes —
  a lifting/transfer robot never operates unsupervised.

  HARD invariants for :care/propose:
    1. Care-plan provenance — a visit or lift event must reference a
       registered client with a registered care plan.
    2. No-actuation         — the proposal must not directly mutate a visit
       or lift-event record outside the record-visit!/record-lift-event!
       path (effect must be :propose, never a raw store write).
  SOFT:
    3. Lifting/transfer assist always escalates to human sign-off (no
       autonomous lift/transfer, robot or otherwise).
    4. Confidence floor → escalate."
  (:require [home-care.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [care-plan-fn]} proposal]
  (let [{:keys [client-id effect]} proposal
        care-plan (care-plan-fn client-id)]
    (cond-> []
      (nil? care-plan)
      (conj {:rule :no-care-plan :detail (str "未登録 care-plan " client-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:care-plan-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)
        lifting? (= :lift (:action proposal))]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      lifting?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :lifting-transfer-assist}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `home-care.store/Store` implementation."
  [store]
  {:care-plan-fn #(store/care-plan-of store %)})
