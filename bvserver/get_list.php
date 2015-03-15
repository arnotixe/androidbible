<?php
 // get_list.php Modification Time indicates if changes have been made to the list content
 // changes to the presets' modifyTime below indicates if changes have been made to each individual version

 // the locale_display section is identifies weird, unknown languages that [most likely] aren't natively supported in Android.

 // M.O.: Add versions and languages manually, this page will check update time.
 // ESCAPE out apostrophes in names (') like this:    It\'s    (not It's)
 // NOTE that there is not a comma after the last translation

require "config.php";

function outpreset($loc, $pn, $sn, $ln, $desc) {
 return '{"locale": "'. $loc . '", "preset_name": "'. $pn .'", "shortName": "' . $sn . '", "longName": "'. $ln .'", "modifyTime": ' .filemtime("yes/$pn.yes") . ', "description": "'. $desc .'"}';
}

echo '{
    "presets": [
'. outpreset("qic","js","JS","Jehovapaj Shimi","Jehovapaj Shimi (Cotopaxi)") . ',
'. outpreset("es","nwt1984S","TNM1984","Traducción del Nuevo Mundo 1984","Traducción del Nuevo Mundo 1984") . ',
'. outpreset("en","nwt1984E","NWT1984","New World Translation 1984","New World Translation 1984") . ',
'. outpreset("en","nwt2013E","NWT2013","New World Translation 2013","New World Translation 2013") . ',
'. outpreset("no","nwt1984N","NV1985","Ny Verden-Oversettelsen 1984","Ny Verden-Oversettelsen 1984") . ',
'. outpreset("qia","qca4","QCA4","Dios Rimashca Shimi","Dios Rimashca Shimi (Cañar)") . ',
'. outpreset("shuar","JIVAIE","JIVAIE","NT Shuar","AIE NT (Shuar)") . '
		],

    "locale_display": {
        "shuar": "Shuar",
        "qix": "Quichua Cotopaxi",
        "qic": "Quichua Chimborazo",
        "qia": "Quichua Cañar",
        "ace": "Aceh",
        "alp": "Alune",
        "blz": "Balantak",
        "ban": "Bali",
        "ptu": "Bambam",
        "btd": "Batak-Dairi",
        "btx": "Batak-Karo",
        "bbc": "Batak-Toba",
        "bts": "Batak-Simalungun",
        "bug": "Bugis",
        "nij": "Dayak-Ngaju",
        "mvp": "Duri",
        "gor": "Gorontalo",
        "kje": "Kisar",
        "kzf": "Kaili Da\'a",
        "ljp": "Lampung",
        "mad": "Madura",
        "mak": "Makassar",
        "mwv": "Mentawai",
        "min": "Minangkabau",
        "mog": "Mongondow",
        "npy": "Napu",
        "nia": "Nias",
        "zzz-ROTE": "Rote",
        "sxn": "Sangir",
        "sas": "Sasak",
        "tby": "Tabaru",
        "sda": "Toraja",
        "yli": "Yali-Angguruk"
    },

    "download_url_format": "' . $bvsroot . 'get_yes.php?preset_name=$PRESET_NAME"
}';
?>
