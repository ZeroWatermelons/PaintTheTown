import json


def splatfromfeature(i, f):
    id = i
    properties = f["properties"]
    geometry = f["geometry"]
    coordinates = geometry["coordinates"]
    long = coordinates[0]
    lat = coordinates[1]
    owner = ""
    osmid = properties["osm_id"]
    return {
        "id": id,
        "owner": owner,
        "osmid": osmid,
        "long": long,
        "lat": lat,
    }


with open("./oberbayern-latest.geojson") as file:
    obj = json.load(file)
    features = obj["features"]
    firstN = features[:2000]
splatmap = [splatfromfeature(i, f) for i, f in enumerate(firstN)]
with open("./mock-splatmap.json", "w") as file:
    json.dump(splatmap, file)
