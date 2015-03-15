<?php

require "config.php";

echo '{
  "downloadUrl": "' . $bvsroot .'/get_list.php?filename=data/yuku.alkitab.kjv-market/version_config.json",
  "success": true,
  "modifyTime": ' . filemtime('get_list.php') . '
}';
?>
