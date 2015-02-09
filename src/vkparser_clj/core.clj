(ns vkparser-clj.core
  (:require [clj-webdriver.taxi :as wd]
            [ring.util.codec :as codec]
            [org.httpkit.client :as http]
            [clojure.data.json :as json])
  (:gen-class))

(def config
  {:oauth-url "https://oauth.vk.com/authorize"
   :api-url "https://api.vk.com/method/audio.get"
   :client-id 4770592})

(defn get-credentials
  "Invoke browser, get user id and access token"
  []
  (wd/set-driver! {:browser :firefox}
      (clojure.string/join [(config :oauth-url)
        "?client_id=" (config :client-id)
        "&redirect_uri=https://oauth.vk.com/blank.html"
        "&scope=audio"
        "&response_type=token"
        "&display=page"]))
  (wd/wait-until #(re-find #"access_token" (wd/current-url)) 60000)
  (let [params (last (clojure.string/split (wd/current-url) #"#"))]
    (wd/quit)
    (clojure.walk/keywordize-keys (codec/form-decode params))))

(defn get-songs-info
  "Get all songs data from VK API"
  [credentials]
  (let [options {:query-params {:access_token (credentials :access_token) :uid (credentials :user_id)}}
        request (http/get (config :api-url) options)]
        request))

(defn download-song
  "Request a song from a VK"
  [song-info]
  (let [response (http/get (:url song-info))]
    (println "requested" (:title song-info))
    (println @response)
    (println "downloaded" (:title song-info))))

(defn -main
  [& args]
  (let [credentials (get-credentials)
        response (get-songs-info credentials)
        songs-info (-> (:body @response) json/read-str clojure.walk/keywordize-keys :response)]
      (time (doall (pmap download-song songs-info)))))
