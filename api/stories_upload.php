<?php
require_once 'db.php';
header('Content-Type: application/json');

$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) {
    if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; }
}

if (!$userId) { echo json_encode(["status" => "error", "message" => "Invalid token"]); exit(); }

$audience = $_POST['audience'] ?? 'story';
$label = $_POST['label'] ?? '';
$mediaType = $_POST['media_type'] ?? 'image';

if (!isset($_FILES['file'])) { echo json_encode(["status" => "error", "message" => "No file uploaded"]); exit(); }

$file = $_FILES['file'];
$uploadDir = 'uploads/stories/';
if (!file_exists($uploadDir)) { mkdir($uploadDir, 0777, true); }

$ext = pathinfo($file['name'], PATHINFO_EXTENSION);
$filename = 'story_' . $userId . '_' . time() . '.' . $ext;
$targetPath = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $targetPath)) {
    $mediaUrl = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $targetPath;
    $expiresAt = date('Y-m-d H:i:s', strtotime('+24 hours'));

    $stmt = $pdo->prepare("INSERT INTO stories (user_id, media_url, media_type, label, audience, expires_at) VALUES (?, ?, ?, ?, ?, ?)");
    if ($stmt->execute([$userId, $mediaUrl, $mediaType, $label, $audience, $expiresAt])) {
        echo json_encode(["status" => "success", "message" => "Story uploaded successfully", "story" => ["media_url" => $mediaUrl]]);
    } else {
        echo json_encode(["status" => "error", "message" => "Database insert failed"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "File upload failed"]);
}
?>