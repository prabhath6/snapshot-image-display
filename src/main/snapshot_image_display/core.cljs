(ns snapshot-image-display.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [clojure.string :as str]
            [adzerk.env :as env]))

;; state
(defonce state (r/atom {:search-term ""
                        :current-category "Mountain"
                        :categories ["Mountain" "Beach" "Bird" "Cats"]
                        :api-response {}
                        :img-data {}
                        :display "none"
                        :access-key nil}))

;; API stuff
(defn build-img-data []
  (let [results (:results (:api-response @state))
        tags (map :tags results)
        landing_pages (filter #(= "landing_page" (:type (first %))) tags)
        ids (map #(get-in (first %) [:source :cover_photo :id]) landing_pages)
        small-urls (map #(get-in (first %) [:source :cover_photo :urls :small]) landing_pages)
        img-data (zipmap ids small-urls)]
    (swap! state assoc :img-data img-data)))

(defn get-api-response [search-term]
  (let [url (str "https://api.unsplash.com/search/collections/?client_id=" (:access-key @state) "&per_page=200&query=" search-term)]
    (->
      (.fetch js/window url)
      (.then (fn [e] (.json e)))
      (.then (fn [e]
               (swap! state assoc :api-response (js->clj e :keywordize-keys true))
               (build-img-data))))))

;; components
(defn snapshot-search-component []
  [:input.input.is-normal {:type        "text"
                           :placeholder "Search..."
                           :on-key-down (fn [e]
                                          (let [key-code (.. e -keyCode)]
                                            (if (= 13 key-code)
                                              (do
                                                (swap! state assoc :search-term (str/trim (.. e -target -value)))
                                                (prn (:search-term @state))))))}])

(defn interactive-categories-on-click-handler [e]
  (swap! state assoc :current-category (.. e -target -value)))

(defn interactive-categories-meta [value]
  {:type     "button"
   :value    value
   :on-click (fn [e]
               (interactive-categories-on-click-handler e)
               (get-api-response (.. e -target -value)))})

(defn interactive-categories [categories]
  [:div.columns.is-desktop
   [:div.column.is-half.is-offset-one-quarter
    [:div.buttons
     [:div.columns
      (for [cat categories]
        ^{:key cat} [:div.column.is-one-third
                     [:input.button.is-medium.is-outlined.is-3 (interactive-categories-meta cat)]])]]]])

(defn current-category-title-component []
  [:div [:h1.title (str (:current-category @state) " Pictures")]])

(defn modal-click-helper [e display]
  (let [modal-id (str "modal-" (.. e -target -id))
        elem        (js/document.getElementById modal-id )
        style       (.-style elem)]
    (set! (.-display style) display)))

(defn img-component [partition-data]
  [:div.tile.is-ancestor
   (doall
     (for [[key url] partition-data]
       ^{:key key}
       [:div.tile.is-parent
        [:article.tile.is-child.box
         [:figure.image.is-4by3
          [:img {:src url
                 :id key
                 :on-click #(modal-click-helper % "block")}]
          [:div.section.modal {:id (str "modal-" key)}
           [:div.modal-background]
           [:div.modal-content
            [:p.image.is-4by3
             [:img {:src url}]]]
           [:button.modal-close.is-large {:id key
                                          :on-click #(modal-click-helper % "none")}]]]]]))])

(defn build-partition []
  (let [partitioned-data (partition 4 (:img-data @state))]
    [:div
     (for [row-data partitioned-data]
       ^{:key (str/join "-" (first (first row-data)))} [img-component row-data])]))

;; Root
(defn snapshot-component
  [name]
  [:div.container.has-text-centered
   [:h1.title name]
    ;[:div.columns.is-mobile
    ; [:div.column.is-half.is-offset-one-quarter
    ;  [:div.control.has-icons-right
    ;   [snapshot-search-component]
    ;   [:span.icon.is-medium.is-right
    ;    [:i.fas.fa-search]]]]]
   [interactive-categories (:categories @state)]
   [current-category-title-component]
   ;; TODO: Remove this and switch to a different api
   [:div.section
    (for [i (range 3)]
      ^{:key i} [build-partition])]])

(defn ^:export main
  []
  (let [_ (env/def
            API_KEY nil)]
    (swap! state assoc :access-key API_KEY)
    (get-api-response "Mountains")
    (rdom/render
      [snapshot-component "Snapshot"]
      (.getElementById js/document "app"))))
