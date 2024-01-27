# Pizza Dronz
PizzaDronz is a pizza delivery service that takes in orders from students and utilises the drone that takes off from the rooftop of the Appleton Tower, picks up and delivers orders sequentially from a variety of restaurants across Edinburgh while avoiding the noFlyZones to bring them back to the tower. The service is supposed to be fast and reliable to handle a large number of orders in a timely fashion.

# Usage
To execute this program, please run the following command from the directory you have cloned the repository into:

    java -jar target/PizzaDronz-1.0-SNAPSHOT.jar 2023-12-31 https://ilp-rest.azurewebsites.net

• Date of the format 2023-12-31 (YYYY-MM-DD).

• API Url of the standard notation (https://domain.com/api) 

# Results
The result of an execution will produce 3 files:

• deliveries-YYYY-MM-DD.json (contains a JSON array of processed orders on a given date)

• flightpath-YYYY-MM-DD.json (contains a JSON array of the drone moves with respective order IDs on a given date)

• drone-YYYY-MM-DD.json   (contains a GeoJSON visual representation of the drone's movement which can be rendered at geojson.io)