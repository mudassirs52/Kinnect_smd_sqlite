<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$receiverId = $input['receiver_id'] ?? $_POST['receiver_id'] ?? 0;
$text = $input['text'] ?? $_POST['text'] ?? '';
$mediaUrl = $input['media_url'] ?? $_POST['media_url'] ?? null;
$mediaType = $input['media_type'] ?? $_POST['media_type'] ?? null;
$clientMsgId = $input['client_msg_id'] ?? $_POST['client_msg_id'] ?? uniqid();
$replyId = $input['reply_id'] ?? $_POST['reply_id'] ?? null;
if (empty($replyId)) $replyId = null;

if (empty($token) || empty($receiverId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmtCheck = $pdo->prepare("SELECT id FROM messages WHERE client_msg_id = ?");
$stmtCheck->execute([$clientMsgId]);
if ($stmtCheck->fetch()) { echo json_encode(["status" => "success", "message" => "Message already exists"]); exit(); }

$stmt = $pdo->prepare("INSERT INTO messages (client_msg_id, sender_id, receiver_id, text, media_url, media_type, reply_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
if ($stmt->execute([$clientMsgId, $userId, $receiverId, $text, $mediaUrl, $mediaType, $replyId])) {
    echo json_encode(["status" => "success", "message_id" => $pdo->lastInsertId(), "client_msg_id" => $clientMsgId]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to send message"]);
}
?>