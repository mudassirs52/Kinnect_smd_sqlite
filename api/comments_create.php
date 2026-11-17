<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$postId = $_POST['post_id'] ?? 0;
$parentId = $_POST['parent_id'] ?? null;
if (empty($parentId)) $parentId = null;
$text = $_POST['text'] ?? '';

if (empty($token) || empty($postId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$imageUrl = null;
if (isset($_FILES['file']) && $_FILES['file']['error'] === UPLOAD_ERR_OK) {
    $uploadDir = 'uploads/comments/';
    if (!file_exists($uploadDir)) { mkdir($uploadDir, 0777, true); }
    $ext = pathinfo($_FILES['file']['name'], PATHINFO_EXTENSION);
    $filename = 'comment_' . $userId . '_' . time() . '.' . $ext;
    $targetPath = $uploadDir . $filename;
    if (move_uploaded_file($_FILES['file']['tmp_name'], $targetPath)) {
        $imageUrl = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $targetPath;
    }
}

$stmt = $pdo->prepare("INSERT INTO comments (post_id, user_id, parent_id, text, image_url) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$postId, $userId, $parentId, $text, $imageUrl]);
echo json_encode(["status" => "success", "comment_id" => $pdo->lastInsertId()]);
?>