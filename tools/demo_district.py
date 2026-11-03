"""Downloads the demo district (Zamoskvorechye and Bolotny island, Moscow) as an Overpass dump.

python tools/demo_district.py   ->  app/demo-assets/district.json

The query is the app's own (data/Osm.kt), over a box of about 2.4 x 2.4 km around the demo position.
"""
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAT, LON = 55.74195, 37.61290
HALF_LAT, HALF_LON = 0.011, 0.019
SERVERS = ["https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter"]


def query(s, w, n, e):
    src = (ROOT / "app/src/main/java/app/fieldscan/data/Osm.kt").read_text(encoding="utf-8")
    body = re.search(r'return """(.*?)""".trimIndent\(\)', src, re.S).group(1)
    body = "\n".join(l.strip() for l in body.strip().splitlines())
    return body.replace("$bb", f"{s:.5f},{w:.5f},{n:.5f},{e:.5f}")


def main():
    q = query(LAT - HALF_LAT, LON - HALF_LON, LAT + HALF_LAT, LON + HALF_LON)
    data = urllib.parse.urlencode({"data": q}).encode()
    for srv in SERVERS:
        try:
            req = urllib.request.Request(srv, data=data, headers={"User-Agent": "Fieldscan demo builder (github.com/bigdevwhale/fieldscan)"})
            raw = urllib.request.urlopen(req, timeout=180).read().decode("utf-8")
            j = json.loads(raw)
            break
        except Exception as ex:  # try the next server
            print("failed", srv, ex, file=sys.stderr)
            time.sleep(3)
    else:
        sys.exit(1)
    out = ROOT / "app/demo-assets/district.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(j, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(out, len(j["elements"]), "elements", out.stat().st_size // 1024, "KB")


if __name__ == "__main__":
    main()
