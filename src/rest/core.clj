(ns rest.core
  (:require [org.httpkit.server :as server]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [clojure.java.io :as io]
            [monger.core :as mg]
            [monger.collection :as mc]
            [cheshire.core :refer [encode]]
            [cheshire.generate :refer [add-encoder encode-str]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId])
  (:gen-class))

(def conn (mg/connect))
(def db (mg/get-db conn "notes"))

;; Connect to a given host and given port
;; (let [dbConn (mg/connect {:host "localhost" :port 27017})])

;; Allows fine-tuning connection parameters, like automatic reconnection
;; (let [^MongoOptions opts (mg/mongo-options {:threads-allowed-to-block-for-connection-multiplier 300})
;;       ^ServerAddress sa (mg/server-address "127.0.0.1" 27017)
;;       conn (mg/connect sa opts)
;;       db (mg/get-db conn "notes")])

;; (def ^:dynamic dbConn)
;; (def ^:dynamic dbInstance)

;; (let [uri "mongodb://127.0.0.1/notes"
;;       {:keys [conn db]} (mg/connect-via-uri uri)]
;;   (dbConn conn)
;;   (dbInstance db))

; Get the parameter specified by pname from :params object in request
(defn getparameter [request pname] (get (:params request) pname))
(add-encoder ObjectId encode-str)

; Save file to project root
(defn save-file [request]
  (println request)
  (println (get-in request [:params]))
  (if  (map? (get-in request [:params "file"]))
    (let [tmpfilepath (:path (bean (get-in request [:params "file" :tempfile])))
          custom-path "./" filename (get-in request [:params "file" :filename])]
      (do
        (io/copy (io/file tmpfilepath) (io/file (str custom-path filename)))
        (println filename)
        {:status 200
         :headers  {"Content-Type" "json"}
         :body (str (json/write-str {:message (str "File now available for download at: http://localhost:3000/" filename)}))}))
    {:status 400
     :headers {"Content-Type" "application/json"}
     :body (str (json/write-str {:message "Can not handle more than one file per request."}))}))

; my people-collection mutable collection vector
(def people-collection (atom []))

;Collection helper functions to add a new person
(defn addperson [firstname surname]
  (swap! people-collection conj {:firstname (str/capitalize firstname) :surname (str/capitalize surname)}))

; Example JSON objects
(addperson "Functional" "Human")
(addperson "Mickey" "Mouse")

; Return List of People
(defn people-handler [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (str (json/write-str @people-collection))})

; Add a new person into the people-collection
(defn addperson-handler [request]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (-> (let [p (partial getparameter request)]
               (str (json/write-str (addperson (p :firstname) (p :surname))))))})

; Add a new note into the notes mongodb collection
(defn addnote-handler [request]
  (let [body (get-in request [:body]) valid (atom (not (empty? body)))]
    ;; (println body)

    ;; (println (filter (fn [x]
    ;;                    (= (count x) 1))
    ;;                  ["a" "aa" "b" "n" "f" "lisp" "clojure" "q" ""]))
    ;;                    

    (println @valid)
    (println (if (empty? (filter (fn [param]
                                   (nil? (get-in body [param]))) ["title" "content"])) true false))

    (swap! valid (fn [_] (if (empty? (filter (fn [param]
                                               (nil? (get-in body [param]))) ["title" "content"])) true false)))
    (println @valid)


    ;; (println (if valid (get-in body ["title"]) "Veio vazio, cabeça de batata"))

    (when @valid (mc/insert db "notes" {:id (ObjectId.) :title (get-in body ["title"]) :content (get-in body ["content"]) :date (.toString (java.time.LocalDateTime/now)) :userId "5f90f11bad5a68c6f46dd177"}))


    {:status (if @valid 200 400)
     :headers {"Content-Type" "text/json"}
     :body (when @valid (str (json/write-str {:message "Successfully registered the note in the database."})))}))

; Simple Body Page
(defn simple-body-page [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (str (json/write-str {:message "Hi"}))})

; Handler to load all notes stored on mongodb
(defn loadnotes-handler [_]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (encode (mc/find-maps db "notes"))})

; request-example
(defn request-example [request]
  {:status 200}
  {:headers {"Content-Type" "text/html"}
   :body (->>
          (pp/pprint request)
          (str "Request Object: " request))})

(defn hello-name [request] ;(3)
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (->
          (pp/pprint request)
          (str "Hello " (:name (:params request))))})

(defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/request" [] request-example)
  (GET "/hello" [] hello-name)
  (GET "/people" [] people-handler)
  (POST "/people/add" [] addperson-handler)
  (GET "/notes" [] loadnotes-handler)
  (POST "/notes" [] (wrap-json-body addnote-handler))
  (POST "/file" [] (-> save-file
                       wrap-params
                       wrap-multipart-params))
  (route/not-found (str (json/write-str {:message "Error, page not found"}))))

(defn -main
  "This is our main entry point"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults (wrap-reload #'app-routes) api-defaults) {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))

