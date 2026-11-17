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

$text = $_POST['text'] ?? '';
$stmt = $pdo->prepare("INSERT INTO posts (user_id, text) VALUES (?, ?)");
$stmt->execute([$userId, $text]);
$postId = $pdo->lastInsertId();

$uploadDir = 'uploads/posts/';
if (!file_exists($uploadDir)) { mkdir($uploadDir, 0777, true); }

$imageUrls = [];
if (isset($_FILES['files'])) {
    $fileCount = count($_FILES['files']['name']);
    for ($i = 0; $i < $fileCount; $i++) {
        if ($_FILES['files']['error'][$i] === UPLOAD_ERR_OK) {
            $ext = pathinfo($_FILES['files']['name'][$i], PATHINFO_EXTENSION);
            $filename = 'post_' . $postId . '_' . $i . '_' . time() . '.' . $ext;
            $targetPath = $uploadDir . $filename;
            if (move_uploaded_file($_FILES['files']['tmp_name'][$i], $targetPath)) {
                $imageUrl = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $targetPath;
                $imageUrls[] = $imageUrl;
                $stmtImg = $pdo->prepare("INSERT INTO post_images (post_id, image_url, image_index) VALUES (?, ?, ?)");
                $stmtImg->execute([$postId, $imageUrl, $i]);
            }
        }
    }
}
echo json_encode(["status" => "success", "post_id" => $postId, "images" => $imageUrls]);
?>