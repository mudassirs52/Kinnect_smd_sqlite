<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$type = $_POST['type'] ?? 'profile';

if (empty($token) || !isset($_FILES['file'])) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$file = $_FILES['file'];
$uploadDir = 'uploads/profiles/';
if (!file_exists($uploadDir)) { mkdir($uploadDir, 0777, true); }

$ext = pathinfo($file['name'], PATHINFO_EXTENSION);
$filename = $type . '_' . $userId . '_' . time() . '.' . $ext;
$targetPath = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $targetPath)) {
    $imageUrl = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $targetPath;
    $col = ($type === 'cover') ? 'cover_photo' : 'profile_photo';
    $pdo->prepare("UPDATE users SET $col = ? WHERE id = ?")->execute([$imageUrl, $userId]);
    echo json_encode(["status" => "success", "image_url" => $imageUrl]);
} else {
    echo json_encode(["status" => "error", "message" => "Image upload failed"]);
}
?>