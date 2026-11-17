<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$otherUserId = $_GET['other_user_id'] ?? 0;

if (empty($token) || empty($otherUserId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY created_at ASC");
$stmt->execute([$userId, $otherUserId, $otherUserId, $userId]);
$messages = $stmt->fetchAll();

$formatted = [];
foreach ($messages as $m) {
    $formatted[] = [
        "id" => (int)$m['id'],
        "client_msg_id" => $m['client_msg_id'],
        "sender_id" => (string)$m['sender_id'],
        "receiver_id" => (string)$m['receiver_id'],
        "text" => $m['text'] ?? '',
        "media_url" => $m['media_url'],
        "media_type" => $m['media_type'],
        "reply_id" => $m['reply_id'] ? (int)$m['reply_id'] : null,
        "edited" => (bool)$m['edited'],
        "deleted" => (bool)$m['deleted'],
        "created_at" => strtotime($m['created_at']) * 1000
    ];
}
echo json_encode(["status" => "success", "messages" => $formatted]);
?>