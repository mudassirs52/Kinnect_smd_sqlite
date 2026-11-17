<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$mediaType = $_POST['media_type'] ?? 'file';
if (!isset($_FILES['file'])) { echo json_encode(["status" => "error", "message" => "No file uploaded"]); exit(); }

$file = $_FILES['file'];
$uploadDir = 'uploads/messages/';
if (!file_exists($uploadDir)) { mkdir($uploadDir, 0777, true); }

$ext = pathinfo($file['name'], PATHINFO_EXTENSION);
$filename = 'msg_' . time() . '_' . mt_rand(1000, 9999) . '.' . $ext;
$targetPath = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $targetPath)) {
    $mediaUrl = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $targetPath;
    echo json_encode(["status" => "success", "media_url" => $mediaUrl, "media_type" => $mediaType]);
} else {
    echo json_encode(["status" => "error", "message" => "Upload failed"]);
}
?>