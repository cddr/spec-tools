(ns spec-tools.avro-schema-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [spec-tools.spec :as spec]
            [spec-tools.data-spec :as ds]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]
            [spec-tools.avro-schema :as asc]
    #?(:clj
            [scjsv.core :as scjsv])))

(s/def ::integer (st/spec integer?))
(s/def ::string (st/spec string?))
(s/def ::named-string (st/spec {:spec string?, :name "myString"}))

(s/def ::my-record (st/spec {:spec (s/keys :req-un [::string])
                            :name "MyRecord"}))

(deftest simple-spec-test
  (testing "primitive predicates"
    ;; You're intented to call jsc/to-json with a registered spec, but to avoid
    ;; boilerplate, we do inline specization here.
    ;; (is (= (asc/transform (s/spec int?)) {:type "int"}))
    (is (= (asc/transform (s/spec integer?)) {:type "long"}))
    (is (= (asc/transform (s/spec float?)) {:type "float"}))
    (is (= (asc/transform (s/spec double?)) {:type "double"}))
    (is (= (asc/transform (s/spec string?)) {:type "string"}))
    (is (= (asc/transform (s/spec boolean?)) {:type "boolean"}))
    (is (= (asc/transform (s/spec nil?)) {:type "null"})))
    ;; (is (= (asc/transform (with-meta #{"a" "b" "c"}
    ;;                         {:avro.enum/name "MyEnum"}) {:type "enum"
    ;;                                                      :name "MyEnum"
    ;;                                                      :symbols ["a" "b" "c"]})))))
           
  (testing "clojure.spec predicates"
    (is (= (asc/transform (s/nilable ::string)) [{:type "null"} {:type "string"}]))
    #_(is (= (jsc/transform (s/int-in 1 10)) {:allOf [{:type "integer" :format "int64"} {:minimum 1 :maximum 10}]})))
  (testing "simple specs"
    (is (= (asc/transform ::integer) {:type "long"}))
    #_(is (= (jsc/transform ::set) {:enum [1 3 2]})))

  (testing "clojure.specs"
    ;; clojure.spec's concept of required/optional keys does not really map well to avro since
    ;; all described fields will be present in the serialized object
    ;; (even though some of their values may be permitted to be "null")
    (is (= (asc/transform (st/spec {:spec (s/keys :req-un [::integer ::string ::named-string])
                                    :description "A test record"
                                    :name "MyRecord"}))
           {:type "record"
            :name "MyRecord"
            :doc "A test record"
            :fields [{:name "integer", :type "long"}
                     {:name "string", :type "string"}
                     {:name "myString", :type "string"}]}))
  
    (is (= (asc/transform ::my-record)
           {:type "record",
            :name "MyRecord"
            :fields [{:name "string", :type "string"}]}))
  
    (is (= (asc/transform (s/or :int integer? :string string?))
           [{:type "long"}, {:type "string"}]))
    
;;     (is (= (jsc/transform (s/and integer? pos?))
;;            {:allOf [{:type "integer"} {:minimum 0 :exclusiveMinimum true}]}))
;;     (is (= (jsc/transform (s/merge (s/keys :req [::integer])
;;                                    (s/keys :req [::string])))
;;            {:type "object"
;;             :properties {"spec-tools.json-schema-test/integer" {:type "integer"}
;;                          "spec-tools.json-schema-test/string" {:type "string"}}
;;             :required ["spec-tools.json-schema-test/integer" "spec-tools.json-schema-test/string"]}))
;;     (is (= (jsc/transform (s/every integer?)) {:type "array" :items {:type "integer"}}))
;;     (is (= (jsc/transform (s/every-kv string? integer?))
;;            {:type "object" :additionalProperties {:type "integer"}}))
;;     (is (= (jsc/transform (s/coll-of string?)) {:type "array" :items {:type "string"}}))
;;     (is (= (jsc/transform (s/coll-of string? :into '())) {:type "array" :items {:type "string"}}))
;;     (is (= (jsc/transform (s/coll-of string? :into [])) {:type "array" :items {:type "string"}}))
;;     (is (= (jsc/transform (s/coll-of string? :into #{})) {:type "array" :items {:type "string"}, :uniqueItems true}))
;;     (is (= (jsc/transform (s/map-of string? integer?))
;;            {:type "object" :additionalProperties {:type "integer"}}))
;;     (is (= (jsc/transform (s/* integer?)) {:type "array" :items {:type "integer"}}))
;;     (is (= (jsc/transform (s/+ integer?)) {:type "array" :items {:type "integer"} :minItems 1}))
;;     (is (= (jsc/transform (s/? integer?)) {:type "array" :items {:type "integer"} :minItems 0}))
;;     (is (= (jsc/transform (s/alt :int integer? :string string?))
;;            {:anyOf [{:type "integer"} {:type "string"}]}))
;;     (is (= (jsc/transform (s/cat :int integer? :string string?))
;;            {:type "array"
;;             :items {:anyOf [{:type "integer"} {:type "string"}]}}))
;;     ;; & is broken (http://dev.clojure.org/jira/browse/CLJ-2152)
;;     (is (= (jsc/transform (s/tuple integer? string?))
;;            {:type "array" :items [{:type "integer"} {:type "string"}]}))
;;     ;; keys* is broken (http://dev.clojure.org/jira/browse/CLJ-2152)
;;     (is (= (jsc/transform (s/map-of string? clojure.core/integer?))
;;            {:type "object" :additionalProperties {:type "integer"}}))
;;     (is (= (jsc/transform (s/nilable string?))
;;            {:oneOf [{:type "string"} {:type "null"}]})))
;;   (testing "failing clojure.specs"
;;     (is (not= (jsc/transform (s/coll-of (s/tuple string? any?) :into {}))
;;               {:type "object", :additionalProperties {:type "string"}}))))

;; #?(:clj
;;    (defn test-spec-conversion [spec]
;;      (let [validate (scjsv/validator (jsc/transform spec))]
;;        (testing (str "with spec " spec)
;;          (checking "JSON schema accepts the data generated by the spec gen" 100
;;            [x (s/gen spec)]
;;            (is (nil? (validate x)) (str x " (" spec ") does not conform to JSON Schema")))))))

;; (s/def ::compound (s/keys :req-un [::integer] :opt-un [::string]))

;; #?(:clj
;;    (deftest validating-test
;;      (test-spec-conversion ::integer)
;;      (test-spec-conversion ::string)
;;      (test-spec-conversion ::set)
;;      (test-spec-conversion ::compound)
;;      (test-spec-conversion (s/nilable ::string))
;;      (test-spec-conversion (s/int-in 0 100))))

;; ;; Test the example from README

;; (s/def ::age (s/and integer? #(> % 18)))

;; (def person-spec
;;   (ds/spec
;;     ::person
;;     {::id integer?
;;      :age ::age
;;      :name string?
;;      :likes {string? boolean?}
;;      (ds/req :languages) #{keyword?}
;;      (ds/opt :address) {:street string?
;;                         :zip string?}}))

;; (deftest readme-test
;;   (is (= {:type "object"
;;           :required ["spec-tools.json-schema-test/id" "age" "name" "likes" "languages"]
;;           :properties
;;           {"spec-tools.json-schema-test/id" {:type "integer"}
;;            "age" {:type "integer"}
;;            "name" {:type "string"}
;;            "likes" {:type "object" :additionalProperties {:type "boolean"}}
;;            "languages" {:type "array", :items {:type "string"}, :uniqueItems true}
;;            "address" {:type "object"
;;                       :required ["street" "zip"]
;;                       :properties {"street" {:type "string"}
;;                                    "zip" {:type "string"}}}}}
;;          (jsc/transform person-spec))))

;; (deftest additional-json-schema-data-test
;;   (is (= {:type "integer"
;;           :title "integer"
;;           :description "it's an int"
;;           :default 42}
;;          (jsc/transform
;;            (st/spec
;;              {:spec integer?
;;               :name "integer"
;;               :description "it's an int"
;;               :json-schema/default 42})))))

;; (deftest deeply-nested-test
;;   (is (= {:type "array"
;;           :items {:type "array"
;;                   :items {:type "array"
;;                           :items {:type "array"
;;                                   :items {:type "string"}}}}}
;;          (jsc/transform
;;            (ds/spec
;;              ::nested
;;              [[[[string?]]]])))))
