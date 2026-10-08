"""
Erzeugt die Referenzdaten unter src/main/resources/data/ aus dem
NotEnoughUpdates-REPO (MIT-Lizenz, https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO).

Nach SkyBlock-Updates (neue Shards, Minions, Accessoires) einfach neu ausfuehren:

    git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO neu
    python tools/update_reference_data.py neu
"""
import json
import pathlib
import sys

OUT = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"


def write(name, data):
    path = OUT / name
    path.write_text(json.dumps(data, indent=1, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{path} geschrieben")


def main(repo):
    constants = pathlib.Path(repo) / "constants"
    misc = json.loads((constants / "misc.json").read_text(encoding="utf-8"))
    shards = json.loads((constants / "attribute_shards.json").read_text(encoding="utf-8"))

    write("attributes.json", [
        {
            "bazaarName": a["bazaarName"],
            "shardId": a["shardId"],
            "shardName": a["displayName"],
            "attributeName": a["abilityName"],
            "rarity": a["rarity"],
        }
        for a in sorted(shards["attributes"], key=lambda a: a["shardId"])
    ])

    write("accessories.json", {
        "upgrades": misc["talisman_upgrades"],
        "ignored": sorted(misc["ignored_talisman"]),
    })

    write("minions.json", {
        key.removesuffix("_GENERATOR"): tier for key, tier in misc["minions"].items()
    })

    OUT.joinpath("NOTICE.txt").write_text(
        "Die Dateien in diesem Ordner stammen aus dem NotEnoughUpdates-REPO\n"
        "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO\n\n"
        + (pathlib.Path(repo) / "LICENSE").read_text(encoding="utf-8"),
        encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Aufruf: python tools/update_reference_data.py <Pfad zum NotEnoughUpdates-REPO>")
    main(sys.argv[1])
