<?php

$preset = $_GET['preset_name'];

$file = 'yes/' . $preset . '.yes';
if (!file_exists($file)) {
    $file .= '.gz';
}

if (file_exists($file)) {
    header('Content-Description: File Transfer');
    header('Content-Type: application/octet-stream');
    header('Content-Disposition: attachment; filename='.basename($file));
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($file));
    readfile($file);
    exit;
}
else {
    header('HTTP/1.0 404 Not Found');
}

?>


